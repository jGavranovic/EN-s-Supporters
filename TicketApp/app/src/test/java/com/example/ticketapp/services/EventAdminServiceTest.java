package com.example.ticketapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ticketapp.UserSession;
import com.example.ticketapp.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class EventAdminServiceTest {

    @AfterEach
    void tearDown() throws Exception {
        resetEventAdminServiceSingleton();
        UserSession.getInstance().logout();
    }

    @Test
    void createEvent_requiresAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.USER);

        AtomicReference<String> error = new AtomicReference<>();
        service.postEvent(validEvent(), callback(error));

        assertEquals("Admin access required", error.get());
    }

    @Test
    void createEvent_rejectsInvalidEventForAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        Event invalid = validEvent();
        invalid.setTitle(" ");

        AtomicReference<String> error = new AtomicReference<>();
        service.postEvent(invalid, callback(error));

        assertEquals("Event title is required", error.get());
    }

    @Test
    void editEvent_requiresAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.USER);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("event-1", validEvent(), callback(error));

        assertEquals("Admin access required", error.get());
    }

    @Test
    void editEvent_rejectsMissingEventIdForAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.putEvent("  ", validEvent(), callback(error));

        assertEquals("Missing event id", error.get());
    }

    @Test
    void cancelEvent_requiresAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.USER);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("event-1", callback(error));

        assertEquals("Admin access required", error.get());
    }

    @Test
    void cancelEvent_rejectsMissingEventIdForAdmin() throws Exception {
        EventAdminService service = newAdminServiceWithMockedFirestore();
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        AtomicReference<String> error = new AtomicReference<>();
        service.deleteEvent("", callback(error));

        assertEquals("Missing event id", error.get());
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

    private boolean hasExpectedPayload(Map<String, Object> payload) {
        return "user-123".equals(payload.get("userId"))
                && "event-55".equals(payload.get("eventId"))
                && "Mega Show".equals(payload.get("eventTitle"))
                && "EVENT_UPDATED".equals(payload.get("type"))
                && "Updated details".equals(payload.get("message"))
                && Boolean.FALSE.equals(payload.get("isRead"));
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
}
