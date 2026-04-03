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

import com.example.ticketapp.models.Event;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class MockConfirmationGatewayTest {

    @Test
    void processReservation_rejectsMissingEvent() {
        MockConfirmationGateway gateway = new MockConfirmationGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processReservation(null, "user@example.com", "EMAIL", callback(error));

        assertEquals("Invalid event selected", error.get());
    }

    @Test
    void processReservation_defaultsDestinationAndInfersSms() {
        verifyReservationSuccess(sampleEvent(), "   ", "", "SMS", "in-app inbox");
    }

    @Test
    void processReservation_respectsPreferredSms() {
        verifyReservationSuccess(sampleEvent(), "5551112222", "sms", "SMS", "5551112222");
    }

    @Test
    void processReservation_respectsPreferredEmail() {
        verifyReservationSuccess(sampleEvent(), "a@b.com", "EMAIL", "EMAIL", "a@b.com");
    }

    @Test
    void processReservation_inferrsEmailFromDestination() {
        verifyReservationSuccess(sampleEvent(), "notify@example.com", "other", "EMAIL", "notify@example.com");
    }

    @Test
    void processReservation_inferrsSmsFromNonEmailDestination() {
        verifyReservationSuccess(sampleEvent(), "desk kiosk", "other", "SMS", "desk kiosk");
    }

    private void verifyReservationSuccess(Event event,
                                          String destinationInput,
                                          String preferredChannel,
                                          String expectedChannel,
                                          String expectedDestination) {
        MockConfirmationGateway gateway = new MockConfirmationGateway();
        AtomicReference<String> channel = new AtomicReference<>();
        AtomicReference<String> message = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        AtomicLong delay = new AtomicLong(-1L);

        withImmediateMainLooper(() -> gateway.processReservation(event, "  " + destinationInput + "  ", preferredChannel,
                new MockConfirmationGateway.GatewayCallback() {
                    @Override
                    public void onSuccess(String resolvedChannel, String resolvedMessage) {
                        channel.set(resolvedChannel);
                        message.set(resolvedMessage);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        error.set(errorMessage);
                    }
                }), delay);

        assertNull(error.get());
        assertEquals(expectedChannel, channel.get());
        assertTrue(message.get().contains("Mock " + expectedChannel + " confirmation sent to " + expectedDestination));
        assertEquals(700L, delay.get());
    }

    private Event sampleEvent() {
        Event event = new Event();
        event.setId("event-1");
        event.setTitle("Title");
        event.setDate(new com.google.firebase.Timestamp(new Date(System.currentTimeMillis() + 1000L)));
        return event;
    }

    private MockConfirmationGateway.GatewayCallback callback(AtomicReference<String> error) {
        return new MockConfirmationGateway.GatewayCallback() {
            @Override
            public void onSuccess(String channel, String message) {
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

