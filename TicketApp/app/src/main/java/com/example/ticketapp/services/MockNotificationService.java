package com.example.ticketapp.services;

import android.os.Handler;
import android.os.Looper;

import com.example.ticketapp.models.Booking;

public class MockNotificationService {

    public interface NotificationCallback {
        void onSuccess(String channel, String destination, String message);

        void onFailure(String errorMessage);
    }

    public void sendBookingConfirmation(Booking booking, String preferredChannel, String destination, NotificationCallback callback) {
        if (booking == null) {
            callback.onFailure("Missing booking details for notification");
            return;
        }

        String resolvedDestination = (destination == null || destination.trim().isEmpty())
                ? "in-app inbox"
                : destination.trim();
        String resolvedChannel = normalizeChannel(preferredChannel, resolvedDestination);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            String message = "Mock " + resolvedChannel + " notification sent to " + resolvedDestination
                    + " with confirmation code " + booking.getConfirmationCode();
            callback.onSuccess(resolvedChannel, resolvedDestination, message);
        }, 700);
    }

    private String normalizeChannel(String preferredChannel, String destination) {
        String safeChannel = preferredChannel == null ? "" : preferredChannel.trim().toUpperCase();
        if ("SMS".equals(safeChannel)) {
            return "SMS";
        }
        if ("EMAIL".equals(safeChannel)) {
            return "EMAIL";
        }
        if (destination.contains("@")) {
            return "EMAIL";
        }
        return "SMS";
    }
}
