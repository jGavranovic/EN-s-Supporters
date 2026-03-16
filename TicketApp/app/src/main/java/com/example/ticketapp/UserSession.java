package com.example.ticketapp;

public class UserSession {
    
    public enum UserType {
        GUEST,
        USER,
        ADMIN
    }
    
    private static UserSession instance;
    private UserType currentUserType;
    
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
    public boolean isGuest() {
        return currentUserType == UserType.GUEST;
    }
    public boolean isUser() {
        return currentUserType == UserType.USER;
    }
    public boolean isAdmin() {
        return currentUserType == UserType.ADMIN;
    }
    public void logout() {
        this.currentUserType = UserType.GUEST;
    }
}
