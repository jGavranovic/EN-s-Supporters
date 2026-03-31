package com.example.ticketapp;

import com.example.ticketapp.models.Event;
import com.google.firebase.Timestamp;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EventFilterTest {

    private Event createEvent(String title, String category, String venue, String city, Double price, long timeOffset) {
        return new Event(
                title,
                new Timestamp(new Date(System.currentTimeMillis() + timeOffset)),
                venue,
                city,
                category,
                price
        );
    }

    @Test
    public void testFilter_ByCategory() {
        List<Event> events = Arrays.asList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000),
                createEvent("Game B", "Sports", "Stadium", "Toronto", 75.0, 100000)
        );

        List<Event> result = EventFilter.filter(events, "music", "Category");

        assertEquals(1, result.size());
        assertEquals("Concert A", result.get(0).getTitle());
    }

    @Test
    public void testFilter_ByLocation() {
        List<Event> events = Arrays.asList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000),
                createEvent("Game B", "Sports", "Stadium", "Toronto", 75.0, 100000)
        );

        List<Event> result = EventFilter.filter(events, "bell", "Location");
        assertEquals(1, result.size());
        assertEquals("Bell Centre", result.get(0).getVenue());

        result = EventFilter.filter(events, "toronto", "Location");
        assertEquals(1, result.size());
        assertEquals("Toronto", result.get(0).getCity());
    }

    @Test
    public void testFilter_ByDateType() {
        List<Event> events = Collections.singletonList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000)
        );

        String dateString = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(events.get(0).getDate().toDate());

        List<Event> result = EventFilter.filter(events, dateString, "Date");
        assertEquals(1, result.size());
    }

    @Test
    public void testFilter_AllFields() {
        List<Event> events = Collections.singletonList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000)
        );

        List<Event> result = EventFilter.filter(events, "concert", "All");
        assertEquals(1, result.size());

        result = EventFilter.filter(events, "music", "All");
        assertEquals(1, result.size());

        result = EventFilter.filter(events, "bell", "All");
        assertEquals(1, result.size());

        result = EventFilter.filter(events, "montreal", "All"); 
        assertEquals(1, result.size());

        String dateString = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(events.get(0).getDate().toDate());
        result = EventFilter.filter(events, dateString, "All");
        assertEquals(1, result.size());

        result = EventFilter.filter(events, "50.0", "All");
        assertEquals(1, result.size());
    }

    @Test
    public void testFilter_NoMatch() {
        List<Event> events = Collections.singletonList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000)
        );

        List<Event> result = EventFilter.filter(events, "sports", "Category");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_IgnorePastEvents() {
        List<Event> events = Collections.singletonList(
                createEvent("Past Event", "Music", "Old Venue", "Montreal", 50.0, -100000)
        );

        List<Event> result = EventFilter.filter(events, "music", "Category");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_NullQuery() {
        List<Event> events = Collections.singletonList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000)
        );

        List<Event> result = EventFilter.filter(events, null, "All");
        assertEquals(1, result.size());
    }

    @Test
    public void testFilter_EmptyList() {
        List<Event> result = EventFilter.filter(new ArrayList<>(), "anything", "All");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_InvalidType() {
        List<Event> events = Collections.singletonList(
                createEvent("Concert A", "Music", "Bell Centre", "Montreal", 50.0, 100000)
        );

        List<Event> result = EventFilter.filter(events, "concert", "UnknownType");
        assertTrue(result.isEmpty());
    }
}