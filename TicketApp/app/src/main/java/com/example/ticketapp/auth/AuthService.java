package com.example.ticketapp.auth;

public final class AuthService {

    private static final AuthApi AUTH_API = new FirebaseAuthApi();

    private AuthService() {
    }

    public static AuthApi getAuthApi() {
        return AUTH_API;
    }
}


