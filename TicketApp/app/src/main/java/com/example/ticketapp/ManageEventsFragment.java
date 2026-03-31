package com.example.ticketapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.adapters.AdminEventAdapter;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.services.EventAdminService;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageEventsFragment extends Fragment {

    private final List<Event> events = new ArrayList<>();
    private AdminEventAdapter adapter;
    private EventAdminService eventAdminService;
    private ListenerRegistration listenerRegistration;
    private TextView noEventsText;
    private TextView infoText;
    private Button createEventButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_events, container, false);

        noEventsText = view.findViewById(R.id.noManagedEventsText);
        infoText = view.findViewById(R.id.manageEventsInfoText);
        createEventButton = view.findViewById(R.id.createEventButton);
        RecyclerView recyclerView = view.findViewById(R.id.adminEventsRecyclerView);

        adapter = new AdminEventAdapter(events, new AdminEventAdapter.EventActionListener() {
            @Override
            public void onEdit(Event event) {
                showEventFormDialog(event);
            }

            @Override
            public void onDelete(Event event) {
                showDeleteConfirmation(event);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        eventAdminService = EventAdminService.getInstance();

        if (!UserSession.getInstance().isAdmin()) {
            infoText.setText("Only admin accounts can manage events");
            createEventButton.setEnabled(false);
            noEventsText.setText("Admin access required");
            noEventsText.setVisibility(View.VISIBLE);
            return view;
        }

        createEventButton.setOnClickListener(v -> showEventFormDialog(null));
        startEventsListener();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void startEventsListener() {
        listenerRegistration = eventAdminService.listenToAllEvents(new EventAdminService.EventListListener() {
            @Override
            public void onUpdate(List<Event> updatedEvents) {
                events.clear();
                events.addAll(updatedEvents);
                adapter.notifyDataSetChanged();
                noEventsText.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                noEventsText.setText(message);
                noEventsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showEventFormDialog(Event existingEvent) {
        if (getContext() == null) {
            return;
        }

        View formView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_event_form, null);
        EditText titleInput = formView.findViewById(R.id.eventFormTitle);
        EditText dateInput = formView.findViewById(R.id.eventFormDateTime);
        EditText venueInput = formView.findViewById(R.id.eventFormVenue);
        EditText cityInput = formView.findViewById(R.id.eventFormCity);
        EditText categoryInput = formView.findViewById(R.id.eventFormCategory);
        EditText priceInput = formView.findViewById(R.id.eventFormPrice);
        EditText seatsInput = formView.findViewById(R.id.eventFormSeats);

        if (existingEvent != null) {
            titleInput.setText(existingEvent.getTitle());
            venueInput.setText(existingEvent.getVenue());
            cityInput.setText(existingEvent.getCity());
            categoryInput.setText(existingEvent.getCategory());
            priceInput.setText(String.valueOf(existingEvent.getPrice()));
            seatsInput.setText(String.valueOf(existingEvent.getSeatsAvailable()));

            if (existingEvent.getDate() != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                dateInput.setText(outputFormat.format(existingEvent.getDate().toDate()));
            }
        }

        boolean isEditing = existingEvent != null;
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(isEditing ? "Edit Event" : "Create Event")
                .setView(formView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton(isEditing ? "Save" : "Create", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                Event parsedEvent = parseEventFromInputs(
                        titleInput,
                        dateInput,
                        venueInput,
                        cityInput,
                        categoryInput,
                        priceInput,
                        seatsInput
                );

                if (parsedEvent == null) {
                    return;
                }

                positiveButton.setEnabled(false);

                EventAdminService.AdminActionCallback callback = new EventAdminService.AdminActionCallback() {
                    @Override
                    public void onSuccess() {
                        if (getContext() == null) {
                            return;
                        }
                        String message = isEditing ? "Event updated" : "Event created";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        if (getContext() == null) {
                            return;
                        }
                        positiveButton.setEnabled(true);
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                };

                if (isEditing) {
                    eventAdminService.putEvent(existingEvent.getId(), parsedEvent, callback);
                } else {
                    eventAdminService.postEvent(parsedEvent, callback);
                }
            });
        });

        dialog.show();
    }

    private Event parseEventFromInputs(EditText titleInput,
                                       EditText dateInput,
                                       EditText venueInput,
                                       EditText cityInput,
                                       EditText categoryInput,
                                       EditText priceInput,
                                       EditText seatsInput) {
        String title = titleInput.getText().toString().trim();
        String dateRaw = dateInput.getText().toString().trim();
        String venue = venueInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String priceRaw = priceInput.getText().toString().trim();
        String seatsRaw = seatsInput.getText().toString().trim();

        if (title.isEmpty()) {
            titleInput.setError("Title is required");
            titleInput.requestFocus();
            return null;
        }
        if (dateRaw.isEmpty()) {
            dateInput.setError("Date is required");
            dateInput.requestFocus();
            return null;
        }
        if (venue.isEmpty()) {
            venueInput.setError("Venue is required");
            venueInput.requestFocus();
            return null;
        }
        if (city.isEmpty()) {
            cityInput.setError("City is required");
            cityInput.requestFocus();
            return null;
        }
        if (category.isEmpty()) {
            categoryInput.setError("Category is required");
            categoryInput.requestFocus();
            return null;
        }
        if (priceRaw.isEmpty()) {
            priceInput.setError("Price is required");
            priceInput.requestFocus();
            return null;
        }
        if (seatsRaw.isEmpty()) {
            seatsInput.setError("Seats are required");
            seatsInput.requestFocus();
            return null;
        }

        Date parsedDate;
        double price;
        int seats;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            format.setLenient(false);
            parsedDate = format.parse(dateRaw);
            if (parsedDate == null) {
                throw new ParseException("Invalid date", 0);
            }
        } catch (ParseException e) {
            dateInput.setError("Use format yyyy-MM-dd HH:mm");
            dateInput.requestFocus();
            return null;
        }

        try {
            price = Double.parseDouble(priceRaw);
            if (price < 0) {
                throw new NumberFormatException("negative");
            }
        } catch (NumberFormatException e) {
            priceInput.setError("Enter a valid non-negative price");
            priceInput.requestFocus();
            return null;
        }

        try {
            seats = Integer.parseInt(seatsRaw);
            if (seats < 0) {
                throw new NumberFormatException("negative");
            }
        } catch (NumberFormatException e) {
            seatsInput.setError("Enter a valid non-negative seat count");
            seatsInput.requestFocus();
            return null;
        }

        Event event = new Event();
        event.setTitle(title);
        event.setDate(new Timestamp(parsedDate));
        event.setVenue(venue);
        event.setCity(city);
        event.setCategory(category);
        event.setPrice(price);
        event.setSeatsAvailable(seats);
        return event;
    }

    private void showDeleteConfirmation(Event event) {
        if (event == null || event.getId() == null || event.getId().trim().isEmpty() || getContext() == null) {
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Event")
                .setMessage("Delete this event and notify all ticket holders?")
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Yes", (dialog, which) -> {
                    eventAdminService.deleteEvent(event.getId(), new EventAdminService.AdminActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .show();
    }
}
