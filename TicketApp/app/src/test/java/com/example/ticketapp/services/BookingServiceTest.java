package com.example.ticketapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;

import com.example.ticketapp.models.Booking;
import com.example.ticketapp.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Date;
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
    void mockPayment_rejectsInvalidCardNumber() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        Event event = eventWithId("event-2");
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Alex", "1234", "12/30", "123");

        AtomicReference<String> error = new AtomicReference<>();
        gateway.processPayment(event, request, new MockPaymentGateway.PaymentCallback() {
            @Override
            public void onSuccess(String paymentMethod, String paymentReference, String message) {
            }

            @Override
            public void onFailure(String errorMessage) {
                error.set(errorMessage);
            }
        });

        assertEquals("Card number must be 13-19 digits", error.get());
    }

    @Test
    void mockPayment_approvesValidVisaRequest() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        Event event = eventWithId("event-3");
        event.setPrice(99.50);

        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "Taylor User", "4242 4242 4242 4242", "12/30", "123");

        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> reference = new AtomicReference<>();
        AtomicReference<String> message = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        try (MockedStatic<Looper> looperMock = org.mockito.Mockito.mockStatic(Looper.class);
             MockedConstruction<Handler> handlerConstruction = org.mockito.Mockito.mockConstruction(Handler.class,
                     (handlerMock, context) -> when(handlerMock.postDelayed(any(Runnable.class), anyLong()))
                             .thenAnswer(invocation -> {
                                 Runnable runnable = invocation.getArgument(0);
                                 runnable.run();
                                 return true;
                             }))) {

            looperMock.when(Looper::getMainLooper).thenReturn(mock(Looper.class));

            gateway.processPayment(event, request, new MockPaymentGateway.PaymentCallback() {
                @Override
                public void onSuccess(String paymentMethod, String paymentReference, String approvedMessage) {
                    method.set(paymentMethod);
                    reference.set(paymentReference);
                    message.set(approvedMessage);
                }

                @Override
                public void onFailure(String errorMessage) {
                    error.set(errorMessage);
                }
            });
        }

        assertNull(error.get());
        assertEquals("VISA", method.get());
        assertNotNull(reference.get());
        assertTrue(reference.get().startsWith("PAY-"));
        assertNotNull(message.get());
        assertTrue(message.get().contains("approved for $99.50"));
    }

    private BookingService newBookingServiceWithMockedFirestore() throws Exception {
        resetBookingServiceSingleton();
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
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
}
