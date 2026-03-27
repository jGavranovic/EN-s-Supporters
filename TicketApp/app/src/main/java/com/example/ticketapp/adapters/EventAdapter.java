package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ticketapp.R;
import com.example.ticketapp.models.Event;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<Event> events;

    public EventAdapter(List<Event> events) { this.events = events; }

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
    }

    @Override
    public int getItemCount() {return events.size(); }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, location, category, price;
        EventViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.eventTitle);
            date = itemView.findViewById(R.id.eventDate);
            location = itemView.findViewById(R.id.eventLocation);
            category = itemView.findViewById(R.id.eventCategory);
            price = itemView.findViewById(R.id.eventPrice);
        }
    }
}