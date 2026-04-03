package com.example.ticketapp.adapters;

import static org.junit.Assert.assertEquals;

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
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AdminEventAdapterTest {

    @Test
    public void onBindViewHolder_bindsValuesAndDispatchesActions() {
        Event event = sampleEvent();
        AtomicReference<Event> edited = new AtomicReference<>();
        AtomicReference<Event> deleted = new AtomicReference<>();

        AdminEventAdapter adapter = new AdminEventAdapter(Arrays.asList(event), new AdminEventAdapter.EventActionListener() {
            @Override
            public void onEdit(Event event) {
                edited.set(event);
            }

            @Override
            public void onDelete(Event event) {
                deleted.set(event);
            }
        });

        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        AdminEventAdapter.AdminEventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Tech Expo", text(holder.itemView, R.id.adminEventTitle));
        assertEquals(format(event.getDate()), text(holder.itemView, R.id.adminEventDate));
        assertEquals("Palais, Quebec", text(holder.itemView, R.id.adminEventLocation));
        assertEquals("Category: Conference", text(holder.itemView, R.id.adminEventCategory));
        assertEquals("Price: $120.00", text(holder.itemView, R.id.adminEventPrice));
        assertEquals("Seats available: 250", text(holder.itemView, R.id.adminEventSeats));

        holder.itemView.<Button>findViewById(R.id.adminEditEventButton).performClick();
        holder.itemView.<Button>findViewById(R.id.adminDeleteEventButton).performClick();

        assertEquals(event, edited.get());
        assertEquals(event, deleted.get());
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void onBindViewHolder_handlesFallbacksAndNullListener() {
        Event event = sampleEvent();
        event.setDate(null);
        event.setVenue("  ");
        event.setCity(null);
        event.setCategory(" ");

        AdminEventAdapter adapter = new AdminEventAdapter(Arrays.asList(event), null);

        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        AdminEventAdapter.AdminEventViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Date unavailable", text(holder.itemView, R.id.adminEventDate));
        assertEquals("N/A, N/A", text(holder.itemView, R.id.adminEventLocation));
        assertEquals("Category: N/A", text(holder.itemView, R.id.adminEventCategory));

        holder.itemView.<Button>findViewById(R.id.adminEditEventButton).performClick();
        holder.itemView.<Button>findViewById(R.id.adminDeleteEventButton).performClick();

        assertEquals(1, adapter.getItemCount());
    }

    private Event sampleEvent() {
        Event event = new Event();
        event.setId("admin-1");
        event.setTitle("Tech Expo");
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 180_000L)));
        event.setVenue("Palais");
        event.setCity("Quebec");
        event.setCategory("Conference");
        event.setPrice(120.0);
        event.setSeatsAvailable(250);
        return event;
    }

    private String text(android.view.View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    private String format(Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp.toDate());
    }
}



