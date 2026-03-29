package com.example.ticketapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.adapters.BookingAdapter;
import com.example.ticketapp.models.Booking;
import com.example.ticketapp.services.BookingService;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsFragment extends Fragment {

    private final List<Booking> bookings = new ArrayList<>();
    private BookingAdapter adapter;
    private TextView noTicketsText;
    private BookingService bookingService;
    private ListenerRegistration bookingListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tickets, container, false);

        bookingService = BookingService.getInstance();
        noTicketsText = view.findViewById(R.id.noTicketsText);
        RecyclerView recyclerView = view.findViewById(R.id.myTicketsRecyclerView);

        adapter = new BookingAdapter(bookings, this::handleCancelTicket);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (UserSession.getInstance().isGuest()) {
            noTicketsText.setText("Login to view and manage your tickets");
            noTicketsText.setVisibility(View.VISIBLE);
        } else {
            startBookingListener();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }

    private void startBookingListener() {
        String userId = UserSession.getInstance().getUserIdentifier();
        bookingListener = bookingService.listenToUserBookings(userId, new BookingService.BookingsListener() {
            @Override
            public void onUpdate(List<Booking> updatedBookings) {
                bookings.clear();
                bookings.addAll(updatedBookings);
                adapter.notifyDataSetChanged();
                noTicketsText.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                noTicketsText.setText(message);
                noTicketsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleCancelTicket(Booking booking) {
        if (getContext() == null) {
            return;
        }
        bookingService.cancelBooking(booking, new BookingService.CancellationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Ticket cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
