package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticketapp.R;
import com.example.ticketapp.models.Event;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    public interface OnReserveClickListener {
        void onReserveClick(Event event);
    }

    private List<Event> events;
    private final OnReserveClickListener reserveClickListener;
    private Set<String> reservingEventIds;
    private Set<String> reservedEventIds;

    public EventAdapter(List<Event> events, OnReserveClickListener reserveClickListener) {
        this.events = events;
        this.reserveClickListener = reserveClickListener;
    }

    public void setReservationState(Set<String> reservingEventIds, Set<String> reservedEventIds) {
        this.reservingEventIds = reservingEventIds;
        this.reservedEventIds = reservedEventIds;
    }

    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.title.setText(event.getTitle());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(event.getDate().toDate());
        holder.date.setText(formattedDate);
        holder.location.setText(event.getVenue() + ", " + event.getCity());
        holder.category.setText(event.getCategory());
        holder.price.setText(String.format("$%.2f", event.getPrice()));

        boolean isReserving = event.getId() != null
                && reservingEventIds != null
                && reservingEventIds.contains(event.getId());
        boolean isReserved = event.getId() != null
                && reservedEventIds != null
                && reservedEventIds.contains(event.getId());

        if (isReserving) {
            holder.reserveButton.setEnabled(false);
            holder.reserveButton.setText("Reserving...");
        } else if (isReserved) {
            holder.reserveButton.setEnabled(false);
            holder.reserveButton.setText("Reserved");
        } else {
            holder.reserveButton.setEnabled(true);
            holder.reserveButton.setText("Reserve");
        }

        View.OnClickListener reserveAction = v -> {
            if (reserveClickListener != null && !isReserving && !isReserved) {
                reserveClickListener.onReserveClick(event);
            }
        };

        holder.reserveButton.setOnClickListener(reserveAction);
        holder.itemView.setOnClickListener(reserveAction);
    }

    @Override
    public int getItemCount() {return events.size(); }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, location, category, price;
        Button reserveButton;
        EventViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.eventTitle);
            date = itemView.findViewById(R.id.eventDate);
            location = itemView.findViewById(R.id.eventLocation);
            category = itemView.findViewById(R.id.eventCategory);
            price = itemView.findViewById(R.id.eventPrice);
            reserveButton = itemView.findViewById(R.id.reserveButton);
        }
    }
}