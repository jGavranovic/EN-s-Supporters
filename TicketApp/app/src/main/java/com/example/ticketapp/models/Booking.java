package com.example.ticketapp.models;

import com.google.firebase.Timestamp;

public class Booking {

    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String NOTIFICATION_STATUS_PENDING = "PENDING";
    public static final String NOTIFICATION_STATUS_SENT = "SENT";

    private String id;
    private String eventId;
    private String eventTitle;
    private Timestamp eventDate;
    private String venue;
    private String city;
    private double price;
    private String userId;
    private String status;
    private String confirmationCode;
    private String confirmationChannel;
    private String contactDestination;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentReference;
    private String notificationStatus;
    private String notificationMessage;
    private Timestamp createdAt;
    private Timestamp cancelledAt;
    private Timestamp notificationSentAt;

    public Booking() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getConfirmationChannel() {
        return confirmationChannel;
    }

    public void setConfirmationChannel(String confirmationChannel) {
        this.confirmationChannel = confirmationChannel;
    }

    public String getContactDestination() {
        return contactDestination;
    }

    public void setContactDestination(String contactDestination) {
        this.contactDestination = contactDestination;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Timestamp getNotificationSentAt() {
        return notificationSentAt;
    }

    public void setNotificationSentAt(Timestamp notificationSentAt) {
        this.notificationSentAt = notificationSentAt;
    }
}
