package com.example.ticketapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.text.Editable;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.app.DatePickerDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Calendar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.ticketapp.adapters.EventAdapter;
import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.services.BookingService;
import com.example.ticketapp.services.MockNotificationService;
import com.example.ticketapp.services.MockPaymentGateway;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;
    private List<Event> filteredList;
    private FirebaseFirestore db;
    private TextView noEventsText;
    private String currentQuery = "";
    private String currentType = "All";
    private BookingService bookingService;
    private MockPaymentGateway paymentGateway;
    private MockNotificationService notificationService;
    private ProgressBar reservationProgress;
    private ListenerRegistration bookingListener;
    private final Set<String> reservingEventIds = new HashSet<>();
    private final Set<String> reservedEventIds = new HashSet<>();
    private static final boolean AUTO_NAVIGATE_TO_MY_TICKETS = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Spinner searchTypeSpinner = view.findViewById(R.id.searchTypeSpinner);
        EditText searchBar = view.findViewById(R.id.searchBar);
        Button searchButton = view.findViewById(R.id.searchButton);

        eventList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new EventAdapter(filteredList, this::handleReserveClick);
        noEventsText = view.findViewById(R.id.noEventsText);
        noEventsText.setVisibility(View.GONE);
        reservationProgress = view.findViewById(R.id.reservationProgress);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        adapter.setReservationState(reservingEventIds, reservedEventIds);
    
        db = FirebaseFirestore.getInstance();
        bookingService = BookingService.getInstance();
        paymentGateway = new MockPaymentGateway();
        notificationService = new MockNotificationService();
        loadEvents();
        startReservedBookingsListener();
    
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                getContext(),
                R.array.search_types,
                android.R.layout.simple_spinner_item
        );

        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchTypeSpinner.setAdapter(adapterSpinner);

        searchTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                searchBar.setText("");
        
                if (selected.equals("Date")) {
                    searchBar.setFocusable(false); 
                    searchBar.setOnClickListener(v -> showDatePicker(searchBar));
                } else {
                    searchBar.setFocusableInTouchMode(true);
                    searchBar.setOnClickListener(null);
                }
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        searchButton.setOnClickListener(v -> {
            currentQuery = searchBar.getText().toString().trim();
            Object selected = searchTypeSpinner.getSelectedItem();
            currentType = (selected != null) ? selected.toString() : "All";
        
            applyCurrentFilter();
        });
    
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    filteredList.clear();
                    filteredList.addAll(eventList);
                    adapter.notifyDataSetChanged();
                    noEventsText.setVisibility(View.GONE); 
                }
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
    
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }
    
    private void showDatePicker(EditText searchBar) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
    
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String monthString = String.format("%02d", selectedMonth + 1);
                    String dayString = String.format("%02d", selectedDay);
                    String formattedDate = selectedYear + "-" + monthString + "-" + dayString;
    
                    searchBar.setText(formattedDate);
    
                    String query = formattedDate;
                    String type = "Date";
                    filterEvents(query, type);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void loadEvents() {
        db.collection("events")
        .addSnapshotListener((value, error) -> {
            if (error != null) {
                noEventsText.setText("Error fetching events");
                noEventsText.setVisibility(View.VISIBLE);
                return;
            }
                        eventList.clear();
                        long nowMillis = System.currentTimeMillis();
                        for (QueryDocumentSnapshot doc : value) {
                            Event event = doc.toObject(Event.class);
                            event.setId(doc.getId());
                            if (event.getDate().toDate().getTime() >= nowMillis) {
                                eventList.add(event);
                            }
                        }
                        applyCurrentFilter();
                    });
    }
    private void applyCurrentFilter() {
        if (currentQuery.isEmpty()) {
            filteredList.clear();
            filteredList.addAll(eventList);
        } else {
            filterEvents(currentQuery, currentType);
            return;
        }
    
        noEventsText.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void filterEvents(String query, String type) {
        query = query.toLowerCase();
        filteredList.clear();
        long nowMillis = System.currentTimeMillis();

        for (Event event : eventList) {
            if (event.getDate().toDate().getTime() < nowMillis) continue;
            boolean match = false;
    
            if (type.equals("All")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String dateString = sdf.format(event.getDate().toDate());
                match = event.getTitle().toLowerCase().contains(query)
                        || event.getCategory().toLowerCase().contains(query)
                        || event.getVenue().toLowerCase().contains(query)
                        || event.getCity().toLowerCase().contains(query)
                        || dateString.toLowerCase().contains(query)
                        || String.valueOf(event.getPrice()).contains(query);
            } else {
                switch (type) {
                    case "Category":
                        match = event.getCategory().toLowerCase().contains(query);
                        break;
                    case "Date":
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        String dateString = sdf.format(event.getDate().toDate());
                        match = dateString.toLowerCase().contains(query);
                        break;
                    case "Location":
                        match = event.getVenue().toLowerCase().contains(query)
                                || event.getCity().toLowerCase().contains(query);
                        break;
                }
            }
            if (match) filteredList.add(event);
        }

        if (filteredList.isEmpty()) {
            noEventsText.setVisibility(View.VISIBLE); 
        } else {
            noEventsText.setVisibility(View.GONE);  
        }
        adapter.notifyDataSetChanged();
    }

    private void handleReserveClick(Event event) {
        if (getContext() == null) {
            return;
        }

        if (event == null) {
            showPersistentMessage("Unavailable", "This event cannot be reserved right now.", false, null);
            return;
        }

        UserSession session = UserSession.getInstance();
        if (session.isGuest()) {
            showPersistentMessage("Login Required", "Please login to reserve tickets.", false, null);
            return;
        }

        View checkoutView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_mock_payment, null);
        EditText cardholderName = checkoutView.findViewById(R.id.paymentCardholderName);
        EditText cardNumber = checkoutView.findViewById(R.id.paymentCardNumber);
        EditText expiry = checkoutView.findViewById(R.id.paymentExpiry);
        EditText cvv = checkoutView.findViewById(R.id.paymentCvv);
        RadioGroup channelGroup = checkoutView.findViewById(R.id.notificationChannelGroup);
        RadioButton emailChannel = checkoutView.findViewById(R.id.channelEmail);
        RadioButton smsChannel = checkoutView.findViewById(R.id.channelSms);
        EditText destinationInput = checkoutView.findViewById(R.id.notificationDestination);
        TextView paymentSummary = checkoutView.findViewById(R.id.paymentSummary);

        setupMockPaymentInputFormatting(cardNumber, expiry);

        String preferredChannel = session.getPreferredConfirmationChannel();
        String destination = session.getContactDestination();
        if ("SMS".equalsIgnoreCase(preferredChannel)) {
            channelGroup.check(R.id.channelSms);
            destinationInput.setHint("Phone number");
            destinationInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        } else {
            channelGroup.check(R.id.channelEmail);
            destinationInput.setHint("Email address");
            destinationInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }
        destinationInput.setText(destination == null ? "" : destination);

        paymentSummary.setText("Event: " + event.getTitle() + "  |  Price: $"
                + String.format(Locale.getDefault(), "%.2f", event.getPrice()));

        channelGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.channelSms) {
                destinationInput.setHint("Phone number");
                destinationInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            } else {
                destinationInput.setHint("Email address");
                destinationInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            }
        });

        AlertDialog checkoutDialog = new AlertDialog.Builder(getContext())
                .setTitle("Mock Payment Checkout")
                .setView(checkoutView)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Pay & Reserve", null)
                .create();

        checkoutDialog.setOnShowListener(dialog -> {
            Button payButton = checkoutDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            payButton.setOnClickListener(v -> {
                String chosenChannel = channelGroup.getCheckedRadioButtonId() == R.id.channelSms ? "SMS" : "EMAIL";
                String destinationValue = destinationInput.getText().toString().trim();

                if (destinationValue.isEmpty()) {
                    destinationInput.setError(chosenChannel.equals("EMAIL") ? "Email is required" : "Phone number is required");
                    destinationInput.requestFocus();
                    return;
                }

                if (chosenChannel.equals("EMAIL") && !destinationValue.contains("@")) {
                    destinationInput.setError("Enter a valid email");
                    destinationInput.requestFocus();
                    return;
                }

                MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                        cardholderName.getText().toString().trim(),
                        cardNumber.getText().toString().trim(),
                        expiry.getText().toString().trim(),
                        cvv.getText().toString().trim()
                );

                session.setPreferredConfirmationChannel(chosenChannel);
                session.setContactDestination(destinationValue);

                checkoutDialog.dismiss();
                processReservation(event, request, chosenChannel, destinationValue);
            });
        });

        checkoutDialog.show();
    }

    private void setupMockPaymentInputFormatting(EditText cardNumber, EditText expiry) {
        final boolean[] isCardFormatting = new boolean[]{false};
        final boolean[] isExpiryFormatting = new boolean[]{false};

        cardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isCardFormatting[0]) {
                    return;
                }

                String digitsOnly = s.toString().replaceAll("[^0-9]", "");
                if (digitsOnly.length() > 19) {
                    digitsOnly = digitsOnly.substring(0, 19);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(' ');
                    }
                    formatted.append(digitsOnly.charAt(i));
                }

                String formattedText = formatted.toString();
                if (!formattedText.equals(s.toString())) {
                    isCardFormatting[0] = true;
                    cardNumber.setText(formattedText);
                    cardNumber.setSelection(formattedText.length());
                    isCardFormatting[0] = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        expiry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isExpiryFormatting[0]) {
                    return;
                }

                String digitsOnly = s.toString().replaceAll("[^0-9]", "");
                if (digitsOnly.length() > 4) {
                    digitsOnly = digitsOnly.substring(0, 4);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i == 2) {
                        formatted.append('/');
                    }
                    formatted.append(digitsOnly.charAt(i));
                }

                String formattedText = formatted.toString();
                if (!formattedText.equals(s.toString())) {
                    isExpiryFormatting[0] = true;
                    expiry.setText(formattedText);
                    expiry.setSelection(formattedText.length());
                    isExpiryFormatting[0] = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void processReservation(Event event, MockPaymentGateway.PaymentRequest paymentRequest,
                                    String selectedChannel, String selectedDestination) {
        if (getContext() == null) {
            return;
        }

        UserSession session = UserSession.getInstance();
        if (session.isGuest()) {
            showPersistentMessage("Login Required", "Please login to reserve tickets.", false, null);
            return;
        }

        if (event == null || event.getId() == null || event.getId().trim().isEmpty()) {
            showPersistentMessage("Unavailable", "This event cannot be reserved right now.", false, null);
            return;
        }

        if (reservingEventIds.contains(event.getId()) || reservedEventIds.contains(event.getId())) {
            return;
        }

        String userId = session.getUserIdentifier();
        String destination = selectedDestination;
        String channel = selectedChannel;

        reservingEventIds.add(event.getId());
        refreshReservationUi();

        paymentGateway.processPayment(event, paymentRequest, new MockPaymentGateway.PaymentCallback() {
            @Override
            public void onSuccess(String approvedPaymentMethod, String paymentReference, String paymentMessage) {
                bookingService.reserveTicket(event, userId, destination, channel, approvedPaymentMethod, paymentReference,
                        new BookingService.ReservationCallback() {
                    @Override
                    public void onSuccess(Booking booking) {
                        notificationService.sendBookingConfirmation(booking, channel, destination,
                                new MockNotificationService.NotificationCallback() {
                            @Override
                            public void onSuccess(String resolvedChannel, String resolvedDestination, String notificationMessage) {
                                booking.setConfirmationChannel(resolvedChannel);
                                booking.setContactDestination(resolvedDestination);
                                booking.setNotificationStatus(Booking.NOTIFICATION_STATUS_SENT);
                                booking.setNotificationMessage(notificationMessage);

                                bookingService.markNotificationSent(booking.getId(), resolvedChannel, resolvedDestination,
                                        notificationMessage, new BookingService.UpdateCallback() {
                                    @Override
                                    public void onSuccess() {
                                        reservingEventIds.remove(event.getId());
                                        reservedEventIds.add(event.getId());
                                        refreshReservationUi();

                                        String confirmationText = paymentMessage + "\n\n"
                                                + "Confirmation code: " + booking.getConfirmationCode() + "\n"
                                                + "Payment method: " + booking.getPaymentMethod() + "\n"
                                                + "Payment reference: " + booking.getPaymentReference() + "\n"
                                                + notificationMessage;
                                        showPersistentMessage("Reservation Confirmed", confirmationText, true, () -> {
                                            if (AUTO_NAVIGATE_TO_MY_TICKETS) {
                                                navigateToMyTickets();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String message) {
                                        reservingEventIds.remove(event.getId());
                                        refreshReservationUi();
                                        showPersistentMessage("Notification Save Failed", message, false, null);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                reservingEventIds.remove(event.getId());
                                refreshReservationUi();
                                showPersistentMessage("Notification Failed", errorMessage, false, null);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        reservingEventIds.remove(event.getId());
                        refreshReservationUi();
                        showPersistentMessage("Reservation Failed", message, false, null);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                reservingEventIds.remove(event.getId());
                refreshReservationUi();
                showPersistentMessage("Payment Failed", errorMessage, false, null);
            }
        });
    }

    private void startReservedBookingsListener() {
        UserSession session = UserSession.getInstance();
        if (session.isGuest()) {
            reservedEventIds.clear();
            refreshReservationUi();
            return;
        }

        bookingListener = bookingService.listenToUserBookings(session.getUserIdentifier(), new BookingService.BookingsListener() {
            @Override
            public void onUpdate(List<com.example.ticketapp.models.Booking> bookings) {
                reservedEventIds.clear();
                for (com.example.ticketapp.models.Booking booking : bookings) {
                    if (com.example.ticketapp.models.Booking.STATUS_CONFIRMED.equals(booking.getStatus())
                            && booking.getEventId() != null) {
                        reservedEventIds.add(booking.getEventId());
                    }
                }
                refreshReservationUi();
            }

            @Override
            public void onError(String message) {
                showPersistentMessage("Bookings Sync", message, false, null);
            }
        });
    }

    private void refreshReservationUi() {
        if (reservationProgress != null) {
            reservationProgress.setVisibility(reservingEventIds.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (adapter != null) {
            adapter.setReservationState(reservingEventIds, reservedEventIds);
            adapter.notifyDataSetChanged();
        }
    }

    private void showPersistentMessage(String title, String message, boolean isSuccess, Runnable positiveAction) {
        if (getContext() == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false);

        if (isSuccess) {
            builder.setPositiveButton("View My Tickets", (dialog, which) -> {
                if (positiveAction != null) {
                    positiveAction.run();
                }
            });
            builder.setNegativeButton("Stay Here", (dialog, which) -> dialog.dismiss());
        } else {
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }

        builder.show();
    }

    private void navigateToMyTickets() {
        if (getActivity() == null) {
            return;
        }
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null && bottomNavigationView.getMenu().findItem(R.id.nav_tickets) != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_tickets);
        }
    }
}