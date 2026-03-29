package com.example.ticketapp.services;

import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BookingService {

    public interface UpdateCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface ReservationCallback {
        void onSuccess(Booking booking);

        void onError(String message);
    }

    public interface CancellationCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface BookingsListener {
        void onUpdate(List<Booking> bookings);

        void onError(String message);
    }

    private static BookingService instance;
    private final FirebaseFirestore db;

    private BookingService() {
        db = FirebaseFirestore.getInstance();
    }

    public static BookingService getInstance() {
        if (instance == null) {
            instance = new BookingService();
        }
        return instance;
    }

    public void reserveTicket(Event event, String userId, String contactDestination, String channel,
                              String paymentMethod, String paymentReference, ReservationCallback callback) {
        if (event == null || event.getId() == null || event.getId().trim().isEmpty()) {
            callback.onError("Event is not available for booking");
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("Missing user information");
            return;
        }

        String bookingId = buildBookingId(userId, event.getId());
        String confirmationCode = generateConfirmationCode();
        String normalizedChannel = normalizeChannel(channel);
        String safeDestination = (contactDestination == null) ? "" : contactDestination.trim();

        DocumentReference eventRef = db.collection("events").document(event.getId());
        DocumentReference bookingRef = db.collection("bookings").document(bookingId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            if (!eventSnapshot.exists()) {
                throw new IllegalStateException("Event no longer exists");
            }

            DocumentSnapshot bookingSnapshot = transaction.get(bookingRef);
            if (bookingSnapshot.exists()) {
                String status = bookingSnapshot.getString("status");
                if (Booking.STATUS_CONFIRMED.equals(status)) {
                    throw new IllegalStateException("You already reserved this ticket");
                }
            }

            Long seatsAvailable = eventSnapshot.getLong("seatsAvailable");
            if (seatsAvailable != null && seatsAvailable <= 0) {
                throw new IllegalStateException("Tickets are sold out");
            }

            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("eventId", event.getId());
            bookingData.put("eventTitle", event.getTitle());
            bookingData.put("eventDate", event.getDate());
            bookingData.put("venue", event.getVenue());
            bookingData.put("city", event.getCity());
            bookingData.put("price", event.getPrice());
            bookingData.put("userId", userId);
            bookingData.put("status", Booking.STATUS_CONFIRMED);
            bookingData.put("confirmationCode", confirmationCode);
            bookingData.put("confirmationChannel", normalizedChannel);
            bookingData.put("contactDestination", safeDestination);
            bookingData.put("paymentMethod", paymentMethod);
            bookingData.put("paymentStatus", Booking.PAYMENT_STATUS_PAID);
            bookingData.put("paymentReference", paymentReference);
            bookingData.put("notificationStatus", Booking.NOTIFICATION_STATUS_PENDING);
            bookingData.put("notificationMessage", "");
            bookingData.put("createdAt", FieldValue.serverTimestamp());
            bookingData.put("cancelledAt", null);
            bookingData.put("notificationSentAt", null);

            transaction.set(bookingRef, bookingData);

            if (seatsAvailable != null) {
                transaction.update(eventRef, "seatsAvailable", seatsAvailable - 1);
            }

            return null;
        }).addOnSuccessListener(unused -> {
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setEventId(event.getId());
            booking.setEventTitle(event.getTitle());
            booking.setEventDate(event.getDate());
            booking.setVenue(event.getVenue());
            booking.setCity(event.getCity());
            booking.setPrice(event.getPrice());
            booking.setUserId(userId);
            booking.setStatus(Booking.STATUS_CONFIRMED);
            booking.setConfirmationCode(confirmationCode);
            booking.setConfirmationChannel(normalizedChannel);
            booking.setContactDestination(safeDestination);
            booking.setPaymentMethod(paymentMethod);
            booking.setPaymentStatus(Booking.PAYMENT_STATUS_PAID);
            booking.setPaymentReference(paymentReference);
            booking.setNotificationStatus(Booking.NOTIFICATION_STATUS_PENDING);
            booking.setNotificationMessage("");
            booking.setCreatedAt(Timestamp.now());
            callback.onSuccess(booking);
        }).addOnFailureListener(e -> callback.onError(getReadableError(e)));
    }

    public void markNotificationSent(String bookingId, String channel, String destination, String message, UpdateCallback callback) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            callback.onError("Missing booking id");
            return;
        }

        db.collection("bookings").document(bookingId)
                .update(
                        "confirmationChannel", normalizeChannel(channel),
                        "contactDestination", destination == null ? "" : destination,
                        "notificationStatus", Booking.NOTIFICATION_STATUS_SENT,
                        "notificationMessage", message == null ? "" : message,
                        "notificationSentAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getReadableError(e)));
    }

    public void cancelBooking(Booking booking, CancellationCallback callback) {
        if (booking == null || booking.getId() == null || booking.getId().trim().isEmpty()) {
            callback.onError("Invalid booking");
            return;
        }

        DocumentReference bookingRef = db.collection("bookings").document(booking.getId());

        db.runTransaction(transaction -> {
            DocumentSnapshot bookingSnapshot = transaction.get(bookingRef);
            if (!bookingSnapshot.exists()) {
                throw new IllegalStateException("Booking not found");
            }

            String status = bookingSnapshot.getString("status");
            if (Booking.STATUS_CANCELLED.equals(status)) {
                throw new IllegalStateException("Booking already cancelled");
            }

            String eventId = bookingSnapshot.getString("eventId");
            if (eventId != null && !eventId.trim().isEmpty()) {
                DocumentReference eventRef = db.collection("events").document(eventId);
                DocumentSnapshot eventSnapshot = transaction.get(eventRef);
                if (eventSnapshot.exists()) {
                    Long seatsAvailable = eventSnapshot.getLong("seatsAvailable");
                    if (seatsAvailable != null) {
                        transaction.update(eventRef, "seatsAvailable", seatsAvailable + 1);
                    }
                }
            }

            transaction.update(bookingRef, "status", Booking.STATUS_CANCELLED);
            transaction.update(bookingRef, "cancelledAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getReadableError(e)));
    }

    public ListenerRegistration listenToUserBookings(String userId, BookingsListener listener) {
        return db.collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError("Failed to load bookings");
                        return;
                    }

                    List<Booking> bookings = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(documentSnapshot -> {
                            Booking booking = documentSnapshot.toObject(Booking.class);
                            if (booking != null) {
                                booking.setId(documentSnapshot.getId());
                                bookings.add(booking);
                            }
                        });
                    }

                    Collections.sort(bookings, new Comparator<Booking>() {
                        @Override
                        public int compare(Booking o1, Booking o2) {
                            Timestamp t1 = o1.getCreatedAt();
                            Timestamp t2 = o2.getCreatedAt();
                            if (t1 == null && t2 == null) {
                                return 0;
                            }
                            if (t1 == null) {
                                return 1;
                            }
                            if (t2 == null) {
                                return -1;
                            }
                            return t2.compareTo(t1);
                        }
                    });

                    listener.onUpdate(bookings);
                });
    }

    private String buildBookingId(String userId, String eventId) {
        return userId + "_" + eventId;
    }

    private String generateConfirmationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            return "EMAIL";
        }
        String value = channel.trim().toUpperCase();
        if ("SMS".equals(value)) {
            return "SMS";
        }
        return "EMAIL";
    }

    private String getReadableError(Exception e) {
        if (e != null && e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return e.getMessage();
        }
        return "Unexpected booking error";
    }
}
