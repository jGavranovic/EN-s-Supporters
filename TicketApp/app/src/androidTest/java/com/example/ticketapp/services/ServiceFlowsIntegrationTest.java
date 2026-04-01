package com.example.ticketapp.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketapp.UserSession;
import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ServiceFlowsIntegrationTest {

    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        db.useEmulator(resolveEmulatorHost(), 8080);
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build());

        resetServiceSingletons();
        clearCollections();
        UserSession.getInstance().logout();
    }

    @After
    public void tearDown() throws Exception {
        clearCollections();
        resetServiceSingletons();
        UserSession.getInstance().logout();
    }

    @Test
    public void reserveAndCancelBooking_integrationFlow_updatesFirestore() throws Exception {
        String eventId = "event-reserve-" + System.currentTimeMillis();
        createEventDocument(eventId, "Reserve Test Event", 3);

        Event event = baseEvent("Reserve Test Event", 49.99, 3);
        event.setId(eventId);

        BookingService bookingService = BookingService.getInstance();

        AtomicReference<Booking> createdBooking = new AtomicReference<>();
        AtomicReference<String> reserveError = new AtomicReference<>();
        CountDownLatch reserveLatch = new CountDownLatch(1);

        bookingService.reserveTicket(event, "user-abc", "user@example.com", "EMAIL", "VISA", "PAY-ABC",
                new BookingService.ReservationCallback() {
                    @Override
                    public void onSuccess(Booking booking) {
                        createdBooking.set(booking);
                        reserveLatch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        reserveError.set(message);
                        reserveLatch.countDown();
                    }
                });

        assertTrue("Reservation callback timed out", reserveLatch.await(10, TimeUnit.SECONDS));
        assertEquals(null, reserveError.get());
        assertNotNull(createdBooking.get());

        String bookingId = "user-abc_" + eventId;
        DocumentSnapshot bookingSnapshot = Tasks.await(db.collection("bookings").document(bookingId).get());
        assertTrue(bookingSnapshot.exists());
        assertEquals(Booking.STATUS_CONFIRMED, bookingSnapshot.getString("status"));
        assertEquals(Booking.PAYMENT_STATUS_PAID, bookingSnapshot.getString("paymentStatus"));

        DocumentSnapshot eventSnapshotAfterReserve = Tasks.await(db.collection("events").document(eventId).get());
        Long seatsAfterReserve = eventSnapshotAfterReserve.getLong("seatsAvailable");
        assertNotNull(seatsAfterReserve);
        assertEquals(2L, seatsAfterReserve.longValue());

        Booking bookingToCancel = new Booking();
        bookingToCancel.setId(bookingId);

        AtomicReference<String> cancelError = new AtomicReference<>();
        CountDownLatch cancelLatch = new CountDownLatch(1);
        bookingService.cancelBooking(bookingToCancel, new BookingService.CancellationCallback() {
            @Override
            public void onSuccess() {
                cancelLatch.countDown();
            }

            @Override
            public void onError(String message) {
                cancelError.set(message);
                cancelLatch.countDown();
            }
        });

        assertTrue("Cancellation callback timed out", cancelLatch.await(10, TimeUnit.SECONDS));
        assertEquals(null, cancelError.get());

        DocumentSnapshot bookingAfterCancel = Tasks.await(db.collection("bookings").document(bookingId).get());
        assertEquals(Booking.STATUS_CANCELLED, bookingAfterCancel.getString("status"));

        DocumentSnapshot eventSnapshotAfterCancel = Tasks.await(db.collection("events").document(eventId).get());
        Long seatsAfterCancel = eventSnapshotAfterCancel.getLong("seatsAvailable");
        assertNotNull(seatsAfterCancel);
        assertEquals(3L, seatsAfterCancel.longValue());
    }

    @Test
    public void adminEditAndCancel_endToEnd_updatesBookingsAndCreatesNotifications() throws Exception {
        UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);

        EventAdminService adminService = EventAdminService.getInstance();

        String originalTitle = "Admin E2E " + System.currentTimeMillis();
        Event createEvent = baseEvent(originalTitle, 35.00, 100);

        AtomicReference<String> createError = new AtomicReference<>();
        CountDownLatch createLatch = new CountDownLatch(1);
        adminService.postEvent(createEvent, new EventAdminService.AdminActionCallback() {
            @Override
            public void onSuccess() {
                createLatch.countDown();
            }

            @Override
            public void onError(String message) {
                createError.set(message);
                createLatch.countDown();
            }
        });

        assertTrue("Create callback timed out", createLatch.await(10, TimeUnit.SECONDS));
        assertEquals(null, createError.get());

        QuerySnapshot eventQuery = Tasks.await(db.collection("events")
                .whereEqualTo("title", originalTitle)
                .limit(1)
                .get());
        assertFalse(eventQuery.isEmpty());
        DocumentSnapshot createdEventDoc = eventQuery.getDocuments().get(0);
        String eventId = createdEventDoc.getId();

        String bookingId = "holder-1_" + eventId;
        Map<String, Object> booking = new HashMap<>();
        booking.put("eventId", eventId);
        booking.put("eventTitle", originalTitle);
        booking.put("eventDate", createEvent.getDate());
        booking.put("venue", createEvent.getVenue());
        booking.put("city", createEvent.getCity());
        booking.put("price", createEvent.getPrice());
        booking.put("userId", "holder-1");
        booking.put("status", Booking.STATUS_CONFIRMED);
        booking.put("notificationStatus", Booking.NOTIFICATION_STATUS_PENDING);
        booking.put("notificationMessage", "");
        Tasks.await(db.collection("bookings").document(bookingId).set(booking));

        Event updatedEvent = baseEvent("Admin E2E Updated", 42.50, 95);

        AtomicReference<String> updateError = new AtomicReference<>();
        CountDownLatch updateLatch = new CountDownLatch(1);
        adminService.putEvent(eventId, updatedEvent, new EventAdminService.AdminActionCallback() {
            @Override
            public void onSuccess() {
                updateLatch.countDown();
            }

            @Override
            public void onError(String message) {
                updateError.set(message);
                updateLatch.countDown();
            }
        });

        assertTrue("Update callback timed out", updateLatch.await(10, TimeUnit.SECONDS));
        assertEquals(null, updateError.get());

        DocumentSnapshot bookingAfterUpdate = Tasks.await(db.collection("bookings").document(bookingId).get());
        assertEquals("Admin E2E Updated", bookingAfterUpdate.getString("eventTitle"));
        assertEquals(Booking.NOTIFICATION_STATUS_SENT, bookingAfterUpdate.getString("notificationStatus"));

        QuerySnapshot updateNotifications = Tasks.await(db.collection("user_notifications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", "holder-1")
                .whereEqualTo("type", "EVENT_UPDATED")
                .get());
        assertFalse(updateNotifications.isEmpty());

        AtomicReference<String> deleteError = new AtomicReference<>();
        CountDownLatch deleteLatch = new CountDownLatch(1);
        adminService.deleteEvent(eventId, new EventAdminService.AdminActionCallback() {
            @Override
            public void onSuccess() {
                deleteLatch.countDown();
            }

            @Override
            public void onError(String message) {
                deleteError.set(message);
                deleteLatch.countDown();
            }
        });

        assertTrue("Delete callback timed out", deleteLatch.await(10, TimeUnit.SECONDS));
        assertEquals(null, deleteError.get());

        DocumentSnapshot deletedEvent = Tasks.await(db.collection("events").document(eventId).get());
        assertFalse(deletedEvent.exists());

        DocumentSnapshot bookingAfterCancel = Tasks.await(db.collection("bookings").document(bookingId).get());
        assertEquals(Booking.STATUS_CANCELLED, bookingAfterCancel.getString("status"));
        assertEquals(Booking.NOTIFICATION_STATUS_SENT, bookingAfterCancel.getString("notificationStatus"));

        QuerySnapshot cancelNotifications = Tasks.await(db.collection("user_notifications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", "holder-1")
                .whereEqualTo("type", "EVENT_CANCELLED")
                .get());
        assertFalse(cancelNotifications.isEmpty());
    }

    private Event baseEvent(String title, double price, int seats) {
        Event event = new Event();
        event.setTitle(title);
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86400000L)));
        event.setVenue("Bell Centre");
        event.setCity("Montreal");
        event.setCategory("Music");
        event.setPrice(price);
        event.setSeatsAvailable(seats);
        return event;
    }

    private void createEventDocument(String eventId, String title, int seatsAvailable) throws Exception {
        Event event = baseEvent(title, 49.99, seatsAvailable);

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", event.getTitle());
        payload.put("date", event.getDate());
        payload.put("venue", event.getVenue());
        payload.put("city", event.getCity());
        payload.put("category", event.getCategory());
        payload.put("price", event.getPrice());
        payload.put("seatsAvailable", seatsAvailable);

        Tasks.await(db.collection("events").document(eventId).set(payload));
    }

    private void clearCollections() throws Exception {
        deleteAllDocuments("user_notifications");
        deleteAllDocuments("bookings");
        deleteAllDocuments("events");
    }

    private void deleteAllDocuments(String collection) throws Exception {
        QuerySnapshot snapshot = Tasks.await(db.collection(collection).get());
        if (snapshot.isEmpty()) {
            return;
        }
        for (QueryDocumentSnapshot doc : snapshot) {
            Tasks.await(doc.getReference().delete());
        }
    }

    private void resetServiceSingletons() throws Exception {
        resetSingleton(BookingService.class, "instance");
        resetSingleton(EventAdminService.class, "instance");
    }

    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }

    private String resolveEmulatorHost() {
        String configured = System.getProperty("firestore.emulator.host");
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        return "10.0.2.2";
    }
}
