package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.Booking;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClick(Booking booking);
    }

    private final List<Booking> bookings;
    private final OnCancelClickListener cancelClickListener;

    public BookingAdapter(List<Booking> bookings, OnCancelClickListener cancelClickListener) {
        this.bookings = bookings;
        this.cancelClickListener = cancelClickListener;
    }

    @Override
    public BookingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.title.setText(booking.getEventTitle());

        String dateText = "Date unavailable";
        if (booking.getEventDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText = sdf.format(booking.getEventDate().toDate());
        }
        holder.date.setText(dateText);

        holder.location.setText(booking.getVenue() + ", " + booking.getCity());
        holder.price.setText(String.format(Locale.getDefault(), "$%.2f", booking.getPrice()));
        holder.status.setText("Status: " + booking.getStatus());

        String channel = booking.getConfirmationChannel() == null ? "N/A" : booking.getConfirmationChannel();
        String code = booking.getConfirmationCode() == null ? "N/A" : booking.getConfirmationCode();
        holder.confirmation.setText("Confirmation: " + code + " (" + channel + ")");
        holder.payment.setText("Payment: " + safeText(booking.getPaymentMethod())
            + " | " + safeText(booking.getPaymentStatus())
            + " | Ref " + safeText(booking.getPaymentReference()));
        holder.notification.setText("Notification: " + safeText(booking.getNotificationStatus())
            + " | " + safeText(booking.getNotificationMessage()));

        boolean isConfirmed = Booking.STATUS_CONFIRMED.equals(booking.getStatus());
        holder.cancelButton.setEnabled(isConfirmed);
        holder.cancelButton.setAlpha(isConfirmed ? 1f : 0.5f);
        holder.cancelButton.setOnClickListener(v -> {
            if (cancelClickListener != null && isConfirmed) {
                cancelClickListener.onCancelClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView date;
        TextView location;
        TextView price;
        TextView status;
        TextView confirmation;
        TextView payment;
        TextView notification;
        Button cancelButton;

        BookingViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.bookingEventTitle);
            date = itemView.findViewById(R.id.bookingEventDate);
            location = itemView.findViewById(R.id.bookingEventLocation);
            price = itemView.findViewById(R.id.bookingEventPrice);
            status = itemView.findViewById(R.id.bookingStatus);
            confirmation = itemView.findViewById(R.id.bookingConfirmation);
            payment = itemView.findViewById(R.id.bookingPayment);
            notification = itemView.findViewById(R.id.bookingNotification);
            cancelButton = itemView.findViewById(R.id.cancelTicketButton);
        }
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }
}
