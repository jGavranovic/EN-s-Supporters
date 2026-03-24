package com.example.ticketapp.auth;

import java.util.Locale;

final class FirebaseAuthErrorMapper {

    private static final String CREATE_FALLBACK = "Unable to create account right now. Check internet and Firebase Email/Password setup.";
    private static final String LOGIN_FALLBACK = "Unable to login right now";

    private FirebaseAuthErrorMapper() {
    }

    static String mapCreateError(Exception exception) {
        String code = extractCode(exception);
        switch (code) {
            case "email-already-in-use":
                return "An account already exists with this email";
            case "invalid-email":
                return "Please enter a valid email address";
            case "weak-password":
                return "Password must be at least 8 characters";
            case "operation-not-allowed":
                return "Account creation is currently unavailable. Enable Email/Password auth in Firebase.";
            case "configuration-not-found":
                return "Account creation is unavailable due to Firebase auth configuration. Check Email/Password setup.";
            case "network-request-failed":
                return "Network error while creating account. Check your connection and try again.";
            case "too-many-requests":
                return "Too many signup attempts. Please wait a moment and try again.";
            default:
                break;
        }

        String message = lowerMessage(exception);
        if (message.contains("already") || message.contains("in use")) {
            return "An account already exists with this email";
        }
        if (message.contains("email")) {
            return "Please enter a valid email address";
        }
        if (message.contains("password")) {
            return "Password must be at least 8 characters";
        }
        if (message.contains("provider is disabled") || message.contains("operation not allowed") || message.contains("operation_not_allowed")) {
            return "Account creation is currently unavailable. Enable Email/Password auth in Firebase.";
        }
        if (message.contains("configuration not found") || message.contains("configuration_not_found")) {
            return "Account creation is unavailable due to Firebase auth configuration. Check Email/Password setup.";
        }
        if (message.contains("network")) {
            return "Network error while creating account. Check your connection and try again.";
        }
        return CREATE_FALLBACK;
    }

    static String mapLoginError(Exception exception) {
        String code = extractCode(exception);
        switch (code) {
            case "wrong-password":
                return "Incorrect password";
            case "invalid-credential":
            case "user-not-found":
                return "No account found for the provided credentials";
            case "network-request-failed":
                return "Network error while logging in. Check your connection and try again.";
            case "too-many-requests":
                return "Too many login attempts. Please wait a moment and try again.";
            default:
                break;
        }

        String message = lowerMessage(exception);
        if (message.contains("password")) {
            return "Incorrect password";
        }
        if (message.contains("no user") || message.contains("not found") || message.contains("invalid login credentials") || message.contains("invalid_login_credentials")) {
            return "No account found for the provided credentials";
        }
        if (message.contains("network")) {
            return "Network error while logging in. Check your connection and try again.";
        }
        return LOGIN_FALLBACK;
    }

    private static String extractCode(Exception exception) {
        String message = lowerMessage(exception);
        if (message.contains("network")) {
            return "network-request-failed";
        }
        if (message.contains("already") && message.contains("email")) {
            return "email-already-in-use";
        }
        if (message.contains("weak") && message.contains("password")) {
            return "weak-password";
        }
        if (message.contains("invalid") && message.contains("email")) {
            return "invalid-email";
        }
        if (message.contains("wrong") && message.contains("password")) {
            return "wrong-password";
        }
        if (message.contains("not found") || message.contains("invalid credential")) {
            return "user-not-found";
        }
        if (message.contains("too many")) {
            return "too-many-requests";
        }
        if (message.contains("operation not allowed") || message.contains("provider is disabled")) {
            return "operation-not-allowed";
        }
        if (message.contains("configuration not found")) {
            return "configuration-not-found";
        }
        return "";
    }

    private static String lowerMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return "";
        }
        return exception.getMessage().toLowerCase(Locale.ROOT);
    }
}


