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

import com.example.ticketapp.models.Event;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class MockPaymentGatewayTest {

    @Test
    void paymentRequest_normalizesInputFields() {
        MockPaymentGateway.PaymentRequest request = new MockPaymentGateway.PaymentRequest(
                "  Alex User  ", " 4242 4242 4242 4242 ", " 12/30 ", " 123 ");

        assertEquals("Alex User", request.getCardholderName());
        assertEquals("4242424242424242", request.getCardNumber());
        assertEquals("12/30", request.getExpiry());
        assertEquals("123", request.getCvv());
    }

    @Test
    void processPayment_rejectsNullEvent() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(null, validVisaRequest(), callback(error));

        assertEquals("Invalid event selected", error.get());
    }

    @Test
    void processPayment_rejectsNullRequest() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0), null, callback(error));

        assertEquals("Missing payment details", error.get());
    }

    @Test
    void processPayment_rejectsMissingCardholder() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest(" ", "4242424242424242", "12/30", "123"),
                callback(error));

        assertEquals("Cardholder name is required", error.get());
    }

    @Test
    void processPayment_rejectsTooShortCardNumber() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest("Alex", "123456789012", "12/30", "123"),
                callback(error));

        assertEquals("Card number must be 13-19 digits", error.get());
    }

    @Test
    void processPayment_rejectsTooLongCardNumber() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest("Alex", "12345678901234567890", "12/30", "123"),
                callback(error));

        assertEquals("Card number must be 13-19 digits", error.get());
    }

    @Test
    void processPayment_rejectsNonDigitCardNumber() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest("Alex", "42424242ABCD4242", "12/30", "123"),
                callback(error));

        assertEquals("Card number must contain digits only", error.get());
    }

    @Test
    void processPayment_rejectsInvalidLuhn() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest("Alex", "4242424242424241", "12/30", "123"),
                callback(error));

        assertEquals("Invalid card number", error.get());
    }

    @Test
    void processPayment_rejectsInvalidExpiry() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest("Alex", "4242424242424242", "13/30", "123"),
                callback(error));

        assertEquals("Expiry must be in MM/YY format", error.get());
    }

    @Test
    void processPayment_rejectsInvalidCvv() {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> error = new AtomicReference<>();

        gateway.processPayment(sampleEvent(20.0),
                new MockPaymentGateway.PaymentRequest("Alex", "4242424242424242", "12/30", "12"),
                callback(error));

        assertEquals("CVV must be 3 or 4 digits", error.get());
    }

    @Test
    void processPayment_successForVisa() {
        verifyPaymentSuccess("VISA", "4242424242424242");
    }

    @Test
    void processPayment_successForMastercard() {
        verifyPaymentSuccess("MASTERCARD", "5555555555554444");
    }

    @Test
    void processPayment_successForAmex() {
        verifyPaymentSuccess("AMEX", "378282246310005");
    }

    @Test
    void processPayment_successForUnknownBrand() {
        verifyPaymentSuccess("Credit Card", "6011111111111117");
    }

    private void verifyPaymentSuccess(String expectedMethod, String cardNumber) {
        MockPaymentGateway gateway = new MockPaymentGateway();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> reference = new AtomicReference<>();
        AtomicReference<String> message = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        AtomicLong delay = new AtomicLong(-1L);

        withImmediateMainLooper(() -> gateway.processPayment(
                sampleEvent(99.50),
                new MockPaymentGateway.PaymentRequest("Taylor", cardNumber, "12/30", "123"),
                new MockPaymentGateway.PaymentCallback() {
                    @Override
                    public void onSuccess(String paymentMethod, String paymentReference, String approvalMessage) {
                        method.set(paymentMethod);
                        reference.set(paymentReference);
                        message.set(approvalMessage);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        error.set(errorMessage);
                    }
                }), delay);

        assertNull(error.get());
        assertEquals(expectedMethod, method.get());
        assertNotNull(reference.get());
        assertTrue(reference.get().matches("PAY-[A-Z0-9]{8}"));
        assertNotNull(message.get());
        assertTrue(message.get().contains(expectedMethod + " approved for $99.50"));
        assertEquals(900L, delay.get());
    }

    private MockPaymentGateway.PaymentCallback callback(AtomicReference<String> error) {
        return new MockPaymentGateway.PaymentCallback() {
            @Override
            public void onSuccess(String paymentMethod, String paymentReference, String message) {
            }

            @Override
            public void onFailure(String errorMessage) {
                error.set(errorMessage);
            }
        };
    }

    private Event sampleEvent(double price) {
        Event event = new Event();
        event.setId("event-1");
        event.setTitle("Concert");
        event.setDate(new com.google.firebase.Timestamp(new Date(System.currentTimeMillis() + 1000L)));
        event.setPrice(price);
        return event;
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

    private MockPaymentGateway.PaymentRequest validVisaRequest() {
        return new MockPaymentGateway.PaymentRequest("Alex", "4242 4242 4242 4242", "12/30", "123");
    }
}

