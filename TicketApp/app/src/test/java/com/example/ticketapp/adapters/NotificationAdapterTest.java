package com.example.ticketapp.adapters;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.UserNotification;
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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NotificationAdapterTest {

    @Test
    public void onBindViewHolder_bindsUpdatedTypeAndFormattedDate() {
        UserNotification item = sampleNotification();
        item.setEventTitle("Concert Update");
        item.setMessage("Doors now open at 7:30 PM");
        item.setType("EVENT_UPDATED");
        item.setCreatedAt(new Timestamp(new Date(System.currentTimeMillis() + 50_000L)));

        NotificationAdapter adapter = new NotificationAdapter(Arrays.asList(item));
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Concert Update", text(holder.itemView, R.id.notificationTitle));
        assertEquals("Doors now open at 7:30 PM", text(holder.itemView, R.id.notificationMessage));
        assertEquals("Updated", text(holder.itemView, R.id.notificationType));
        assertEquals(format(item.getCreatedAt()), text(holder.itemView, R.id.notificationCreatedAt));
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void onBindViewHolder_usesFallbackTextAndCancelledType() {
        UserNotification item = sampleNotification();
        item.setEventTitle("   ");
        item.setMessage(null);
        item.setType("event_cancelled");
        item.setCreatedAt(null);

        NotificationAdapter adapter = new NotificationAdapter(Arrays.asList(item));
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        NotificationAdapter.NotificationViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Event update", text(holder.itemView, R.id.notificationTitle));
        assertEquals("No details available", text(holder.itemView, R.id.notificationMessage));
        assertEquals("Cancelled", text(holder.itemView, R.id.notificationType));
        assertEquals("Just now", text(holder.itemView, R.id.notificationCreatedAt));
    }

    @Test
    public void onBindViewHolder_mapsUnknownAndNullTypeToUpdate() {
        UserNotification unknown = sampleNotification();
        unknown.setType("SOMETHING_ELSE");

        UserNotification nullType = sampleNotification();
        nullType.setType(null);

        NotificationAdapter adapter = new NotificationAdapter(Arrays.asList(unknown, nullType));
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());

        NotificationAdapter.NotificationViewHolder holder0 = adapter.onCreateViewHolder(parent, 0);
        NotificationAdapter.NotificationViewHolder holder1 = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder0, 0);
        adapter.onBindViewHolder(holder1, 1);

        assertEquals("Update", text(holder0.itemView, R.id.notificationType));
        assertEquals("Update", text(holder1.itemView, R.id.notificationType));
    }

    private UserNotification sampleNotification() {
        UserNotification item = new UserNotification();
        item.setId("n1");
        item.setEventTitle("Default title");
        item.setMessage("Default message");
        return item;
    }

    private String text(android.view.View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    private String format(Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp.toDate());
    }
}


