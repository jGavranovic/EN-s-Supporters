package com.example.ticketapp.services;

import android.os.Handler;
import android.os.Looper;

import com.example.ticketapp.models.Event;

import java.util.Locale;
import java.util.UUID;

public class MockPaymentGateway {

    public static class PaymentRequest {
        private final String cardholderName;
        private final String cardNumber;
        private final String expiry;
        private final String cvv;

        public PaymentRequest(String cardholderName, String cardNumber, String expiry, String cvv) {
            this.cardholderName = cardholderName == null ? "" : cardholderName.trim();
            this.cardNumber = cardNumber == null ? "" : cardNumber.replace(" ", "").trim();
            this.expiry = expiry == null ? "" : expiry.trim();
            this.cvv = cvv == null ? "" : cvv.trim();
        }

        public String getCardholderName() {
            return cardholderName;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public String getExpiry() {
            return expiry;
        }

        public String getCvv() {
            return cvv;
        }
    }

    public interface PaymentCallback {
        void onSuccess(String paymentMethod, String paymentReference, String message);

        void onFailure(String errorMessage);
    }

    public void processPayment(Event event, PaymentRequest request, PaymentCallback callback) {
        if (event == null) {
            callback.onFailure("Invalid event selected");
            return;
        }
        if (request == null) {
            callback.onFailure("Missing payment details");
            return;
        }

        String validationError = validateRequest(request);
        if (validationError != null) {
            callback.onFailure(validationError);
            return;
        }

        String normalizedPaymentMethod = resolveCardBrand(request.getCardNumber());
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            String reference = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.US);
            String message = normalizedPaymentMethod + " approved for $" + String.format(Locale.getDefault(), "%.2f", event.getPrice());
            callback.onSuccess(normalizedPaymentMethod, reference, message);
        }, 900);
    }

    private String validateRequest(PaymentRequest request) {
        if (request.getCardholderName().isEmpty()) {
            return "Cardholder name is required";
        }
        if (request.getCardNumber().length() < 13 || request.getCardNumber().length() > 19) {
            return "Card number must be 13-19 digits";
        }
        if (!request.getCardNumber().matches("\\d+")) {
            return "Card number must contain digits only";
        }
        if (!isValidLuhn(request.getCardNumber())) {
            return "Invalid card number";
        }
        if (!request.getExpiry().matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            return "Expiry must be in MM/YY format";
        }
        if (!request.getCvv().matches("[0-9]{3,4}")) {
            return "CVV must be 3 or 4 digits";
        }
        return null;
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleDigit) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String resolveCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        }
        if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        }
        if (cardNumber.startsWith("3")) {
            return "AMEX";
        }
        return "Credit Card";
    }
}
