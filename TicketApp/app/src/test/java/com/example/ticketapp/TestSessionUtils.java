package com.example.ticketapp;

import java.lang.reflect.Field;

final class TestSessionUtils {

    private TestSessionUtils() {
    }

    static void resetUserSessionSingleton() {
        try {
            Field instance = UserSession.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to reset UserSession singleton", e);
        }
    }
}

