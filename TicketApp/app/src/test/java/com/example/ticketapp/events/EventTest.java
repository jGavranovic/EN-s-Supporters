package com.example.ticketapp.models;

import com.google.firebase.Timestamp;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class EventTest {

    @Test
    public void testAllGettersAndSetters() {
        Event event = new Event();

        Timestamp timestamp = new Timestamp(new Date());

        event.setId("1");
        event.setTitle("Concert");
        event.setDate(timestamp);
        event.setVenue("Bell Centre");
        event.setCity("Montreal");
        event.setCategory("Music");
        event.setPrice(99.99);

        assertEquals("1", event.getId());
        assertEquals("Concert", event.getTitle());
        assertEquals(timestamp, event.getDate());
        assertEquals("Bell Centre", event.getVenue());
        assertEquals("Montreal", event.getCity());
        assertEquals("Music", event.getCategory());
        assertEquals(99.99, event.getPrice());
    }

    @Test
    public void testConstructor() {
        Timestamp timestamp = new Timestamp(new Date());

        Event event = new Event(
                "Concert",
                timestamp,
                "Bell Centre",
                "Montreal",
                "Music",
                99.99
        );

        assertEquals("Concert", event.getTitle());
        assertEquals(timestamp, event.getDate());
        assertEquals("Bell Centre", event.getVenue());
        assertEquals("Montreal", event.getCity());
        assertEquals("Music", event.getCategory());
        assertEquals(99.99, event.getPrice());
    }

    @Test
    public void testEmptyConstructor() {
        Event event = new Event();
        assertNotNull(event);
    }
}