package com.example.ticketapp.auth;

import com.example.ticketapp.model.User;

public class AuthResponse {

    private final boolean success;
    private final String message;
    private final User user;
    private final String jwtToken;

    private AuthResponse(boolean success, String message, User user, String jwtToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.jwtToken = jwtToken;
    }

    public static AuthResponse success(String message, User user, String jwtToken) {
        return new AuthResponse(true, message, user, jwtToken);
    }

    public static AuthResponse failure(String message) {
        return new AuthResponse(false, message, null, null);
    }

    public boolean succeeded() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public String getJwtToken() {
        return jwtToken;
    }
}

