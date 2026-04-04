package com.example.ticketapp.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

public class ModelBeansTest {

    @Test
    public void booking_gettersAndSetters_coverRemainingBeanLines() {
        Booking booking = new Booking();
        Timestamp ts = new Timestamp(new Date(System.currentTimeMillis() + 10_000L));

        booking.setId("b1");
        booking.setEventId("e1");
        booking.setEventTitle("Title");
        booking.setEventDate(ts);
        booking.setVenue("Venue");
        booking.setCity("City");
        booking.setPrice(12.3);
        booking.setUserId("u1");
        booking.setStatus(Booking.STATUS_CONFIRMED);
        booking.setConfirmationCode("C1");
        booking.setConfirmationChannel("EMAIL");
        booking.setContactDestination("x@y.com");
        booking.setPaymentMethod("VISA");
        booking.setPaymentStatus(Booking.PAYMENT_STATUS_PAID);
        booking.setPaymentReference("P1");
        booking.setNotificationStatus(Booking.NOTIFICATION_STATUS_PENDING);
        booking.setNotificationMessage("M");
        booking.setCreatedAt(ts);
        booking.setCancelledAt(ts);
        booking.setNotificationSentAt(ts);

        assertEquals("b1", booking.getId());
        assertEquals("e1", booking.getEventId());
        assertEquals("Title", booking.getEventTitle());
        assertEquals(ts, booking.getEventDate());
        assertEquals("Venue", booking.getVenue());
        assertEquals("City", booking.getCity());
        assertEquals(12.3, booking.getPrice(), 0.0001);
        assertEquals("u1", booking.getUserId());
        assertEquals(Booking.STATUS_CONFIRMED, booking.getStatus());
        assertEquals("C1", booking.getConfirmationCode());
        assertEquals("EMAIL", booking.getConfirmationChannel());
        assertEquals("x@y.com", booking.getContactDestination());
        assertEquals("VISA", booking.getPaymentMethod());
        assertEquals(Booking.PAYMENT_STATUS_PAID, booking.getPaymentStatus());
        assertEquals("P1", booking.getPaymentReference());
        assertEquals(Booking.NOTIFICATION_STATUS_PENDING, booking.getNotificationStatus());
        assertEquals("M", booking.getNotificationMessage());
        assertEquals(ts, booking.getCreatedAt());
        assertEquals(ts, booking.getCancelledAt());
        assertEquals(ts, booking.getNotificationSentAt());
    }

    @Test
    public void userNotification_gettersAndSetters_coverBeanLines() {
        UserNotification notification = new UserNotification();
        Timestamp createdAt = new Timestamp(new Date(System.currentTimeMillis() + 15_000L));

        notification.setId("n1");
        notification.setUserId("u1");
        notification.setEventId("e1");
        notification.setEventTitle("Event");
        notification.setType("EVENT_UPDATED");
        notification.setMessage("Updated");
        notification.setRead(true);
        notification.setCreatedAt(createdAt);

        assertEquals("n1", notification.getId());
        assertEquals("u1", notification.getUserId());
        assertEquals("e1", notification.getEventId());
        assertEquals("Event", notification.getEventTitle());
        assertEquals("EVENT_UPDATED", notification.getType());
        assertEquals("Updated", notification.getMessage());
        assertTrue(notification.isRead());
        assertEquals(createdAt, notification.getCreatedAt());
    }
}

