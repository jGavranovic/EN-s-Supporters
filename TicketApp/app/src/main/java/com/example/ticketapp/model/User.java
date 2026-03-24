package com.example.ticketapp.model;

import com.example.ticketapp.UserSession;

@SuppressWarnings("unused")
public class User {

    private final String id;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final UserSession.UserType userType;

    public User(
            String id,
            String fullName,
            String email,
            String phoneNumber,
            UserSession.UserType userType
    ) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
    }

    public String getId() {
        return id;
    }


    public UserSession.UserType getUserType() {
        return userType;
    }
}


