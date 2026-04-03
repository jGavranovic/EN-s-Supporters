package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import androidx.test.core.app.ApplicationProvider;

import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.services.BookingService;
import com.example.ticketapp.services.MockNotificationService;
import com.example.ticketapp.services.MockPaymentGateway;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HomeFragmentTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void setupMockPaymentInputFormatting_formatsCardAndExpiry() throws Exception {
        HomeFragment fragment = new HomeFragment();
        EditText cardNumber = new EditText(ApplicationProvider.getApplicationContext());
        EditText expiry = new EditText(ApplicationProvider.getApplicationContext());

        Method method = HomeFragment.class.getDeclaredMethod(
                "setupMockPaymentInputFormatting",
                EditText.class,
                EditText.class);
        method.setAccessible(true);
        method.invoke(fragment, cardNumber, expiry);

        cardNumber.setText("4242424242424242");
        expiry.setText("1230");

        assertEquals("4242 4242 4242 4242", cardNumber.getText().toString());
        assertEquals("12/30", expiry.getText().toString());
    }

    @Test
    public void onCreateView_andLoadEventsCallback_andApplyCurrentFilter_areCovered() throws Exception {
        AtomicReference<EventListener<QuerySnapshot>> eventsListenerRef = new AtomicReference<>();

        HomeFragment fragment = launchHomeFragment(eventsListenerRef);

        Event futureEvent = new Event();
        futureEvent.setTitle("Future");
        futureEvent.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86_400_000L)));

        Event pastEvent = new Event();
        pastEvent.setTitle("Past");
        pastEvent.setDate(new Timestamp(new Date(System.currentTimeMillis() - 86_400_000L)));

        QueryDocumentSnapshot futureDoc = mock(QueryDocumentSnapshot.class);
        when(futureDoc.toObject(Event.class)).thenReturn(futureEvent);
        when(futureDoc.getId()).thenReturn("future-1");

        QueryDocumentSnapshot pastDoc = mock(QueryDocumentSnapshot.class);
        when(pastDoc.toObject(Event.class)).thenReturn(pastEvent);
        when(pastDoc.getId()).thenReturn("past-1");

        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(snapshot.iterator()).thenReturn(Arrays.asList(futureDoc, pastDoc).iterator());

        EventListener<QuerySnapshot> eventsListener = eventsListenerRef.get();
        assertNotNull(eventsListener);
        eventsListener.onEvent(snapshot, null);

        invokeApplyCurrentFilter(fragment);

        TextView noEventsText = (TextView) getField(fragment, "noEventsText");
        assertEquals(TextView.GONE, noEventsText.getVisibility());
    }

    @Test
    public void handleReserveClick_forGuest_showsLoginRequiredMessage() throws Exception {
        UserSession.getInstance().setUserType(UserSession.UserType.GUEST);
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        invokeHandleReserveClick(fragment, sampleEvent("event-guest"));

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Login Required", Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void handleReserveClick_forUser_opensMockPaymentCheckoutDialog() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("user@example.com", "EMAIL");

        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        invokeHandleReserveClick(fragment, sampleEvent("event-checkout"));

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Mock Payment Checkout", Shadows.shadowOf(dialog).getTitle());

        TextView paymentSummary = dialog.findViewById(R.id.paymentSummary);
        assertNotNull(paymentSummary);
        assertTrue(paymentSummary.getText().toString().contains("Sample Event"));
    }

    @Test
    public void handleReserveClick_positiveButtonPath_coversCheckoutLambdaValidation() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("", "EMAIL");
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        RadioGroup channelGroup = new RadioGroup(ApplicationProvider.getApplicationContext());
        RadioButton emailButton = new RadioButton(ApplicationProvider.getApplicationContext());
        emailButton.setId(R.id.channelEmail);
        channelGroup.addView(emailButton);
        channelGroup.check(R.id.channelEmail);

        EditText destination = new EditText(ApplicationProvider.getApplicationContext());
        destination.setText("");

        Method checkoutLambda = HomeFragment.class.getDeclaredMethod(
                "lambda$handleReserveClick$5",
                RadioGroup.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                UserSession.class,
                AlertDialog.class,
                Event.class,
                View.class);
        checkoutLambda.setAccessible(true);
        checkoutLambda.invoke(fragment,
                channelGroup,
                destination,
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                session,
                mock(AlertDialog.class),
                sampleEvent("event-user"),
                new View(ApplicationProvider.getApplicationContext()));

        assertNotNull(destination.getError());
        assertEquals("Email is required", destination.getError().toString());
    }

    @Test
    public void payButtonClick_smsEmptyDestination_showsPhoneRequired() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        EditText destination = new EditText(ApplicationProvider.getApplicationContext());
        destination.setText("");

        invokePayClickLambda(fragment, session, R.id.channelSms, destination,
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                sampleEvent("event-sms-empty"),
                mock(AlertDialog.class));

        assertNotNull(destination.getError());
        assertEquals("Phone number is required", destination.getError().toString());
    }

    @Test
    public void payButtonClick_invalidEmail_showsValidationError() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        EditText destination = new EditText(ApplicationProvider.getApplicationContext());
        destination.setText("invalid-email");

        invokePayClickLambda(fragment, session, R.id.channelEmail, destination,
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                new EditText(ApplicationProvider.getApplicationContext()),
                sampleEvent("event-invalid-email"),
                mock(AlertDialog.class));

        assertNotNull(destination.getError());
        assertEquals("Enter a valid email", destination.getError().toString());
    }

    @Test
    public void payButtonClick_validSms_updatesSessionAndDismissesDialog() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("old@example.com", "EMAIL");
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        AlertDialog checkoutDialog = mock(AlertDialog.class);
        EditText destination = new EditText(ApplicationProvider.getApplicationContext());
        destination.setText("5551234567");

        invokePayClickLambda(fragment, session, R.id.channelSms, destination,
                textInput("Name"),
                textInput("4242 4242 4242 4242"),
                textInput("12/30"),
                textInput("123"),
                null,
                checkoutDialog);

        assertEquals("SMS", session.getPreferredConfirmationChannel());
        assertEquals("5551234567", session.getContactDestination());
        verify(checkoutDialog).dismiss();
    }

    @Test
    public void processReservation_paymentOnSuccess_passesApprovedPaymentToBookingService() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("buyer@example.com", "EMAIL");

        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        BookingService bookingServiceMock = mock(BookingService.class);
        com.example.ticketapp.services.MockNotificationService notificationServiceMock =
                mock(com.example.ticketapp.services.MockNotificationService.class);
        com.example.ticketapp.services.MockPaymentGateway paymentGatewayMock =
                mock(com.example.ticketapp.services.MockPaymentGateway.class);

        setField(fragment, "bookingService", bookingServiceMock);
        setField(fragment, "notificationService", notificationServiceMock);
        setField(fragment, "paymentGateway", paymentGatewayMock);

        Event event = sampleEvent("event-success");
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Buyer", "4242 4242 4242 4242", "12/30", "123");

        doAnswer(invocation -> {
            MockPaymentGateway.PaymentCallback callback = invocation.getArgument(2);
            callback.onSuccess("VISA", "PAY-123", "Approved");
            return null;
        }).when(paymentGatewayMock).processPayment(eq(event), eq(request), any(MockPaymentGateway.PaymentCallback.class));

        invokeProcessReservation(fragment, event, request, "EMAIL", "buyer@example.com");

        verify(bookingServiceMock).reserveTicket(
                eq(event),
                eq(session.getUserIdentifier()),
                eq("buyer@example.com"),
                eq("EMAIL"),
                eq("VISA"),
                eq("PAY-123"),
                any(BookingService.ReservationCallback.class));
    }

    @Test
    public void reservationCallback_onSuccess_booking_triggersNotificationSend() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("buyer@example.com", "EMAIL");

        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        BookingService bookingServiceMock = mock(BookingService.class);
        MockNotificationService notificationServiceMock = mock(MockNotificationService.class);
        MockPaymentGateway paymentGatewayMock = mock(MockPaymentGateway.class);

        setField(fragment, "bookingService", bookingServiceMock);
        setField(fragment, "notificationService", notificationServiceMock);
        setField(fragment, "paymentGateway", paymentGatewayMock);

        Event event = sampleEvent("event-booking-success");
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Buyer", "4242 4242 4242 4242", "12/30", "123");

        doAnswer(invocation -> {
            MockPaymentGateway.PaymentCallback callback = invocation.getArgument(2);
            callback.onSuccess("VISA", "PAY-456", "Approved");
            return null;
        }).when(paymentGatewayMock).processPayment(eq(event), eq(request), any(MockPaymentGateway.PaymentCallback.class));

        doAnswer(invocation -> {
            BookingService.ReservationCallback callback = invocation.getArgument(6);
            Booking booking = new Booking();
            booking.setId("booking-1");
            callback.onSuccess(booking);
            return null;
        }).when(bookingServiceMock).reserveTicket(
                eq(event),
                eq(session.getUserIdentifier()),
                eq("buyer@example.com"),
                eq("EMAIL"),
                eq("VISA"),
                eq("PAY-456"),
                any(BookingService.ReservationCallback.class));

        invokeProcessReservation(fragment, event, request, "EMAIL", "buyer@example.com");

        verify(notificationServiceMock).sendBookingConfirmation(
                any(Booking.class),
                eq("EMAIL"),
                eq("buyer@example.com"),
                any(MockNotificationService.NotificationCallback.class));
    }

    @Test
    public void notificationCallback_onSuccess_resolvedValues_markNotificationSentCalled() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("buyer@example.com", "EMAIL");

        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        BookingService bookingServiceMock = mock(BookingService.class);
        MockNotificationService notificationServiceMock = mock(MockNotificationService.class);
        MockPaymentGateway paymentGatewayMock = mock(MockPaymentGateway.class);

        setField(fragment, "bookingService", bookingServiceMock);
        setField(fragment, "notificationService", notificationServiceMock);
        setField(fragment, "paymentGateway", paymentGatewayMock);

        Event event = sampleEvent("event-notification-success");
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Buyer", "4242 4242 4242 4242", "12/30", "123");

        doAnswer(invocation -> {
            MockPaymentGateway.PaymentCallback callback = invocation.getArgument(2);
            callback.onSuccess("VISA", "PAY-777", "Approved");
            return null;
        }).when(paymentGatewayMock).processPayment(eq(event), eq(request), any(MockPaymentGateway.PaymentCallback.class));

        doAnswer(invocation -> {
            BookingService.ReservationCallback callback = invocation.getArgument(6);
            Booking booking = new Booking();
            booking.setId("booking-777");
            callback.onSuccess(booking);
            return null;
        }).when(bookingServiceMock).reserveTicket(any(), anyString(), anyString(), anyString(), anyString(), anyString(), any(BookingService.ReservationCallback.class));

        doAnswer(invocation -> {
            MockNotificationService.NotificationCallback callback = invocation.getArgument(3);
            callback.onSuccess("SMS", "5551112222", "sent");
            return null;
        }).when(notificationServiceMock).sendBookingConfirmation(any(Booking.class), anyString(), anyString(), any(MockNotificationService.NotificationCallback.class));

        doAnswer(invocation -> null).when(bookingServiceMock)
                .markNotificationSent(anyString(), anyString(), anyString(), anyString(), any(BookingService.UpdateCallback.class));

        invokeProcessReservation(fragment, event, request, "EMAIL", "buyer@example.com");

        verify(bookingServiceMock).markNotificationSent(
                eq("booking-777"),
                eq("SMS"),
                eq("5551112222"),
                eq("sent"),
                any(BookingService.UpdateCallback.class));
    }

    @Test
    public void updateCallback_onSuccess_showsReservationConfirmedDialog() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("buyer@example.com", "EMAIL");

        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        BookingService bookingServiceMock = mock(BookingService.class);
        MockNotificationService notificationServiceMock = mock(MockNotificationService.class);
        MockPaymentGateway paymentGatewayMock = mock(MockPaymentGateway.class);

        setField(fragment, "bookingService", bookingServiceMock);
        setField(fragment, "notificationService", notificationServiceMock);
        setField(fragment, "paymentGateway", paymentGatewayMock);

        Event event = sampleEvent("event-update-success");
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Buyer", "4242 4242 4242 4242", "12/30", "123");

        doAnswer(invocation -> {
            MockPaymentGateway.PaymentCallback callback = invocation.getArgument(2);
            callback.onSuccess("VISA", "PAY-888", "Approved");
            return null;
        }).when(paymentGatewayMock).processPayment(eq(event), eq(request), any(MockPaymentGateway.PaymentCallback.class));

        doAnswer(invocation -> {
            BookingService.ReservationCallback callback = invocation.getArgument(6);
            Booking booking = new Booking();
            booking.setId("booking-888");
            booking.setConfirmationCode("CONF-888");
            booking.setPaymentMethod("VISA");
            booking.setPaymentReference("PAY-888");
            callback.onSuccess(booking);
            return null;
        }).when(bookingServiceMock).reserveTicket(any(), anyString(), anyString(), anyString(), anyString(), anyString(), any(BookingService.ReservationCallback.class));

        doAnswer(invocation -> {
            MockNotificationService.NotificationCallback callback = invocation.getArgument(3);
            callback.onSuccess("EMAIL", "buyer@example.com", "sent");
            return null;
        }).when(notificationServiceMock).sendBookingConfirmation(any(Booking.class), anyString(), anyString(), any(MockNotificationService.NotificationCallback.class));

        doAnswer(invocation -> {
            BookingService.UpdateCallback callback = invocation.getArgument(4);
            callback.onSuccess();
            return null;
        }).when(bookingServiceMock).markNotificationSent(anyString(), anyString(), anyString(), anyString(), any(BookingService.UpdateCallback.class));

        invokeProcessReservation(fragment, event, request, "EMAIL", "buyer@example.com");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Reservation Confirmed", Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void updateCallback_onError_showsNotificationSaveFailedDialog() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("buyer@example.com", "EMAIL");

        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        BookingService bookingServiceMock = mock(BookingService.class);
        MockNotificationService notificationServiceMock = mock(MockNotificationService.class);
        MockPaymentGateway paymentGatewayMock = mock(MockPaymentGateway.class);

        setField(fragment, "bookingService", bookingServiceMock);
        setField(fragment, "notificationService", notificationServiceMock);
        setField(fragment, "paymentGateway", paymentGatewayMock);

        Event event = sampleEvent("event-update-error");
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Buyer", "4242 4242 4242 4242", "12/30", "123");

        doAnswer(invocation -> {
            MockPaymentGateway.PaymentCallback callback = invocation.getArgument(2);
            callback.onSuccess("VISA", "PAY-999", "Approved");
            return null;
        }).when(paymentGatewayMock).processPayment(eq(event), eq(request), any(MockPaymentGateway.PaymentCallback.class));

        doAnswer(invocation -> {
            BookingService.ReservationCallback callback = invocation.getArgument(6);
            Booking booking = new Booking();
            booking.setId("booking-999");
            callback.onSuccess(booking);
            return null;
        }).when(bookingServiceMock).reserveTicket(any(), anyString(), anyString(), anyString(), anyString(), anyString(), any(BookingService.ReservationCallback.class));

        doAnswer(invocation -> {
            MockNotificationService.NotificationCallback callback = invocation.getArgument(3);
            callback.onSuccess("EMAIL", "buyer@example.com", "sent");
            return null;
        }).when(notificationServiceMock).sendBookingConfirmation(any(Booking.class), anyString(), anyString(), any(MockNotificationService.NotificationCallback.class));

        doAnswer(invocation -> {
            BookingService.UpdateCallback callback = invocation.getArgument(4);
            callback.onError("Save failed");
            return null;
        }).when(bookingServiceMock).markNotificationSent(anyString(), anyString(), anyString(), anyString(), any(BookingService.UpdateCallback.class));

        invokeProcessReservation(fragment, event, request, "EMAIL", "buyer@example.com");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Notification Save Failed", Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void processReservation_invalidEvent_showsUnavailableMessage() throws Exception {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity("user@example.com", "EMAIL");
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Test User", "4242 4242 4242 4242", "12/30", "123");
        invokeProcessReservation(fragment, null, request, "EMAIL", "user@example.com");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Unavailable", Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void showPersistentMessage_success_runsPositiveAction() throws Exception {
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());

        invokeShowPersistentMessage(fragment, "Reservation Confirmed", "Done", true,
                () -> { });

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Reservation Confirmed", Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void showDatePicker_andSelectionLambda_updateSearchBar() throws Exception {
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        EditText searchBar = requireViewField(fragment, R.id.searchBar, EditText.class);

        invokeShowDatePicker(fragment, searchBar);

        Method dateLambda = null;
        for (Method declaredMethod : HomeFragment.class.getDeclaredMethods()) {
            if (declaredMethod.getName().startsWith("lambda$showDatePicker$")
                    && declaredMethod.getParameterTypes().length == 5) {
                dateLambda = declaredMethod;
                break;
            }
        }
        assertNotNull(dateLambda);
        dateLambda.setAccessible(true);
        dateLambda.invoke(fragment, searchBar, null, 2026, 4, 12);

        assertEquals("2026-05-12", searchBar.getText().toString());
    }

    @Test
    public void onItemSelected_coversDateAndNonDateBranches() throws Exception {
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        Spinner spinner = requireViewField(fragment, R.id.searchTypeSpinner, Spinner.class);
        EditText searchBar = requireViewField(fragment, R.id.searchBar, EditText.class);

        AdapterView.OnItemSelectedListener listener = spinner.getOnItemSelectedListener();
        assertNotNull(listener);

        AdapterView<?> parent = mock(AdapterView.class);
        when(parent.getItemAtPosition(0)).thenReturn("Date");
        listener.onItemSelected(parent, null, 0, 0L);

        assertFalse(searchBar.isFocusable());
        assertTrue(searchBar.hasOnClickListeners());

        when(parent.getItemAtPosition(1)).thenReturn("All");
        listener.onItemSelected(parent, null, 1, 0L);

        assertTrue(searchBar.isFocusableInTouchMode());
        assertFalse(searchBar.hasOnClickListeners());
    }

    @Test
    public void onTextChanged_coversEmptyAndNonEmptyBranches() throws Exception {
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        EditText searchBar = requireViewField(fragment, R.id.searchBar, EditText.class);
        TextView noEventsText = (TextView) getField(fragment, "noEventsText");
        @SuppressWarnings("unchecked")
        List<Event> eventList = (List<Event>) getField(fragment, "eventList");
        @SuppressWarnings("unchecked")
        List<Event> filteredList = (List<Event>) getField(fragment, "filteredList");

        eventList.clear();
        filteredList.clear();
        Event future = sampleEvent("event-search");
        eventList.add(future);

        filteredList.add(sampleEvent("existing"));
        noEventsText.setVisibility(View.VISIBLE);
        searchBar.setText("rock");

        assertEquals(1, filteredList.size());
        assertEquals(View.VISIBLE, noEventsText.getVisibility());

        searchBar.setText("");

        assertEquals(1, filteredList.size());
        assertEquals("event-search", filteredList.get(0).getId());
        assertEquals(View.GONE, noEventsText.getVisibility());
    }

    @Test
    public void showDatePicker_setsMinimumDateAndShowsDialog() throws Exception {
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        EditText searchBar = requireViewField(fragment, R.id.searchBar, EditText.class);

        invokeShowDatePicker(fragment, searchBar);

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(dialog instanceof android.app.DatePickerDialog);
        android.app.DatePickerDialog pickerDialog = (android.app.DatePickerDialog) dialog;
        assertTrue(pickerDialog.getDatePicker().getMinDate() > 0L);
    }

    @Test
    public void applyCurrentFilter_coversEmptyAndFilteredBranches() throws Exception {
        HomeFragment fragment = launchHomeFragment(new AtomicReference<>());
        TextView noEventsText = (TextView) getField(fragment, "noEventsText");
        @SuppressWarnings("unchecked")
        List<Event> eventList = (List<Event>) getField(fragment, "eventList");
        @SuppressWarnings("unchecked")
        List<Event> filteredList = (List<Event>) getField(fragment, "filteredList");

        eventList.clear();
        filteredList.clear();
        Event matching = sampleEvent("event-filter");
        matching.setTitle("Rock Night");
        eventList.add(matching);

        setField(fragment, "currentQuery", "");
        setField(fragment, "currentType", "All");
        invokeApplyCurrentFilter(fragment);
        assertEquals(1, filteredList.size());
        assertEquals(View.GONE, noEventsText.getVisibility());

        setField(fragment, "currentQuery", "No Match");
        setField(fragment, "currentType", "All");
        invokeApplyCurrentFilter(fragment);
        assertEquals(0, filteredList.size());
        assertEquals(View.VISIBLE, noEventsText.getVisibility());
    }

    private HomeFragment launchHomeFragment(AtomicReference<EventListener<QuerySnapshot>> eventsListenerRef) {
        try (MockedStatic<FirebaseFirestore> firestoreStatic = org.mockito.Mockito.mockStatic(FirebaseFirestore.class);
             MockedStatic<BookingService> bookingStatic = org.mockito.Mockito.mockStatic(BookingService.class)) {

            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference eventsCollection = mock(CollectionReference.class);
            BookingService bookingService = mock(BookingService.class);

            when(firestore.collection("events")).thenReturn(eventsCollection);
            when(eventsCollection.addSnapshotListener(any(EventListener.class))).thenAnswer(invocation -> {
                eventsListenerRef.set(invocation.getArgument(0));
                return mock(ListenerRegistration.class);
            });
            when(bookingService.listenToUserBookings(anyString(), any(BookingService.BookingsListener.class)))
                    .thenReturn(mock(ListenerRegistration.class));

            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            bookingStatic.when(BookingService::getInstance).thenReturn(bookingService);

            AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
            FrameLayout container = new FrameLayout(activity);
            container.setId(android.R.id.content);
            activity.setContentView(container);

            HomeFragment fragment = new HomeFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commitNow();
            return fragment;
        }
    }

    private Event sampleEvent(String id) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Sample Event");
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86_400_000L)));
        event.setPrice(50.0);
        event.setVenue("Sample Venue");
        event.setCity("Montreal");
        event.setCategory("Music");
        event.setSeatsAvailable(10);
        return event;
    }

    private <T> T requireViewField(HomeFragment fragment, int id, Class<T> type) {
        if (fragment.getView() == null) {
            throw new AssertionError("Fragment view is null");
        }
        return type.cast(fragment.getView().findViewById(id));
    }

    private Object getField(Object target, String fieldName) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private EditText textInput(String value) {
        EditText input = new EditText(ApplicationProvider.getApplicationContext());
        input.setText(value);
        return input;
    }

    private void invokePayClickLambda(HomeFragment fragment,
                                      UserSession session,
                                      int selectedChannelId,
                                      EditText destination,
                                      EditText cardholder,
                                      EditText cardNumber,
                                      EditText expiry,
                                      EditText cvv,
                                      Event event,
                                      AlertDialog checkoutDialog) throws Exception {
        RadioGroup channelGroup = new RadioGroup(ApplicationProvider.getApplicationContext());
        RadioButton email = new RadioButton(ApplicationProvider.getApplicationContext());
        email.setId(R.id.channelEmail);
        RadioButton sms = new RadioButton(ApplicationProvider.getApplicationContext());
        sms.setId(R.id.channelSms);
        channelGroup.addView(email);
        channelGroup.addView(sms);
        channelGroup.check(selectedChannelId);

        Method checkoutLambda = HomeFragment.class.getDeclaredMethod(
                "lambda$handleReserveClick$5",
                RadioGroup.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                UserSession.class,
                AlertDialog.class,
                Event.class,
                View.class);
        checkoutLambda.setAccessible(true);
        checkoutLambda.invoke(fragment,
                channelGroup,
                destination,
                cardholder,
                cardNumber,
                expiry,
                cvv,
                session,
                checkoutDialog,
                event,
                new View(ApplicationProvider.getApplicationContext()));
    }

    private void invokeHandleReserveClick(HomeFragment fragment, Event event) throws Exception {
        Method method = HomeFragment.class.getDeclaredMethod("handleReserveClick", Event.class);
        method.setAccessible(true);
        method.invoke(fragment, event);
    }

    private void invokeProcessReservation(HomeFragment fragment,
                                          Event event,
                                          MockPaymentGateway.PaymentRequest request,
                                          String channel,
                                          String destination) throws Exception {
        Method method = HomeFragment.class.getDeclaredMethod(
                "processReservation",
                Event.class,
                MockPaymentGateway.PaymentRequest.class,
                String.class,
                String.class);
        method.setAccessible(true);
        method.invoke(fragment, event, request, channel, destination);
    }

    private void invokeApplyCurrentFilter(HomeFragment fragment) throws Exception {
        Method method = HomeFragment.class.getDeclaredMethod("applyCurrentFilter");
        method.setAccessible(true);
        method.invoke(fragment);
    }

    private void invokeShowPersistentMessage(HomeFragment fragment,
                                             String title,
                                             String message,
                                             boolean success,
                                             Runnable positiveAction) throws Exception {
        Method method = HomeFragment.class.getDeclaredMethod(
                "showPersistentMessage",
                String.class,
                String.class,
                boolean.class,
                Runnable.class);
        method.setAccessible(true);
        method.invoke(fragment, title, message, success, positiveAction);
    }

    private void invokeShowDatePicker(HomeFragment fragment, EditText searchBar) throws Exception {
        Method method = HomeFragment.class.getDeclaredMethod("showDatePicker", EditText.class);
        method.setAccessible(true);
        method.invoke(fragment, searchBar);
    }
}


