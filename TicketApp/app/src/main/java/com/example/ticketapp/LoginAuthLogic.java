package com.example.ticketapp;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.util.regex.Pattern;

public final class LoginAuthLogic {

    public static final String ADMIN_PASSCODE = "ADMIN2026";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private LoginAuthLogic() {
    }

    public enum InputField {
        LOGIN_EMAIL,
        LOGIN_PASSWORD,
        CREATE_NAME,
        CREATE_EMAIL,
        CREATE_PHONE,
        CREATE_PASSWORD,
        CREATE_ADMIN_PASSCODE
    }

    public static final class ValidationError {
        private final InputField field;
        private final String message;

        ValidationError(InputField field, String message) {
            this.field = field;
            this.message = message;
        }

        public InputField getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }

    public static ValidationError validateLoginInputs(String email, String password) {
        if (isBlank(email)) {
            return new ValidationError(InputField.LOGIN_EMAIL, "Email is required");
        }
        if (!isValidEmail(email)) {
            return new ValidationError(InputField.LOGIN_EMAIL, "Enter a valid email");
        }
        if (isBlank(password)) {
            return new ValidationError(InputField.LOGIN_PASSWORD, "Password is required");
        }
        return null;
    }

    public static ValidationError validateCreateAccountInputs(
            String name,
            String email,
            String phoneNumber,
            String password,
            boolean createAsAdmin,
            String adminPasscode
    ) {
        if (isBlank(name)) {
            return new ValidationError(InputField.CREATE_NAME, "Name is required");
        }
        if (isBlank(email)) {
            return new ValidationError(InputField.CREATE_EMAIL, "Email is required");
        }
        if (!isValidEmail(email)) {
            return new ValidationError(InputField.CREATE_EMAIL, "Enter a valid email");
        }
        if (isBlank(phoneNumber)) {
            return new ValidationError(InputField.CREATE_PHONE, "Phone number is required");
        }
        if (isBlank(password)) {
            return new ValidationError(InputField.CREATE_PASSWORD, "Password is required");
        }
        if (createAsAdmin && !ADMIN_PASSCODE.equals(trimToEmpty(adminPasscode))) {
            return new ValidationError(InputField.CREATE_ADMIN_PASSCODE, "Invalid admin passcode");
        }
        return null;
    }

    public static String resolveRole(String storedRole) {
        return "ADMIN".equalsIgnoreCase(trimToEmpty(storedRole)) ? "ADMIN" : "USER";
    }

    public static String getFriendlyAuthError(Exception exception, boolean creatingAccount) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email already exists";
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return creatingAccount ? "Please use a valid email and password" : "Email or password is incorrect";
        }
        if (exception instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return "Password must be at least 6 characters";
            }
            if ("ERROR_USER_NOT_FOUND".equals(code)) {
                return "No account found for this email";
            }
            if ("ERROR_WRONG_PASSWORD".equals(code)) {
                return "Email or password is incorrect";
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Please enter a valid email";
            }
        }
        return creatingAccount ? "Could not create account right now" : "Could not log in right now";
    }

    private static boolean isValidEmail(String value) {
        return EMAIL_PATTERN.matcher(trimToEmpty(value)).matches();
    }

    private static boolean isBlank(String value) {
        return trimToEmpty(value).isEmpty();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

