package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.UserNotification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<UserNotification> notifications;

    public NotificationAdapter(List<UserNotification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        UserNotification item = notifications.get(position);

        holder.title.setText(safeText(item.getEventTitle(), "Event update"));
        holder.message.setText(safeText(item.getMessage(), "No details available"));
        holder.type.setText(readableType(item.getType()));

        if (item.getCreatedAt() != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.createdAt.setText(format.format(item.getCreatedAt().toDate()));
        } else {
            holder.createdAt.setText("Just now");
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView message;
        TextView type;
        TextView createdAt;

        NotificationViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            type = itemView.findViewById(R.id.notificationType);
            createdAt = itemView.findViewById(R.id.notificationCreatedAt);
        }
    }

    private String readableType(String rawType) {
        if (rawType == null) {
            return "Update";
        }
        if ("EVENT_CANCELLED".equalsIgnoreCase(rawType)) {
            return "Cancelled";
        }
        if ("EVENT_UPDATED".equalsIgnoreCase(rawType)) {
            return "Updated";
        }
        return "Update";
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}
