package com.example.ticketapp.unit;

import com.example.ticketapp.models.Event;
import com.example.ticketapp.EventFilter;
import com.google.firebase.Timestamp;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EventFilterUnitTest {

    private Event createEvent(String title, String category, String venue,
                              String city, double price, long offsetMs) {
        Event e = new Event(title,
                new Timestamp(new Date(System.currentTimeMillis() + offsetMs)),
                venue, city, category, price);
        e.setId(UUID.randomUUID().toString());
        return e;
    }

    private String formatDate(Event e) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(e.getDate().toDate());
    }
    @Test
    public void eventModel_constructorsAndSetters() {
        Event e = new Event();
        e.setId("id");
        e.setTitle("Concert");
        e.setCategory("Music");
        e.setVenue("Bell Centre");
        e.setCity("Montreal");
        e.setPrice(50.0);
        e.setDate(new Timestamp(new Date()));
        e.setSeatsAvailable(100);

        assertEquals("id", e.getId());
        assertEquals("Concert", e.getTitle());
        assertEquals("Music", e.getCategory());
        assertEquals("Bell Centre", e.getVenue());
        assertEquals("Montreal", e.getCity());
        assertEquals(50.0, e.getPrice(), 0.001);
        assertEquals(100, e.getSeatsAvailable());
    }

    @Test
    public void eventModel_seatsDefaultZero() {
        Event e = new Event("x", new Timestamp(new Date()), "v", "c", "Music", 10.0);
        assertEquals(0, e.getSeatsAvailable());
    }

    // EventFilter logic tests
    @Test
    public void filterByCategory_caseInsensitive() {
        List<Event> events = Arrays.asList(
                createEvent("A", "Music", "V1", "C1", 10.0, 100_000),
                createEvent("B", "Sports", "V2", "C2", 20.0, 100_000)
        );
        List<Event> result = EventFilter.filter(events, "music", "Category");
        assertEquals(1, result.size());
    }

    @Test
    public void filterByLocation_venueMatch() {
        List<Event> events = Collections.singletonList(
                createEvent("A", "Music", "Bell Venue", "C1", 10.0, 100_000)
        );
        List<Event> result = EventFilter.filter(events, "bell", "Location");
        assertEquals(1, result.size());
    }

    @Test
    public void filterByDate_match() {
        Event e = createEvent("A", "Music", "V", "C", 10.0, 100_000);
        String dateStr = formatDate(e);
        List<Event> result = EventFilter.filter(Collections.singletonList(e), dateStr, "Date");
        assertEquals(1, result.size());
    }

    @Test
    public void filterAll_noMatchReturnsEmpty() {
        Event e = createEvent("A", "Music", "V", "C", 10.0, 100_000);
        List<Event> result = EventFilter.filter(Collections.singletonList(e), "zzz", "All");
        assertTrue(result.isEmpty());
    }

    @Test
    public void filterEmptyListReturnsEmpty() {
        List<Event> result = EventFilter.filter(Collections.emptyList(), "music", "All");
        assertTrue(result.isEmpty());
    }
}