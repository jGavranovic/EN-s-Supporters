package com.example.ticketapp.component;

import com.example.ticketapp.adapters.EventAdapter;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.EventFilter;
import com.google.firebase.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EventFilterComponentTest {

    private List<Event> allEvents;
    private List<Event> displayList;

    @BeforeEach
    public void setup() {
        allEvents = Arrays.asList(
                new Event("Concert", new Timestamp(new Date(System.currentTimeMillis() + 100_000)), "Bell Centre", "Montreal", "Music", 50.0),
                new Event("Game",    new Timestamp(new Date(System.currentTimeMillis() + 200_000)), "Stadium",    "Toronto", "Sports", 75.0),
                new Event("Old",     new Timestamp(new Date(System.currentTimeMillis() - 100_000)), "Old Hall",  "Montreal", "Music", 20.0)
        );
        allEvents.forEach(e -> e.setId(UUID.randomUUID().toString()));

        displayList = allEvents.stream()
                .filter(e -> e.getDate().toDate().getTime() >= System.currentTimeMillis())
                .collect(Collectors.toList());
    }

    @Test
    public void adapterReflectsFilteredList() {
        List<Event> filtered = EventFilter.filter(displayList, "music", "Category");
        EventAdapter adapter = new EventAdapter(filtered, null);
        assertEquals(filtered.size(), adapter.getItemCount());
    }

    @Test
    public void sequentialFiltering_categoryThenLocation() {
        List<Event> step1 = EventFilter.filter(displayList, "music", "Category");
        assertTrue(step1.stream().allMatch(e -> e.getCategory().equalsIgnoreCase("Music")));

        List<Event> step2 = EventFilter.filter(displayList, "montreal", "Location");
        assertTrue(step2.stream().allMatch(e -> e.getCity().toLowerCase().contains("montreal")
                || e.getVenue().toLowerCase().contains("montreal")));
    }

    @Test
    public void clearingFilterRestoresDisplayList() {
        EventFilter.filter(displayList, "music", "Category");
        List<Event> restored = EventFilter.filter(displayList, "", "All");
        assertEquals(displayList.size(), restored.size());
    }
}