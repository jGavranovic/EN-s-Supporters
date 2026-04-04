package com.example.ticketapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import org.junit.jupiter.api.Test;

class LoginAuthLogicTest {

    @Test
    void validateLoginInputs_requiresEmail() {
        LoginAuthLogic.ValidationError error = LoginAuthLogic.validateLoginInputs(" ", "password123");

        assertNotNull(error);
        assertEquals(LoginAuthLogic.InputField.LOGIN_EMAIL, error.getField());
        assertEquals("Email is required", error.getMessage());
    }

    @Test
    void validateLoginInputs_requiresValidEmailFormat() {
        LoginAuthLogic.ValidationError error = LoginAuthLogic.validateLoginInputs("invalid", "password123");

        assertNotNull(error);
        assertEquals(LoginAuthLogic.InputField.LOGIN_EMAIL, error.getField());
        assertEquals("Enter a valid email", error.getMessage());
    }

    @Test
    void validateLoginInputs_requiresPassword() {
        LoginAuthLogic.ValidationError error = LoginAuthLogic.validateLoginInputs("user@example.com", " ");

        assertNotNull(error);
        assertEquals(LoginAuthLogic.InputField.LOGIN_PASSWORD, error.getField());
        assertEquals("Password is required", error.getMessage());
    }

    @Test
    void validateLoginInputs_acceptsValidCredentials() {
        LoginAuthLogic.ValidationError error = LoginAuthLogic.validateLoginInputs("user@example.com", "password123");

        assertNull(error);
    }

    @Test
    void validateCreateAccountInputs_rejectsInvalidAdminPasscode() {
        LoginAuthLogic.ValidationError error = LoginAuthLogic.validateCreateAccountInputs(
                "Alex", "alex@example.com", "5551231234", "pass123", true, "WRONG");

        assertNotNull(error);
        assertEquals(LoginAuthLogic.InputField.CREATE_ADMIN_PASSCODE, error.getField());
        assertEquals("Invalid admin passcode", error.getMessage());
    }

    @Test
    void validateCreateAccountInputs_acceptsValidAdminPasscode() {
        LoginAuthLogic.ValidationError error = LoginAuthLogic.validateCreateAccountInputs(
                "Alex", "alex@example.com", "5551231234", "pass123", true, LoginAuthLogic.ADMIN_PASSCODE);

        assertNull(error);
    }

    @Test
    void getFriendlyAuthError_mapsCollisionError() {
        Exception exception = mock(FirebaseAuthUserCollisionException.class);

        String message = LoginAuthLogic.getFriendlyAuthError(exception, true);

        assertEquals("An account with this email already exists", message);
    }

    @Test
    void getFriendlyAuthError_mapsInvalidCredentialsForLogin() {
        Exception exception = mock(FirebaseAuthInvalidCredentialsException.class);

        String message = LoginAuthLogic.getFriendlyAuthError(exception, false);

        assertEquals("Email or password is incorrect", message);
    }

    @Test
    void getFriendlyAuthError_mapsWeakPasswordCode() {
        FirebaseAuthException exception = mock(FirebaseAuthException.class);
        when(exception.getErrorCode()).thenReturn("ERROR_WEAK_PASSWORD");

        String message = LoginAuthLogic.getFriendlyAuthError(exception, true);

        assertEquals("Password must be at least 6 characters", message);
    }

    @Test
    void resolveRole_defaultsToUser() {
        assertEquals("USER", LoginAuthLogic.resolveRole(null));
        assertEquals("USER", LoginAuthLogic.resolveRole("guest"));
    }

    @Test
    void resolveRole_recognizesAdminCaseInsensitive() {
        assertEquals("ADMIN", LoginAuthLogic.resolveRole(" admin "));
    }
}


