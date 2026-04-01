package com.example.ticketapp.system;

import com.example.ticketapp.adapters.EventAdapter;
import com.example.ticketapp.models.Event;
import com.example.ticketapp.EventFilter;
import com.google.firebase.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class EventFilterSystemTest {

    private List<Event> database;
    private List<Event> displayList;
    private String currentQuery;
    private String currentType;

    @BeforeEach
    public void setupDatabase() {
        database = new ArrayList<>(Arrays.asList(
                event("e1", "Coldplay World Tour",  "Music",      "Bell Centre",       "Montreal", 110.00, days(5)),
                event("e2", "Canadiens vs Leafs",   "Sports",     "Centre Bell",      "Montreal",  90.00, days(10)),
                event("e3", "Ballet Spectacle",     "Arts",       "Place des Arts",   "Montreal",  75.00, days(15)),
                event("e4", "Rock Fest 2025",       "Music",      "Parc Jean-Drapeau","Laval",     95.00, days(20)),
                event("e5", "Raptors Game",         "Sports",     "Scotiabank Arena", "Toronto",   80.00, days(7)),
                event("e6", "Stand-Up Gala",        "Comedy",     "Club Soda",        "Montreal",  40.00, days(3)),
                event("e7", "Tech Summit",          "Conference","Palais des congrès", "Montreal", 25.00, days(30)),
                event("e8", "Old Concert",          "Music",      "Past Hall",        "Toronto",   50.00, -days(2))
        ));

        displayList = database.stream()
                .filter(e -> e.getDate().toDate().getTime() >= System.currentTimeMillis())
                .collect(Collectors.toList());

        currentQuery = "";
        currentType  = "All";
    }

    private Event event(String id, String title, String category,
                        String venue, String city, double price, long offsetMs) {
        Event e = new Event(title, new Timestamp(new Date(System.currentTimeMillis() + offsetMs)),
                venue, city, category, price);
        e.setId(id);
        return e;
    }

    private long days(int n) { return (long) n * 86_400_000L; }

    private String formatDate(Event e) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(e.getDate().toDate());
    }

    private List<Event> applyFilter() {
        if (currentQuery.isEmpty()) return new ArrayList<>(displayList);
        return EventFilter.filter(displayList, currentQuery, currentType);
    }

    // end-to-end tests
    @Test
    public void browseFutureEvents() {
        List<Event> visible = applyFilter();
        assertTrue(visible.stream().allMatch(e -> e.getDate().toDate().getTime() >= System.currentTimeMillis()));
        assertFalse(visible.stream().anyMatch(e -> e.getTitle().equals("Old Concert")));
    }

    @Test
    public void filterByCategoryAndLocation() {
        currentQuery = "Music"; currentType = "Category";
        List<Event> musicEvents = applyFilter();
        assertTrue(musicEvents.stream().allMatch(e -> e.getCategory().equalsIgnoreCase("Music")));

        currentQuery = "Montreal"; currentType = "Location";
        List<Event> montrealEvents = applyFilter();
        assertTrue(montrealEvents.stream().allMatch(
                e -> e.getCity().equalsIgnoreCase("Montreal") || e.getVenue().toLowerCase().contains("montreal")));
    }

    @Test
    public void freeTextSearchScenario() {
        currentQuery = "Bell"; currentType = "All";
        List<Event> bellEvents = applyFilter();
        assertFalse(bellEvents.isEmpty());
        assertTrue(bellEvents.stream().anyMatch(e -> e.getVenue().toLowerCase().contains("bell")));

        currentQuery = "Sports"; currentType = "Category";
        List<Event> sportsEvents = applyFilter();
        assertTrue(sportsEvents.stream().allMatch(e -> e.getCategory().equalsIgnoreCase("Sports")));
    }

    @Test
    public void dateFilterScenario() {
        Event target = displayList.stream().filter(e -> e.getTitle().equals("Coldplay World Tour")).findFirst().orElseThrow();
        currentQuery = formatDate(target);
        currentType = "Date";

        List<Event> result = applyFilter();
        assertTrue(result.stream().anyMatch(e -> e.getTitle().equals("Coldplay World Tour")));
    }

    @Test
    public void sequentialBrowsingFilters() {
        assertEquals(displayList.size(), applyFilter().size());

        currentQuery = "Music"; currentType = "Category";
        List<Event> musicOnly = applyFilter();
        assertEquals(2, musicOnly.size());

        currentQuery = ""; currentType = "All";
        assertEquals(displayList.size(), applyFilter().size());

        currentQuery = "Montreal"; currentType = "Location";
        List<Event> montrealOnly = applyFilter();
        assertFalse(montrealOnly.isEmpty());
        assertTrue(montrealOnly.stream().allMatch(
                e -> e.getCity().equalsIgnoreCase("Montreal") || e.getVenue().toLowerCase().contains("montreal")));
    }

    @Test
    public void addNewEventAppears() {
        int beforeCount = displayList.size();
        Event newEvent = event("e9", "Jazz Brunch", "Music", "Upstairs Jazz Bar", "Montreal", 30.0, days(12));
        displayList.add(newEvent);

        currentQuery = ""; currentType = "All";
        List<Event> after = applyFilter();
        assertEquals(beforeCount + 1, after.size());
        assertTrue(after.stream().anyMatch(e -> e.getTitle().equals("Jazz Brunch")));
    }

    @Test
    public void removeEventDisappears() {
        int beforeCount = displayList.size();
        displayList.removeIf(e -> e.getId().equals("e6"));

        currentQuery = ""; currentType = "All";
        List<Event> after = applyFilter();
        assertEquals(beforeCount - 1, after.size());
        assertFalse(after.stream().anyMatch(e -> e.getId().equals("e6")));
    }

    @Test
    public void emptySearchShowsEmptyAdapter() {
        currentQuery = "nomatch"; currentType = "All";
        List<Event> result = applyFilter();
        EventAdapter adapter = new EventAdapter(result, null);
        assertEquals(0, adapter.getItemCount());
        assertTrue(result.isEmpty());
    }
}