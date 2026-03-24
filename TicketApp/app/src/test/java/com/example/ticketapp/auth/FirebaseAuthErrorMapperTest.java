package com.example.ticketapp.auth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FirebaseAuthErrorMapperTest {

    @Test
    public void mapCreateError_returnsProviderDisabledMessageForOperationNotAllowedMessage() {
        Exception exception = new Exception("FirebaseAuthException: ERROR_OPERATION_NOT_ALLOWED");

        String message = FirebaseAuthErrorMapper.mapCreateError(exception);

        assertEquals("Account creation is currently unavailable. Enable Email/Password auth in Firebase.", message);
    }

    @Test
    public void mapCreateError_returnsProviderDisabledMessageFromExceptionMessageFallback() {
        Exception exception = new Exception("The given sign-in provider is disabled for this Firebase project.");

        String message = FirebaseAuthErrorMapper.mapCreateError(exception);

        assertEquals("Account creation is currently unavailable. Enable Email/Password auth in Firebase.", message);
    }

    @Test
    public void mapCreateError_returnsConfigurationMessageForConfigurationNotFound() {
        Exception exception = new Exception("CONFIGURATION_NOT_FOUND");

        String message = FirebaseAuthErrorMapper.mapCreateError(exception);

        assertEquals("Account creation is unavailable due to Firebase auth configuration. Check Email/Password setup.", message);
    }

    @Test
    public void mapCreateError_returnsActionableFallbackForUnknownError() {
        Exception exception = new Exception("Something unexpected happened");

        String message = FirebaseAuthErrorMapper.mapCreateError(exception);

        assertEquals("Unable to create account right now. Check internet and Firebase Email/Password setup.", message);
    }

    @Test
    public void mapLoginError_returnsNoAccountForInvalidCredentialCode() {
        Exception exception = new Exception("INVALID_LOGIN_CREDENTIALS");

        String message = FirebaseAuthErrorMapper.mapLoginError(exception);

        assertEquals("No account found for the provided credentials", message);
    }
}



