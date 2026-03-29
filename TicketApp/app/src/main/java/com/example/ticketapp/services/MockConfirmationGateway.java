package com.example.ticketapp.services;

import android.os.Handler;
import android.os.Looper;

import com.example.ticketapp.models.Event;

public class MockConfirmationGateway {

    public interface GatewayCallback {
        void onSuccess(String channel, String message);

        void onFailure(String errorMessage);
    }

    public void processReservation(Event event, String contactDestination, String preferredChannel, GatewayCallback callback) {
        if (event == null) {
            callback.onFailure("Invalid event selected");
            return;
        }

        String normalizedChannel = normalizeChannel(preferredChannel, contactDestination);
        String destination = (contactDestination == null || contactDestination.trim().isEmpty())
                ? "in-app inbox"
                : contactDestination.trim();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            String message = "Mock " + normalizedChannel + " confirmation sent to " + destination;
            callback.onSuccess(normalizedChannel, message);
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

        if (destination != null && destination.contains("@")) {
            return "EMAIL";
        }
        return "SMS";
    }
}
