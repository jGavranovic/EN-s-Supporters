package com.example.ticketapp.auth;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthValidatorTest {

    @Test
    public void isValidEmail_acceptsValidEmail() {
        assertTrue(AuthValidator.isValidEmail("user@example.com"));
    }

    @Test
    public void isValidEmail_rejectsInvalidEmail() {
        assertFalse(AuthValidator.isValidEmail("user.example.com"));
    }

    @Test
    public void isValidPhoneNumber_acceptsInternationalAndLocalFormat() {
        assertTrue(AuthValidator.isValidPhoneNumber("+14155552671"));
        assertTrue(AuthValidator.isValidPhoneNumber("0712345678"));
    }

    @Test
    public void isValidPhoneNumber_rejectsShortOrLetteredInput() {
        assertFalse(AuthValidator.isValidPhoneNumber("12345"));
        assertFalse(AuthValidator.isValidPhoneNumber("07123abcde"));
    }

    @Test
    public void isValidPassword_requiresMinimumLength() {
        assertTrue(AuthValidator.isValidPassword("password123"));
        assertFalse(AuthValidator.isValidPassword("short"));
    }
}

