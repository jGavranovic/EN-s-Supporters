package com.example.ticketapp.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.Event;
import com.google.firebase.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EventAdapterTest {

    @Test
    public void onBindViewHolder_defaultStateEnablesReserveAndHandlesClicks() {
        Event event = sampleEvent("event-1");
        AtomicReference<Event> reserved = new AtomicReference<>();
        AtomicInteger clickCount = new AtomicInteger();

        EventAdapter adapter = new EventAdapter(Arrays.asList(event), e -> {
            reserved.set(e);
            clickCount.incrementAndGet();
        });

        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Jazz Night", text(holder.itemView, R.id.eventTitle));
        assertEquals("Music", text(holder.itemView, R.id.eventCategory));
        assertEquals("Place Bell, Laval", text(holder.itemView, R.id.eventLocation));
        assertEquals("$39.99", text(holder.itemView, R.id.eventPrice));
        assertEquals(format(event.getDate()), text(holder.itemView, R.id.eventDate));

        Button reserveButton = holder.itemView.findViewById(R.id.reserveButton);
        assertTrue(reserveButton.isEnabled());
        assertEquals("Reserve", reserveButton.getText().toString());

        reserveButton.performClick();
        holder.itemView.performClick();

        assertEquals(event, reserved.get());
        assertEquals(2, clickCount.get());
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void onBindViewHolder_reservingStateDisablesButtonAndBlocksClicks() {
        Event event = sampleEvent("event-2");
        AtomicReference<Event> reserved = new AtomicReference<>();

        EventAdapter adapter = new EventAdapter(Arrays.asList(event), reserved::set);
        Set<String> reservingIds = new HashSet<>(Collections.singletonList("event-2"));
        adapter.setReservationState(reservingIds, Collections.emptySet());

        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        Button reserveButton = holder.itemView.findViewById(R.id.reserveButton);
        assertFalse(reserveButton.isEnabled());
        assertEquals("Reserving...", reserveButton.getText().toString());

        reserveButton.performClick();
        holder.itemView.performClick();
        assertNull(reserved.get());
    }

    @Test
    public void onBindViewHolder_reservedStateDisablesButtonAndBlocksClicks() {
        Event event = sampleEvent("event-3");
        AtomicReference<Event> reserved = new AtomicReference<>();

        EventAdapter adapter = new EventAdapter(Arrays.asList(event), reserved::set);
        adapter.setReservationState(Collections.emptySet(), new HashSet<>(Collections.singletonList("event-3")));

        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        Button reserveButton = holder.itemView.findViewById(R.id.reserveButton);
        assertFalse(reserveButton.isEnabled());
        assertEquals("Reserved", reserveButton.getText().toString());

        reserveButton.performClick();
        holder.itemView.performClick();
        assertNull(reserved.get());
    }

    @Test
    public void onBindViewHolder_nullEventIdWithStateSetsStillAllowsReserve() {
        Event event = sampleEvent(null);
        AtomicReference<Event> reserved = new AtomicReference<>();

        EventAdapter adapter = new EventAdapter(Arrays.asList(event), reserved::set);
        adapter.setReservationState(new HashSet<>(Collections.singletonList("event-4")), new HashSet<>(Collections.singletonList("event-4")));

        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        Button reserveButton = holder.itemView.findViewById(R.id.reserveButton);
        assertTrue(reserveButton.isEnabled());
        assertEquals("Reserve", reserveButton.getText().toString());

        reserveButton.performClick();
        assertEquals(event, reserved.get());
    }

    @Test
    public void onBindViewHolder_nullListenerDoesNotCrash() {
        EventAdapter adapter = new EventAdapter(Arrays.asList(sampleEvent("event-5")), null);
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        EventAdapter.EventViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        holder.itemView.<Button>findViewById(R.id.reserveButton).performClick();
        holder.itemView.performClick();

        assertTrue(true);
    }

    private Event sampleEvent(String id) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Jazz Night");
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 90_000L)));
        event.setVenue("Place Bell");
        event.setCity("Laval");
        event.setCategory("Music");
        event.setPrice(39.99);
        return event;
    }

    private String text(android.view.View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    private String format(Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp.toDate());
    }
}


