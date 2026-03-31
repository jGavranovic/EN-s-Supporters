package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    public interface EventActionListener {
        void onEdit(Event event);

        void onDelete(Event event);
    }

    private final List<Event> events;
    private final EventActionListener actionListener;

    public AdminEventAdapter(List<Event> events, EventActionListener actionListener) {
        this.events = events;
        this.actionListener = actionListener;
    }

    @Override
    public AdminEventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AdminEventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.title.setText(event.getTitle());

        if (event.getDate() != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.date.setText(format.format(event.getDate().toDate()));
        } else {
            holder.date.setText("Date unavailable");
        }

        holder.location.setText(safeText(event.getVenue()) + ", " + safeText(event.getCity()));
        holder.category.setText("Category: " + safeText(event.getCategory()));
        holder.price.setText(String.format(Locale.getDefault(), "Price: $%.2f", event.getPrice()));
        holder.seats.setText("Seats available: " + event.getSeatsAvailable());

        holder.editButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(event);
            }
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class AdminEventViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView date;
        TextView location;
        TextView category;
        TextView price;
        TextView seats;
        Button editButton;
        Button deleteButton;

        AdminEventViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.adminEventTitle);
            date = itemView.findViewById(R.id.adminEventDate);
            location = itemView.findViewById(R.id.adminEventLocation);
            category = itemView.findViewById(R.id.adminEventCategory);
            price = itemView.findViewById(R.id.adminEventPrice);
            seats = itemView.findViewById(R.id.adminEventSeats);
            editButton = itemView.findViewById(R.id.adminEditEventButton);
            deleteButton = itemView.findViewById(R.id.adminDeleteEventButton);
        }
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }
}
