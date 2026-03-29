package com.example.ticketapp;

import java.util.UUID;

public class UserSession {
    
    public enum UserType {
        GUEST,
        USER,
        ADMIN
    }
    
    private static UserSession instance;
    private UserType currentUserType;
    private String userIdentifier;
    private String contactDestination;
    private String preferredConfirmationChannel;
    
    private UserSession() {
        this.currentUserType = UserType.GUEST;
        this.userIdentifier = buildGuestIdentifier();
        this.contactDestination = "";
        this.preferredConfirmationChannel = "EMAIL";
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

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public String getContactDestination() {
        return contactDestination;
    }

    public void setContactDestination(String contactDestination) {
        this.contactDestination = contactDestination == null ? "" : contactDestination.trim();
    }

    public String getPreferredConfirmationChannel() {
        return preferredConfirmationChannel;
    }

    public void setIdentity(String contactDestination, String channel) {
        String safeContact = contactDestination == null ? "" : contactDestination.trim();
        this.contactDestination = safeContact;

        if (isGuest()) {
            this.userIdentifier = buildGuestIdentifier();
        } else if (!safeContact.isEmpty()) {
            this.userIdentifier = normalizeIdentifier(safeContact);
        }

        setPreferredConfirmationChannel(channel);
    }

    public void setPreferredConfirmationChannel(String channel) {
        if (channel == null) {
            this.preferredConfirmationChannel = "EMAIL";
            return;
        }
        String normalized = channel.trim().toUpperCase();
        if ("SMS".equals(normalized)) {
            this.preferredConfirmationChannel = "SMS";
        } else {
            this.preferredConfirmationChannel = "EMAIL";
        }
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
        this.userIdentifier = buildGuestIdentifier();
        this.contactDestination = "";
        this.preferredConfirmationChannel = "EMAIL";
    }

    private String buildGuestIdentifier() {
        return "guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String normalizeIdentifier(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
