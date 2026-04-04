package com.example.ticketapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;

import com.example.ticketapp.models.Booking;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class MockNotificationServiceTest {

    @Test
    void sendBookingConfirmation_rejectsMissingBooking() {
        MockNotificationService service = new MockNotificationService();
        AtomicReference<String> error = new AtomicReference<>();

        service.sendBookingConfirmation(null, "EMAIL", "user@example.com", callback(error));

        assertEquals("Missing booking details for notification", error.get());
    }

    @Test
    void sendBookingConfirmation_usesFallbackDestinationAndInferredSmsChannel() {
        MockNotificationService service = new MockNotificationService();
        Booking booking = booking("ABC123");

        AtomicReference<String> channel = new AtomicReference<>();
        AtomicReference<String> destination = new AtomicReference<>();
        AtomicReference<String> message = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        AtomicLong delay = new AtomicLong(-1L);

        withImmediateMainLooper(() -> service.sendBookingConfirmation(
                booking, "", "  ",
                new MockNotificationService.NotificationCallback() {
                    @Override
                    public void onSuccess(String resolvedChannel, String resolvedDestination, String resolvedMessage) {
                        channel.set(resolvedChannel);
                        destination.set(resolvedDestination);
                        message.set(resolvedMessage);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        error.set(errorMessage);
                    }
                }), delay);

        assertNull(error.get());
        assertEquals("SMS", channel.get());
        assertEquals("in-app inbox", destination.get());
        assertTrue(message.get().contains("Mock SMS notification sent to in-app inbox"));
        assertTrue(message.get().contains("confirmation code ABC123"));
        assertEquals(700L, delay.get());
    }

    @Test
    void sendBookingConfirmation_respectsPreferredSms() {
        assertNotificationSuccess("sms", "5551231234", "SMS", "5551231234");
    }

    @Test
    void sendBookingConfirmation_respectsPreferredEmail() {
        assertNotificationSuccess("EMAIL", "user@example.com", "EMAIL", "user@example.com");
    }

    @Test
    void sendBookingConfirmation_infersEmailFromDestination() {
        assertNotificationSuccess("push", "alerts@example.com", "EMAIL", "alerts@example.com");
    }

    private void assertNotificationSuccess(String preferredChannel,
                                           String destinationInput,
                                           String expectedChannel,
                                           String expectedDestination) {
        MockNotificationService service = new MockNotificationService();
        Booking booking = booking("ZXCV99");

        AtomicReference<String> channel = new AtomicReference<>();
        AtomicReference<String> destination = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        AtomicLong delay = new AtomicLong(-1L);

        withImmediateMainLooper(() -> service.sendBookingConfirmation(
                booking, preferredChannel, "  " + destinationInput + "  ",
                new MockNotificationService.NotificationCallback() {
                    @Override
                    public void onSuccess(String resolvedChannel, String resolvedDestination, String message) {
                        channel.set(resolvedChannel);
                        destination.set(resolvedDestination);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        error.set(errorMessage);
                    }
                }), delay);

        assertNull(error.get());
        assertEquals(expectedChannel, channel.get());
        assertEquals(expectedDestination, destination.get());
        assertEquals(700L, delay.get());
    }

    private Booking booking(String confirmationCode) {
        Booking booking = new Booking();
        booking.setConfirmationCode(confirmationCode);
        return booking;
    }

    private MockNotificationService.NotificationCallback callback(AtomicReference<String> error) {
        return new MockNotificationService.NotificationCallback() {
            @Override
            public void onSuccess(String channel, String destination, String message) {
            }

            @Override
            public void onFailure(String errorMessage) {
                error.set(errorMessage);
            }
        };
    }

    private void withImmediateMainLooper(ThrowingRunnable action, AtomicLong delayCapture) {
        try (MockedStatic<Looper> looperMock = org.mockito.Mockito.mockStatic(Looper.class);
             MockedConstruction<Handler> handlerConstruction = org.mockito.Mockito.mockConstruction(Handler.class,
                     (handlerMock, context) -> when(handlerMock.postDelayed(any(Runnable.class), anyLong()))
                             .thenAnswer(invocation -> {
                                 Runnable runnable = invocation.getArgument(0);
                                 long delayMillis = invocation.getArgument(1);
                                 delayCapture.set(delayMillis);
                                 runnable.run();
                                 return true;
                             }))) {
            looperMock.when(Looper::getMainLooper).thenReturn(mock(Looper.class));
            action.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

