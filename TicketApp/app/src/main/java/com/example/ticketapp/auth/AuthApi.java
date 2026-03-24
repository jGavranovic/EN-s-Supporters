package com.example.ticketapp.auth;

public interface AuthApi {
    void register(String fullName, String email, String phoneNumber, String password, AuthCallback callback);

    void login(String emailOrPhone, String password, AuthCallback callback);

    void logout();
}


