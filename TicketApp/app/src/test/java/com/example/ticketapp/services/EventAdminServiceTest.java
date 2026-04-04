package com.example.ticketapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ticketapp.UserSession;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

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

class EventAdminServiceTest {

    @AfterEach
    void tearDown() throws Exception {
        resetEventAdminServiceSingleton();
        UserSession.getInstance().logout();
    }

    @Test
    void listenToAllEvents_emitsErrorWhenFirestoreListenerFails() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(null, mock(FirebaseFirestoreException.class));
            return registration;
        });

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        AtomicReference<String> error = new AtomicReference<>();
        AtomicReference<List<Event>> updates = new AtomicReference<>();

        ListenerRegistration returned = service.listenToAllEvents(new EventAdminService.EventListListener() {
            @Override
            public void onUpdate(List<Event> events) {
                updates.set(events);
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertEquals("Failed to load events", error.get());
        assertNull(updates.get());
        assertEquals(registration, returned);
    }

    @Test
    void listenToAllEvents_handlesNullSnapshotAndReturnsEmptyList() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(null, null);
            return mock(ListenerRegistration.class);
        });

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        AtomicReference<List<Event>> updates = new AtomicReference<>();

        service.listenToAllEvents(new EventAdminService.EventListListener() {
            @Override
            public void onUpdate(List<Event> events) {
                updates.set(events);
            }

            @Override
            public void onError(String message) {
            }
        });

        assertNotNull(updates.get());
        assertTrue(updates.get().isEmpty());
    }

    @Test
    void listenToAllEvents_mapsSnapshotToEventsAndSetsIds() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        Event mapped = validEvent();
        mapped.setId(null);
        when(doc.toObject(Event.class)).thenReturn(mapped);
        when(doc.getId()).thenReturn("event-22");
        when(snapshot.iterator()).thenReturn(Arrays.asList(doc).iterator());

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(snapshot, null);
            return mock(ListenerRegistration.class);
        });

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        AtomicReference<List<Event>> updates = new AtomicReference<>();

        service.listenToAllEvents(new EventAdminService.EventListListener() {
            @Override
            public void onUpdate(List<Event> events) {
                updates.set(events);
            }

            @Override
            public void onError(String message) {
            }
        });

        assertEquals(1, updates.get().size());
        assertEquals("event-22", updates.get().get(0).getId());
    }

    @Test
    void postEvent_requiresAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.USER);

        AtomicReference<String> error = new AtomicReference<>();
        service.postEvent(validEvent(), callback(error));

        assertEquals("Admin access required", error.get());
    }

    @Test
    void postEvent_rejectsInvalidPayloads() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        for (InvalidEventCase testCase : invalidEventCases()) {
            AtomicReference<String> error = new AtomicReference<>();
            service.postEvent(testCase.event, callback(error));
            assertEquals(testCase.expectedError, error.get());
        }
    }

    @Test
    void postEvent_writesEventAndInvokesSuccess() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        Task<DocumentReference> addTask = immediateSuccessTask(docRef);

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.add(any())).thenReturn(addTask);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        Event event = validEvent();
        event.setCategory(" ");

        CallbackCapture capture = new CallbackCapture();
        service.postEvent(event, capture);

        verify(eventsCollection).add(argThat(raw -> {
            Map<String, Object> payload = (Map<String, Object>) raw;
            return "Rock Night".equals(payload.get("title"))
                    && "Bell Centre".equals(payload.get("venue"))
                    && "Montreal".equals(payload.get("city"))
                    && "General".equals(payload.get("category"))
                    && payload.get("createdAt") != null
                    && payload.get("updatedAt") != null;
        }));
        assertTrue(capture.success);
        assertNull(capture.error);
    }

    @Test
    void postEvent_surfacesReadableFailureMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        Task<DocumentReference> addTask = immediateFailureTask(new RuntimeException("no write permission"));

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.add(any())).thenReturn(addTask);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.postEvent(validEvent(), callback(error));

        assertEquals("no write permission", error.get());
    }

    @Test
    void postEvent_usesFallbackErrorWhenExceptionHasNoMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        Task<DocumentReference> addTask = immediateFailureTask(new RuntimeException());

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.add(any())).thenReturn(addTask);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.postEvent(validEvent(), callback(error));

        assertEquals("Failed to create event", error.get());
    }

    @Test
    void putEvent_requiresAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.USER);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("event-1", validEvent(), callback(error));

        assertEquals("Admin access required", error.get());
    }

    @Test
    void putEvent_rejectsMissingEventIdForAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("  ", validEvent(), callback(error));

        assertEquals("Missing event id", error.get());
    }

    @Test
    void putEvent_failsWhenTransactionCannotFindEvent() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-1")).thenReturn(eventRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction tx = mock(Transaction.class);
            DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
            when(tx.get(eventRef)).thenReturn(snapshot);
            when(snapshot.exists()).thenReturn(false);
            try {
                function.apply(tx);
                return immediateSuccessTask(null);
            } catch (Exception exception) {
                return immediateFailureTask(exception);
            }
        });

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("event-1", validEvent(), callback(error));

        assertEquals("Event not found", error.get());
    }

    @Test
    void putEvent_usesFallbackErrorWhenTransactionFailsWithoutMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-2")).thenReturn(eventRef);
        Task<Void> transactionFailure = immediateFailureTask(new RuntimeException());
        when(firestore.runTransaction(any())).thenAnswer(invocation -> transactionFailure);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("event-2", validEvent(), callback(error));

        assertEquals("Failed to update event", error.get());
    }

    @Test
    void putEvent_updatesEventAndSucceedsWhenNoConfirmedBookings() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        CollectionReference bookingsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        Query bookingsByEvent = mock(Query.class);
        Query confirmedBookings = mock(Query.class);
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-5")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookingsCollection);
        when(bookingsCollection.whereEqualTo("eventId", "event-5")).thenReturn(bookingsByEvent);
        when(bookingsByEvent.whereEqualTo("status", Booking.STATUS_CONFIRMED)).thenReturn(confirmedBookings);
        Task<QuerySnapshot> emptyBookingsTask = immediateSuccessTask(emptySnapshot);
        when(confirmedBookings.get()).thenReturn(emptyBookingsTask);
        when(emptySnapshot.isEmpty()).thenReturn(true);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction tx = mock(Transaction.class);
            DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
            when(tx.get(eventRef)).thenReturn(snapshot);
            when(snapshot.exists()).thenReturn(true);
            function.apply(tx);
            return immediateSuccessTask(null);
        });

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        CallbackCapture capture = new CallbackCapture();
        service.putEvent("event-5", validEvent(), capture);

        assertTrue(capture.success);
        assertNull(capture.error);
    }

    @Test
    void putEvent_propagatesNotificationBatchFailure() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        CollectionReference bookingsCollection = mock(CollectionReference.class);
        CollectionReference notificationsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        Query bookingsByEvent = mock(Query.class);
        Query confirmedBookings = mock(Query.class);
        QuerySnapshot bookingSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot bookingDoc = mock(QueryDocumentSnapshot.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        DocumentReference notificationRef = mock(DocumentReference.class);
        WriteBatch batch = mock(WriteBatch.class);

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-8")).thenReturn(eventRef);
        when(firestore.collection("bookings")).thenReturn(bookingsCollection);
        when(bookingsCollection.whereEqualTo("eventId", "event-8")).thenReturn(bookingsByEvent);
        when(bookingsByEvent.whereEqualTo("status", Booking.STATUS_CONFIRMED)).thenReturn(confirmedBookings);
        Task<QuerySnapshot> bookingsTask = immediateSuccessTask(bookingSnapshot);
        when(confirmedBookings.get()).thenReturn(bookingsTask);
        when(bookingSnapshot.isEmpty()).thenReturn(false);
        when(bookingSnapshot.iterator()).thenReturn(Arrays.asList(bookingDoc).iterator());

        when(bookingDoc.getReference()).thenReturn(bookingRef);
        when(bookingDoc.getString("userId")).thenReturn("user-9");
        when(firestore.batch()).thenReturn(batch);
        Task<Void> batchFailureTask = immediateFailureTask(new RuntimeException());
        when(batch.commit()).thenReturn(batchFailureTask);

        when(firestore.collection("user_notifications")).thenReturn(notificationsCollection);
        when(notificationsCollection.document()).thenReturn(notificationRef);

        when(firestore.runTransaction(any())).thenAnswer(invocation -> {
            Transaction.Function<Void> function = invocation.getArgument(0);
            Transaction tx = mock(Transaction.class);
            DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
            when(tx.get(eventRef)).thenReturn(snapshot);
            when(snapshot.exists()).thenReturn(true);
            function.apply(tx);
            return immediateSuccessTask(null);
        });

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("event-8", validEvent(), callback(error));

        assertEquals("Failed to notify ticket holders", error.get());
    }

    @Test
    void deleteEvent_requiresAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.USER);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("event-1", callback(error));

        assertEquals("Admin access required", error.get());
    }

    @Test
    void deleteEvent_rejectsMissingEventIdForAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("", callback(error));

        assertEquals("Missing event id", error.get());
    }

    @Test
    void deleteEvent_returnsErrorWhenEventLookupFails() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-50")).thenReturn(eventRef);
        Task<DocumentSnapshot> eventGetFailure = immediateFailureTask(new RuntimeException());
        when(eventRef.get()).thenReturn(eventGetFailure);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("event-50", callback(error));

        assertEquals("Failed to load event", error.get());
    }

    @Test
    void deleteEvent_returnsErrorWhenEventDoesNotExist() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-51")).thenReturn(eventRef);
        Task<DocumentSnapshot> eventGetTask = immediateSuccessTask(snapshot);
        when(eventRef.get()).thenReturn(eventGetTask);
        when(snapshot.exists()).thenReturn(false);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("event-51", callback(error));

        assertEquals("Event not found", error.get());
    }

    @Test
    void deleteEvent_returnsDeleteFailureMessage() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-52")).thenReturn(eventRef);
        Task<DocumentSnapshot> eventGetTask = immediateSuccessTask(snapshot);
        when(eventRef.get()).thenReturn(eventGetTask);
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getString("title")).thenReturn("Expo");
        Task<Void> deleteFailureTask = immediateFailureTask(new RuntimeException("delete blocked"));
        when(eventRef.delete()).thenReturn(deleteFailureTask);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("event-52", callback(error));

        assertEquals("delete blocked", error.get());
    }

    @Test
    void deleteEvent_cancelsConfirmedBookingsAndHandlesBatchFailure() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference eventsCollection = mock(CollectionReference.class);
        CollectionReference bookingsCollection = mock(CollectionReference.class);
        CollectionReference notificationsCollection = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        Query bookingsByEvent = mock(Query.class);
        Query confirmedBookings = mock(Query.class);
        QuerySnapshot bookingsSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot bookingDoc = mock(QueryDocumentSnapshot.class);
        DocumentReference bookingRef = mock(DocumentReference.class);
        DocumentReference notificationRef = mock(DocumentReference.class);
        WriteBatch batch = mock(WriteBatch.class);

        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(eventsCollection.document("event-53")).thenReturn(eventRef);
        Task<DocumentSnapshot> eventGetTask = immediateSuccessTask(snapshot);
        when(eventRef.get()).thenReturn(eventGetTask);
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getString("title")).thenReturn("Expo");
        Task<Void> deleteSuccessTask = immediateSuccessTask(null);
        when(eventRef.delete()).thenReturn(deleteSuccessTask);

        when(firestore.collection("bookings")).thenReturn(bookingsCollection);
        when(bookingsCollection.whereEqualTo("eventId", "event-53")).thenReturn(bookingsByEvent);
        when(bookingsByEvent.whereEqualTo("status", Booking.STATUS_CONFIRMED)).thenReturn(confirmedBookings);
        Task<QuerySnapshot> bookingsTask = immediateSuccessTask(bookingsSnapshot);
        when(confirmedBookings.get()).thenReturn(bookingsTask);
        when(bookingsSnapshot.isEmpty()).thenReturn(false);
        when(bookingsSnapshot.iterator()).thenReturn(Arrays.asList(bookingDoc).iterator());
        when(bookingDoc.getReference()).thenReturn(bookingRef);
        when(bookingDoc.getString("userId")).thenReturn("user-100");

        when(firestore.batch()).thenReturn(batch);
        Task<Void> batchFailureTask = immediateFailureTask(new RuntimeException("batch failed"));
        when(batch.commit()).thenReturn(batchFailureTask);

        when(firestore.collection("user_notifications")).thenReturn(notificationsCollection);
        when(notificationsCollection.document()).thenReturn(notificationRef);

        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("event-53", callback(error));

        assertEquals("batch failed", error.get());
    }

    @Test
    void createNotificationWrite_skipsWhenBookingHasNoUser() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);

        WriteBatch batch = mock(WriteBatch.class);
        QueryDocumentSnapshot bookingDoc = mock(QueryDocumentSnapshot.class);
        when(bookingDoc.getString("userId")).thenReturn("   ");

        Method createNotificationWrite = EventAdminService.class.getDeclaredMethod(
                "createNotificationWrite",
                WriteBatch.class,
                QueryDocumentSnapshot.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        createNotificationWrite.setAccessible(true);
        createNotificationWrite.invoke(service, batch, bookingDoc, "event-1", "Show", "msg", "EVENT_UPDATED");

        verify(batch, never()).set(any(), any());
    }

    @Test
    void notificationUpdates_writePayloadForTicketHolder() throws Exception {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        EventAdminService service = newAdminServiceWithMockedFirestore(firestore);

        WriteBatch batch = mock(WriteBatch.class);
        QueryDocumentSnapshot bookingDoc = mock(QueryDocumentSnapshot.class);
        CollectionReference notifications = mock(CollectionReference.class);
        DocumentReference notificationRef = mock(DocumentReference.class);

        when(bookingDoc.getString("userId")).thenReturn("user-123");
        when(firestore.collection("user_notifications")).thenReturn(notifications);
        when(notifications.document()).thenReturn(notificationRef);

        Method createNotificationWrite = EventAdminService.class.getDeclaredMethod(
                "createNotificationWrite",
                WriteBatch.class,
                QueryDocumentSnapshot.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        createNotificationWrite.setAccessible(true);
        createNotificationWrite.invoke(service, batch, bookingDoc, "event-55", "Mega Show", "Updated details", "EVENT_UPDATED");

        verify(batch).set(eq(notificationRef), argThat(payload -> hasExpectedPayload((Map<String, Object>) payload)));
    }

    @Test
    void privateHelpers_toEventMapAndReadableErrorBranches() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();

        Event raw = new Event();
        raw.setTitle("   ");
        raw.setVenue(null);
        raw.setCity("  ");
        raw.setCategory(null);
        raw.setSeatsAvailable(-4);
        raw.setPrice(42.5);

        Method toEventMap = EventAdminService.class.getDeclaredMethod("toEventMap", Event.class);
        toEventMap.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) toEventMap.invoke(service, raw);
        assertEquals("", payload.get("title"));
        assertEquals("", payload.get("venue"));
        assertEquals("", payload.get("city"));
        assertEquals("General", payload.get("category"));
        assertEquals(0, payload.get("seatsAvailable"));

        Method readableError = EventAdminService.class.getDeclaredMethod("readableError", Exception.class, String.class);
        readableError.setAccessible(true);
        String withMessage = (String) readableError.invoke(service, new RuntimeException("specific"), "fallback");
        String fallback = (String) readableError.invoke(service, new RuntimeException(), "fallback");
        assertEquals("specific", withMessage);
        assertEquals("fallback", fallback);
    }

    private boolean hasExpectedPayload(Map<String, Object> payload) {
        return "user-123".equals(payload.get("userId"))
                && "event-55".equals(payload.get("eventId"))
                && "Mega Show".equals(payload.get("eventTitle"))
                && "EVENT_UPDATED".equals(payload.get("type"))
                && "Updated details".equals(payload.get("message"))
                && Boolean.FALSE.equals(payload.get("isRead"));
    }

    private static List<InvalidEventCase> invalidEventCases() {
        Event nullEvent = null;

        Event missingTitle = validEventStatic();
        missingTitle.setTitle(" ");

        Event missingDate = validEventStatic();
        missingDate.setDate(null);

        Event missingVenue = validEventStatic();
        missingVenue.setVenue("\t");

        Event missingCity = validEventStatic();
        missingCity.setCity(" ");

        Event negativePrice = validEventStatic();
        negativePrice.setPrice(-1);

        Event negativeSeats = validEventStatic();
        negativeSeats.setSeatsAvailable(-3);

        return Arrays.asList(
                new InvalidEventCase(nullEvent, "Missing event data"),
                new InvalidEventCase(missingTitle, "Event title is required"),
                new InvalidEventCase(missingDate, "Event date is required"),
                new InvalidEventCase(missingVenue, "Venue is required"),
                new InvalidEventCase(missingCity, "City is required"),
                new InvalidEventCase(negativePrice, "Price cannot be negative"),
                new InvalidEventCase(negativeSeats, "Seats cannot be negative")
        );
    }

    private EventAdminService.AdminActionCallback callback(AtomicReference<String> error) {
        return new EventAdminService.AdminActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        };
    }

    private Event validEvent() {
        return validEventStatic();
    }

    private static Event validEventStatic() {
        Event event = new Event();
        event.setTitle("Rock Night");
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86400000L)));
        event.setVenue("Bell Centre");
        event.setCity("Montreal");
        event.setCategory("Music");
        event.setPrice(35.0);
        event.setSeatsAvailable(100);
        return event;
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
                })
                .when(task)
                .addOnSuccessListener(org.mockito.ArgumentMatchers.any(OnSuccessListener.class));
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
                })
                .when(task)
                .addOnFailureListener(org.mockito.ArgumentMatchers.any(OnFailureListener.class));
        return task;
    }

    private EventAdminService newAdminServiceWithMockedFirestore() throws Exception {
        return newAdminServiceWithMockedFirestore(mock(FirebaseFirestore.class));
    }

    private EventAdminService newAdminServiceWithMockedFirestore(FirebaseFirestore firestore) throws Exception {
        resetEventAdminServiceSingleton();
        try (MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            return EventAdminService.getInstance();
        }
    }

    private void resetEventAdminServiceSingleton() throws Exception {
        Field instance = EventAdminService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static class CallbackCapture implements EventAdminService.AdminActionCallback {
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

    private static class InvalidEventCase {
        private final Event event;
        private final String expectedError;

        private InvalidEventCase(Event event, String expectedError) {
            this.event = event;
            this.expectedError = expectedError;
        }
    }
}
