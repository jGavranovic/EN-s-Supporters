package com.example.ticketapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserSessionTest {

    @AfterEach
    void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    void getInstance_defaultsToGuestSession() {
        UserSession session = UserSession.getInstance();

        assertTrue(session.isGuest());
        assertTrue(session.getUserIdentifier().startsWith("guest_"));
        assertEquals("", session.getContactDestination());
        assertEquals("EMAIL", session.getPreferredConfirmationChannel());
    }

    @Test
    void setIdentity_normalizesUserIdentifierAndChannel() {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);

        session.setIdentity(" User.Name+One@Example.com ", "sms");

        assertEquals("user_name_one_example_com", session.getUserIdentifier());
        assertEquals("User.Name+One@Example.com", session.getContactDestination());
        assertEquals("SMS", session.getPreferredConfirmationChannel());
    }

    @Test
    void logout_resetsToGuestDefaults() {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.ADMIN);
        session.setIdentity("admin@example.com", "SMS");

        session.logout();

        assertTrue(session.isGuest());
        assertTrue(session.getUserIdentifier().startsWith("guest_"));
        assertEquals("", session.getContactDestination());
        assertEquals("EMAIL", session.getPreferredConfirmationChannel());
    }
}

