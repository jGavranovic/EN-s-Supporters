package com.example.ticketapp;

import com.example.ticketapp.model.User;

@SuppressWarnings("unused")
public class UserSession {
    
    public enum UserType {
        GUEST,
        USER,
        ADMIN
    }
    
    private static UserSession instance;
    private UserType currentUserType;
    private User currentUser;
    private String jwtToken;
    
    private UserSession() {
        this.currentUserType = UserType.GUEST;
    }
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    public UserType getCurrentUserType() {
        return currentUserType;
    }
    public void setUserType(UserType userType) {
        this.currentUserType = userType;
    }

    public void setAuthenticatedUser(User user, String token) {
        this.currentUser = user;
        this.jwtToken = token;
        if (user != null) {
            this.currentUserType = user.getUserType();
        }
    }

    public boolean isGuest() {
        return currentUserType == UserType.GUEST;
    }
    public void logout() {
        this.currentUserType = UserType.GUEST;
        this.currentUser = null;
        this.jwtToken = null;
    }
}
