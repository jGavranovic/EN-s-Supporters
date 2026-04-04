package com.example.ticketapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class BookingServiceTest {

    @AfterEach
    void tearDown() throws Exception {
        resetBookingServiceSingleton();
    }

    @Test
    void reserveTicket_rejectsMissingEvent() throws Exception {
        BookingService service = newBookingServiceWithMockedFirestore();

        AtomicReference<String> error = new AtomicReference<>();
        service.reserveTicket(null, "user-1", "user@example.com", "EMAIL", "VISA", "PAY-1",
                new BookingService.ReservationCallback() {
                    @Override
                    public void onSuccess(Booking booking) {
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                    }
                });

        assertEquals("Event is not available for booking", error.get());
    }

    @Test
    void reserveTicket_rejectsMissingEventId() throws Exception {
        BookingService service = newBookingServiceWithMockedFirestore();
        Event event = eventWithId("  ");

        AtomicReference<String> error = new AtomicReference<>();
        service.reserveTicket(event, "user-1", "user@example.com", "EMAIL", "VISA", "PAY-1", reservationCallback(error));

        assertEquals("Event is not available for booking", error.get());
    }

    @Test
    void reserveTicket_rejectsMissingUser() throws Exception {
        BookingService service = newBookingServiceWithMockedFirestore();
        Event event = eventWithId("event-1");

        AtomicReference<String> error = new AtomicReference<>();
        service.reserveTicket(event, "   ", "user@example.com", "EMAIL", "VISA", "PAY-1",
                new BookingService.ReservationCallback() {
                    @Override
                    public void onSuccess(Booking booking) {
                    }

                    @Override
                    public void onError(String message) {
                        error.set(message);
                    }
                });

        assertEquals("Missing user information", error.get());
    }

    @Test
    void reserveTicket_failsWhenEventNoLongerExists() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);

        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-10")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("user-1_event-10")).thenReturn(bookingRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(eventSnapshot.exists()).thenReturn(false);
            try {
                function.apply(transaction);
                return immediateSuccessTask(null);
            } catch (Exception e) {
                return immediateFailureTask(e);
            }
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();

        service.reserveTicket(eventWithId("event-10"), "user-1", "user@example.com", "EMAIL", "VISA", "PAY-1", reservationCallback(error));

        assertEquals("Event no longer exists", error.get());
    }

    @Test
    void reserveTicket_failsWhenUserAlreadyHasConfirmedBooking() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);

        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-11")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("user-1_event-11")).thenReturn(bookingRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(eventSnapshot.exists()).thenReturn(true);
            when(eventSnapshot.getLong("seatsAvailable")).thenReturn(5L);
            when(bookingSnapshot.exists()).thenReturn(true);
            when(bookingSnapshot.getString("status")).thenReturn(Booking.STATUS_CONFIRMED);
            try {
                function.apply(transaction);
                return immediateSuccessTask(null);
            } catch (Exception e) {
                return immediateFailureTask(e);
            }
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();

        service.reserveTicket(eventWithId("event-11"), "user-1", "user@example.com", "EMAIL", "VISA", "PAY-1", reservationCallback(error));

        assertEquals("You already reserved this ticket", error.get());
    }

    @Test
    void reserveTicket_failsWhenSoldOut() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);

        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-12")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("user-1_event-12")).thenReturn(bookingRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(eventSnapshot.exists()).thenReturn(true);
            when(eventSnapshot.getLong("seatsAvailable")).thenReturn(0L);
            when(bookingSnapshot.exists()).thenReturn(false);
            try {
                function.apply(transaction);
                return immediateSuccessTask(null);
            } catch (Exception e) {
                return immediateFailureTask(e);
            }
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();

        service.reserveTicket(eventWithId("event-12"), "user-1", "user@example.com", "EMAIL", "VISA", "PAY-1", reservationCallback(error));

        assertEquals("Tickets are sold out", error.get());
    }

    @Test
    void reserveTicket_successDecrementsSeatsAndBuildsBooking() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        AtomicReference<Transaction> transactionRef = new AtomicReference<>();

        Event event = eventWithId("event-13");
        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-13")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("user-1_event-13")).thenReturn(bookingRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            transactionRef.set(transaction);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(eventSnapshot.exists()).thenReturn(true);
            when(eventSnapshot.getLong("seatsAvailable")).thenReturn(10L);
            when(bookingSnapshot.exists()).thenReturn(true);
            when(bookingSnapshot.getString("status")).thenReturn(Booking.STATUS_CANCELLED);
            function.apply(transaction);
            return immediateSuccessTask(null);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        ReservationCapture capture = new ReservationCapture();

        service.reserveTicket(event, "user-1", " user@example.com ", "sms", "VISA", "PAY-13", capture);

        verify(transactionRef.get()).set(eq(bookingRef), argThat(raw -> {
            Map<String, Object> payload = (Map<String, Object>) raw;
            return "event-13".equals(payload.get("eventId"))
                    && Booking.STATUS_CONFIRMED.equals(payload.get("status"))
                    && Booking.PAYMENT_STATUS_PAID.equals(payload.get("paymentStatus"))
                    && Booking.NOTIFICATION_STATUS_PENDING.equals(payload.get("notificationStatus"));
        }));
        verify(transactionRef.get()).update(eventRef, "seatsAvailable", 9L);

        assertNull(capture.error);
        assertNotNull(capture.booking);
        assertEquals("user-1_event-13", capture.booking.getId());
        assertEquals("SMS", capture.booking.getConfirmationChannel());
        assertEquals("user@example.com", capture.booking.getContactDestination());
        assertEquals(Booking.STATUS_CONFIRMED, capture.booking.getStatus());
        assertEquals(Booking.PAYMENT_STATUS_PAID, capture.booking.getPaymentStatus());
        assertNotNull(capture.booking.getConfirmationCode());
        assertEquals(10, capture.booking.getConfirmationCode().length());
    }

    @Test
    void reserveTicket_successWithNullSeatCountSkipsSeatUpdate() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        AtomicReference<Transaction> transactionRef = new AtomicReference<>();

        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-14")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("user-1_event-14")).thenReturn(bookingRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            transactionRef.set(transaction);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(eventSnapshot.exists()).thenReturn(true);
            when(eventSnapshot.getLong("seatsAvailable")).thenReturn(null);
            when(bookingSnapshot.exists()).thenReturn(false);
            function.apply(transaction);
            return immediateSuccessTask(null);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        ReservationCapture capture = new ReservationCapture();

        service.reserveTicket(eventWithId("event-14"), "user-1", null, null, "CARD", "PAY-14", capture);

        verify(transactionRef.get(), never()).update(eq(eventRef), eq("seatsAvailable"), any());
        assertNotNull(capture.booking);
        assertEquals("EMAIL", capture.booking.getConfirmationChannel());
        assertEquals("", capture.booking.getContactDestination());
    }

    @Test
    void reserveTicket_usesFallbackErrorMessageWhenFailureHasNoMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference events = mock(CollectionReference.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        Task<Void> failedTask = immediateFailureTask(new RuntimeException());

        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-15")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("user-1_event-15")).thenReturn(bookingRef);
        when(firestore.runTransaction(any())).thenAnswer(invocation -> failedTask);

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();

        service.reserveTicket(eventWithId("event-15"), "user-1", "d", "EMAIL", "CARD", "P", reservationCallback(error));

        assertEquals("Unexpected booking error", error.get());
    }

    @Test
    void cancelBooking_rejectsInvalidBooking() throws Exception {
        BookingService service = newBookingServiceWithMockedFirestore();

        AtomicReference<String> error = new AtomicReference<>();
        service.cancelBooking(new Booking(), new BookingService.CancellationCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertEquals("Invalid booking", error.get());
    }

    @Test
    void markNotificationSent_rejectsMissingBookingId() throws Exception {
        BookingService service = newBookingServiceWithMockedFirestore();
        AtomicReference<String> error = new AtomicReference<>();

        service.markNotificationSent("   ", "EMAIL", "user@example.com", "sent", updateCallback(error));

        assertEquals("Missing booking id", error.get());
    }

    @Test
    void markNotificationSent_updatesBookingAndSucceeds() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        Task<Void> successTask = immediateSuccessTask(null);

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("booking-1")).thenReturn(bookingRef);
        when(bookingRef.update(anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any()))
                .thenReturn(successTask);

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        CallbackCapture capture = new CallbackCapture();

        service.markNotificationSent("booking-1", "sms", null, null, capture);

        verify(bookingRef).update(anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any());
        assertTrue(capture.success);
        assertNull(capture.error);
    }

    @Test
    void markNotificationSent_returnsReadableErrorMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        Task<Void> failureTask = immediateFailureTask(new RuntimeException("network down"));

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("booking-2")).thenReturn(bookingRef);
        when(bookingRef.update(anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any()))
                .thenReturn(failureTask);

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();

        service.markNotificationSent("booking-2", "fax", "x", "hello", updateCallback(error));

        assertEquals("network down", error.get());
    }

    @Test
    void markNotificationSent_returnsFallbackErrorMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        Task<Void> failureTask = immediateFailureTask(new RuntimeException());

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("booking-3")).thenReturn(bookingRef);
        when(bookingRef.update(anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any(), anyString(), any()))
                .thenReturn(failureTask);

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();

        service.markNotificationSent("booking-3", "email", "x", "hello", updateCallback(error));

        assertEquals("Unexpected booking error", error.get());
    }

    @Test
    void cancelBooking_failsWhenBookingMissing() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("b-1")).thenReturn(bookingRef);
        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(bookingSnapshot.exists()).thenReturn(false);
            try {
                function.apply(transaction);
                return immediateSuccessTask(null);
            } catch (Exception e) {
                return immediateFailureTask(e);
            }
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        Booking booking = new Booking();
        booking.setId("b-1");
        AtomicReference<String> error = new AtomicReference<>();

        service.cancelBooking(booking, cancellationCallback(error));

        assertEquals("Booking not found", error.get());
    }

    @Test
    void cancelBooking_failsWhenAlreadyCancelled() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("b-2")).thenReturn(bookingRef);
        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(bookingSnapshot.exists()).thenReturn(true);
            when(bookingSnapshot.getString("status")).thenReturn(Booking.STATUS_CANCELLED);
            try {
                function.apply(transaction);
                return immediateSuccessTask(null);
            } catch (Exception e) {
                return immediateFailureTask(e);
            }
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        Booking booking = new Booking();
        booking.setId("b-2");
        AtomicReference<String> error = new AtomicReference<>();

        service.cancelBooking(booking, cancellationCallback(error));

        assertEquals("Booking already cancelled", error.get());
    }

    @Test
    void cancelBooking_incrementsSeatsAndCancelsWhenEventExists() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        CollectionReference events = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        AtomicReference<Transaction> transactionRef = new AtomicReference<>();

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("b-3")).thenReturn(bookingRef);
        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-3")).thenReturn(eventRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            transactionRef.set(transaction);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(bookingSnapshot.exists()).thenReturn(true);
            when(bookingSnapshot.getString("status")).thenReturn(Booking.STATUS_CONFIRMED);
            when(bookingSnapshot.getString("eventId")).thenReturn("event-3");
            when(eventSnapshot.exists()).thenReturn(true);
            when(eventSnapshot.getLong("seatsAvailable")).thenReturn(7L);
            function.apply(transaction);
            return immediateSuccessTask(null);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        Booking booking = new Booking();
        booking.setId("b-3");
        CallbackCapture capture = new CallbackCapture();

        service.cancelBooking(booking, capture);

        verify(transactionRef.get()).update(eventRef, "seatsAvailable", 8L);
        verify(transactionRef.get()).update(bookingRef, "status", Booking.STATUS_CANCELLED);
        assertTrue(capture.success);
        assertNull(capture.error);
    }

    @Test
    void cancelBooking_skipsEventLookupWhenEventIdBlank() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        AtomicReference<Transaction> transactionRef = new AtomicReference<>();

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("b-4")).thenReturn(bookingRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            transactionRef.set(transaction);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(bookingSnapshot.exists()).thenReturn(true);
            when(bookingSnapshot.getString("status")).thenReturn(Booking.STATUS_CONFIRMED);
            when(bookingSnapshot.getString("eventId")).thenReturn("  ");
            function.apply(transaction);
            return immediateSuccessTask(null);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        Booking booking = new Booking();
        booking.setId("b-4");
        CallbackCapture capture = new CallbackCapture();

        service.cancelBooking(booking, capture);

        verify(firestore, never()).collection("events");
        assertTrue(capture.success);
    }

    @Test
    void cancelBooking_skipsSeatUpdateWhenEventMissingOrSeatCountNull() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        CollectionReference events = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        AtomicReference<Transaction> transactionRef = new AtomicReference<>();

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("b-5")).thenReturn(bookingRef);
        when(firestore.collection("events")).thenReturn(events);
        when(events.document("event-5")).thenReturn(eventRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction transaction = mock(Transaction.class);
            transactionRef.set(transaction);
            DocumentSnapshot bookingSnapshot = mock(DocumentSnapshot.class);
            DocumentSnapshot eventSnapshot = mock(DocumentSnapshot.class);
            when(transaction.get(bookingRef)).thenReturn(bookingSnapshot);
            when(transaction.get(eventRef)).thenReturn(eventSnapshot);
            when(bookingSnapshot.exists()).thenReturn(true);
            when(bookingSnapshot.getString("status")).thenReturn(Booking.STATUS_CONFIRMED);
            when(bookingSnapshot.getString("eventId")).thenReturn("event-5");
            when(eventSnapshot.exists()).thenReturn(true);
            when(eventSnapshot.getLong("seatsAvailable")).thenReturn(null);
            function.apply(transaction);
            return immediateSuccessTask(null);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        Booking booking = new Booking();
        booking.setId("b-5");
        CallbackCapture capture = new CallbackCapture();

        service.cancelBooking(booking, capture);

        verify(transactionRef.get(), never()).update(eq(eventRef), eq("seatsAvailable"), any());
        verify(transactionRef.get()).update(bookingRef, "status", Booking.STATUS_CANCELLED);
        assertTrue(capture.success);
    }

    @Test
    void cancelBooking_usesFallbackErrorWhenTransactionFailsWithoutMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        Task<Void> failure = immediateFailureTask(new RuntimeException());

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.document("b-6")).thenReturn(bookingRef);
        when(firestore.runTransaction(any())).thenAnswer(invocation -> failure);

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        Booking booking = new Booking();
        booking.setId("b-6");
        AtomicReference<String> error = new AtomicReference<>();

        service.cancelBooking(booking, cancellationCallback(error));

        assertEquals("Unexpected booking error", error.get());
    }

    @Test
    void listenToUserBookings_emitsErrorOnSnapshotFailure() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        Query query = mock(Query.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.whereEqualTo("userId", "user-1")).thenReturn(query);
        when(query.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(null, mock(FirebaseFirestoreException.class));
            return registration;
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();
        AtomicReference<List<Booking>> updates = new AtomicReference<>();

        ListenerRegistration returned = service.listenToUserBookings("user-1", new BookingService.BookingsListener() {
            @Override
            public void onUpdate(List<Booking> bookings) {
                updates.set(bookings);
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertEquals("Failed to load bookings", error.get());
        assertNull(updates.get());
        assertEquals(registration, returned);
    }

    @Test
    void listenToUserBookings_handlesNullSnapshotAsEmptyList() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        Query query = mock(Query.class);

        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.whereEqualTo("userId", "user-2")).thenReturn(query);
        when(query.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(null, null);
            return mock(ListenerRegistration.class);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<List<Booking>> updates = new AtomicReference<>();

        service.listenToUserBookings("user-2", new BookingService.BookingsListener() {
            @Override
            public void onUpdate(List<Booking> bookings) {
                updates.set(bookings);
            }

            @Override
            public void onError(String message) {
            }
        });

        assertNotNull(updates.get());
        assertTrue(updates.get().isEmpty());
    }

    @Test
    void listenToUserBookings_mapsSkipsNullObjectsAndSortsByCreatedAt() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference bookings = mock(CollectionReference.class);
        Query query = mock(Query.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        DocumentSnapshot d1 = mock(DocumentSnapshot.class);
        DocumentSnapshot d2 = mock(DocumentSnapshot.class);
        DocumentSnapshot d3 = mock(DocumentSnapshot.class);
        DocumentSnapshot d4 = mock(DocumentSnapshot.class);
        DocumentSnapshot d5 = mock(DocumentSnapshot.class);

        Booking b1 = new Booking();
        b1.setCreatedAt(null);
        Booking b2 = new Booking();
        b2.setCreatedAt(new Timestamp(new Date(System.currentTimeMillis() - 10_000L)));
        Booking b3 = new Booking();
        b3.setCreatedAt(new Timestamp(new Date(System.currentTimeMillis() - 1_000L)));
        Booking b5 = new Booking();
        b5.setCreatedAt(null);

        when(d1.toObject(Booking.class)).thenReturn(b1);
        when(d1.getId()).thenReturn("b1");
        when(d2.toObject(Booking.class)).thenReturn(b2);
        when(d2.getId()).thenReturn("b2");
        when(d3.toObject(Booking.class)).thenReturn(b3);
        when(d3.getId()).thenReturn("b3");
        when(d4.toObject(Booking.class)).thenReturn(null);
        when(d5.toObject(Booking.class)).thenReturn(b5);
        when(d5.getId()).thenReturn("b5");

        when(snapshot.getDocuments()).thenReturn(Arrays.asList(d1, d2, d3, d4, d5));
        when(firestore.collection("bookings")).thenReturn(bookings);
        when(bookings.whereEqualTo("userId", "user-3")).thenReturn(query);
        when(query.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(snapshot, null);
            return mock(ListenerRegistration.class);
        });

        BookingService service = newBookingServiceWithMockedFirestore(firestore);
        AtomicReference<List<Booking>> updates = new AtomicReference<>();

        service.listenToUserBookings("user-3", new BookingService.BookingsListener() {
            @Override
            public void onUpdate(List<Booking> bookings) {
                updates.set(bookings);
            }

            @Override
            public void onError(String message) {
            }
        });

        assertEquals(4, updates.get().size());
        assertEquals("b3", updates.get().get(0).getId());
        assertEquals("b2", updates.get().get(1).getId());
        assertEquals("b1", updates.get().get(2).getId());
        assertEquals("b5", updates.get().get(3).getId());
    }

    @Test
    void privateHelpers_coverBuildIdConfirmationCodeChannelAndReadableError() throws Exception {
        BookingService service = newBookingServiceWithMockedFirestore();

        Method buildBookingId = BookingService.class.getDeclaredMethod("buildBookingId", String.class, String.class);
        buildBookingId.setAccessible(true);
        assertEquals("u_e", buildBookingId.invoke(service, "u", "e"));

        Method generateCode = BookingService.class.getDeclaredMethod("generateConfirmationCode");
        generateCode.setAccessible(true);
        String code = (String) generateCode.invoke(service);
        assertEquals(10, code.length());
        assertEquals(code.toUpperCase(), code);
        assertFalse(code.contains("-"));

        Method normalizeChannel = BookingService.class.getDeclaredMethod("normalizeChannel", String.class);
        normalizeChannel.setAccessible(true);
        assertEquals("EMAIL", normalizeChannel.invoke(service, new Object[]{null}));
        assertEquals("SMS", normalizeChannel.invoke(service, " sms "));
        assertEquals("EMAIL", normalizeChannel.invoke(service, "push"));

        Method readableError = BookingService.class.getDeclaredMethod("getReadableError", Exception.class);
        readableError.setAccessible(true);
        assertEquals("detail", readableError.invoke(service, new RuntimeException("detail")));
        assertEquals("Unexpected booking error", readableError.invoke(service, new RuntimeException()));
    }

    private BookingService newBookingServiceWithMockedFirestore() throws Exception {
        return newBookingServiceWithMockedFirestore(mock(FirebaseFirestore.class));
    }

    private BookingService newBookingServiceWithMockedFirestore(FirebaseFirestore firestore) throws Exception {
        resetBookingServiceSingleton();
        try (MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            return BookingService.getInstance();
        }
    }

    private void resetBookingServiceSingleton() throws Exception {
        Field instance = BookingService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private Event eventWithId(String id) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Sample Event");
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86400000L)));
        event.setVenue("Sample Venue");
        event.setCity("Montreal");
        event.setCategory("Music");
        event.setPrice(10.0);
        event.setSeatsAvailable(10);
        return event;
    }

    private BookingService.ReservationCallback reservationCallback(AtomicReference<String> error) {
        return new BookingService.ReservationCallback() {
            @Override
            public void onSuccess(Booking booking) {
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        };
    }

    private BookingService.UpdateCallback updateCallback(AtomicReference<String> error) {
        return new BookingService.UpdateCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        };
    }

    private BookingService.CancellationCallback cancellationCallback(AtomicReference<String> error) {
        return new BookingService.CancellationCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Task<T> immediateSuccessTask(T result) {
        Task<T> task = mock(Task.class);
        doAnswer(invocation -> {
            OnSuccessListener listener = invocation.getArgument(0);
            if (listener != null) {
                listener.onSuccess(result);
            }
            return task;
        }).when(task).addOnSuccessListener(org.mockito.ArgumentMatchers.any(OnSuccessListener.class));
        doAnswer(invocation -> task)
                .when(task)
                .addOnFailureListener(org.mockito.ArgumentMatchers.any(OnFailureListener.class));
        return task;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Task<T> immediateFailureTask(Exception exception) {
        Task<T> task = mock(Task.class);
        doAnswer(invocation -> task)
                .when(task)
                .addOnSuccessListener(org.mockito.ArgumentMatchers.any(OnSuccessListener.class));
        doAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            if (listener != null) {
                listener.onFailure(exception);
            }
            return task;
        }).when(task).addOnFailureListener(org.mockito.ArgumentMatchers.any(OnFailureListener.class));
        return task;
    }

    private static class CallbackCapture implements BookingService.UpdateCallback, BookingService.CancellationCallback {
        private boolean success;
        private String error;

        @Override
        public void onSuccess() {
            success = true;
        }

        @Override
        public void onError(String message) {
            error = message;
        }
    }

    private static class ReservationCapture implements BookingService.ReservationCallback {
        private Booking booking;
        private String error;

        @Override
        public void onSuccess(Booking booking) {
            this.booking = booking;
        }

        @Override
        public void onError(String message) {
            this.error = message;
        }
    }
}
