package com.example.ticketapp.services;

import com.example.ticketapp.UserSession;
import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventAdminService {

    public interface AdminActionCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface EventListListener {
        void onUpdate(List<Event> events);

        void onError(String message);
    }

    private interface CompletionCallback {
        void onComplete(String errorMessage);
    }

    private static EventAdminService instance;
    private final FirebaseFirestore db;

    private EventAdminService() {
        db = FirebaseFirestore.getInstance();
    }

    public static EventAdminService getInstance() {
        if (instance == null) {
            instance = new EventAdminService();
        }
        return instance;
    }

    public ListenerRegistration listenToAllEvents(EventListListener listener) {
        return db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError("Failed to load events");
                        return;
                    }

                    List<Event> events = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Event event = doc.toObject(Event.class);
                            event.setId(doc.getId());
                            events.add(event);
                        }
                    }
                    listener.onUpdate(events);
                });
    }

    // Admin POST endpoint equivalent
    public void postEvent(Event event, AdminActionCallback callback) {
        if (!isAdmin()) {
            callback.onError("Admin access required");
            return;
        }

        String validationError = validateEvent(event);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        Map<String, Object> eventData = toEventMap(event);
        eventData.put("createdAt", FieldValue.serverTimestamp());
        eventData.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(readableError(e, "Failed to create event")));
    }

    // Admin PUT endpoint equivalent
    public void putEvent(String eventId, Event updatedEvent, AdminActionCallback callback) {
        if (!isAdmin()) {
            callback.onError("Admin access required");
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError("Missing event id");
            return;
        }

        String validationError = validateEvent(updatedEvent);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        Map<String, Object> eventData = toEventMap(updatedEvent);
        eventData.put("updatedAt", FieldValue.serverTimestamp());

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Event not found");
            }
            transaction.update(eventRef, eventData);
            return null;
        }).addOnSuccessListener(unused -> {
            String title = safeText(updatedEvent.getTitle(), "Untitled Event");
            String message = "Event \"" + title + "\" was updated. Please review the latest details.";
            notifyTicketHoldersOfUpdate(eventId, updatedEvent, message, error -> {
                if (error != null) {
                    callback.onError(error);
                    return;
                }
                callback.onSuccess();
            });
        }).addOnFailureListener(e -> callback.onError(readableError(e, "Failed to update event")));
    }

    // Admin DELETE endpoint equivalent
    public void deleteEvent(String eventId, AdminActionCallback callback) {
        if (!isAdmin()) {
            callback.onError("Admin access required");
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onError("Missing event id");
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        eventRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                callback.onError("Event not found");
                return;
            }

            String title = safeText(snapshot.getString("title"), "This event");
            eventRef.delete().addOnSuccessListener(unused -> {
                String message = "Event \"" + title + "\" was cancelled. Your ticket has been cancelled.";
                notifyTicketHoldersOfCancellation(eventId, title, message, error -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    callback.onSuccess();
                });
            }).addOnFailureListener(e -> callback.onError(readableError(e, "Failed to delete event")));
        }).addOnFailureListener(e -> callback.onError(readableError(e, "Failed to load event")));
    }

    private void notifyTicketHoldersOfUpdate(String eventId, Event updatedEvent, String message,
                                             CompletionCallback callback) {
        db.collection("bookings")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", Booking.STATUS_CONFIRMED)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(null);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot bookingDoc : snapshot) {
                        DocumentReference bookingRef = bookingDoc.getReference();
                        batch.update(bookingRef,
                                "eventTitle", updatedEvent.getTitle(),
                                "eventDate", updatedEvent.getDate(),
                                "venue", updatedEvent.getVenue(),
                                "city", updatedEvent.getCity(),
                                "price", updatedEvent.getPrice(),
                                "notificationStatus", Booking.NOTIFICATION_STATUS_SENT,
                                "notificationMessage", message,
                                "notificationSentAt", FieldValue.serverTimestamp());

                        createNotificationWrite(batch, bookingDoc, eventId, updatedEvent.getTitle(), message, "EVENT_UPDATED");
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onComplete(null))
                            .addOnFailureListener(e -> callback.onComplete(readableError(e, "Failed to notify ticket holders")));
                })
                .addOnFailureListener(e -> callback.onComplete(readableError(e, "Failed to find ticket holders")));
    }

    private void notifyTicketHoldersOfCancellation(String eventId, String title, String message,
                                                   CompletionCallback callback) {
        db.collection("bookings")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", Booking.STATUS_CONFIRMED)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onComplete(null);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot bookingDoc : snapshot) {
                        DocumentReference bookingRef = bookingDoc.getReference();
                        batch.update(bookingRef,
                                "status", Booking.STATUS_CANCELLED,
                                "cancelledAt", FieldValue.serverTimestamp(),
                                "notificationStatus", Booking.NOTIFICATION_STATUS_SENT,
                                "notificationMessage", message,
                                "notificationSentAt", FieldValue.serverTimestamp());

                        createNotificationWrite(batch, bookingDoc, eventId, title, message, "EVENT_CANCELLED");
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onComplete(null))
                            .addOnFailureListener(e -> callback.onComplete(readableError(e, "Failed to notify ticket holders")));
                })
                .addOnFailureListener(e -> callback.onComplete(readableError(e, "Failed to find ticket holders")));
    }

    private void createNotificationWrite(WriteBatch batch, QueryDocumentSnapshot bookingDoc, String eventId,
                                         String eventTitle, String message, String type) {
        String userId = bookingDoc.getString("userId");
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        DocumentReference notificationRef = db.collection("user_notifications").document();
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("eventId", eventId);
        payload.put("eventTitle", safeText(eventTitle, "Event"));
        payload.put("type", type);
        payload.put("message", message);
        payload.put("isRead", false);
        payload.put("createdAt", FieldValue.serverTimestamp());
        batch.set(notificationRef, payload);
    }

    private Map<String, Object> toEventMap(Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", safeText(event.getTitle(), ""));
        data.put("date", event.getDate());
        data.put("venue", safeText(event.getVenue(), ""));
        data.put("city", safeText(event.getCity(), ""));
        data.put("category", safeText(event.getCategory(), "General"));
        data.put("price", event.getPrice());
        data.put("seatsAvailable", Math.max(0, event.getSeatsAvailable()));
        return data;
    }

    private String validateEvent(Event event) {
        if (event == null) {
            return "Missing event data";
        }
        if (isBlank(event.getTitle())) {
            return "Event title is required";
        }
        Timestamp date = event.getDate();
        if (date == null) {
            return "Event date is required";
        }
        if (isBlank(event.getVenue())) {
            return "Venue is required";
        }
        if (isBlank(event.getCity())) {
            return "City is required";
        }
        if (event.getPrice() < 0) {
            return "Price cannot be negative";
        }
        if (event.getSeatsAvailable() < 0) {
            return "Seats cannot be negative";
        }
        return null;
    }

    private boolean isAdmin() {
        return UserSession.getInstance().isAdmin();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String readableError(Exception e, String fallback) {
        if (e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return e.getMessage();
        }
        return fallback;
    }
}
