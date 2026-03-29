package com.example.ticketapp;

import com.example.ticketapp.models.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventFilter {

    public static List<Event> filter(List<Event> events, String query, String type) {
        List<Event> result = new ArrayList<>();

        if (query == null) query = "";
        query = query.toLowerCase();

        long nowMillis = System.currentTimeMillis();

        for (Event event : events) {
            if (event.getDate().toDate().getTime() < nowMillis) continue;

            boolean match = false;

            if (type.equals("All")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String dateString = sdf.format(event.getDate().toDate());

                match = event.getTitle().toLowerCase().contains(query)
                        || event.getCategory().toLowerCase().contains(query)
                        || event.getVenue().toLowerCase().contains(query)
                        || event.getCity().toLowerCase().contains(query)
                        || dateString.toLowerCase().contains(query)
                        || String.valueOf(event.getPrice()).contains(query);
            } else {
                switch (type) {
                    case "Category":
                        match = event.getCategory().toLowerCase().contains(query);
                        break;
                    case "Date":
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        String dateString = sdf.format(event.getDate().toDate());
                        match = dateString.toLowerCase().contains(query);
                        break;
                    case "Location":
                        match = event.getVenue().toLowerCase().contains(query)
                                || event.getCity().toLowerCase().contains(query);
                        break;
                }
            }

            if (match) result.add(event);
        }

        return result;
    }
}