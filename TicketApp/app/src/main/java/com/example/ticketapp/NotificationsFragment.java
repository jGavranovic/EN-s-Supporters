package com.example.ticketapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.adapters.NotificationAdapter;
import com.example.ticketapp.models.UserNotification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private final List<UserNotification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private TextView emptyText;
    private ListenerRegistration listenerRegistration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        emptyText = view.findViewById(R.id.noNotificationsText);
        RecyclerView recyclerView = view.findViewById(R.id.notificationsRecyclerView);

        adapter = new NotificationAdapter(notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (UserSession.getInstance().isGuest()) {
            emptyText.setText("Login to view notifications");
            emptyText.setVisibility(View.VISIBLE);
            return view;
        }

        startNotificationListener();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void startNotificationListener() {
        String userId = UserSession.getInstance().getUserIdentifier();
        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("user_notifications")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        emptyText.setText("Failed to load notifications");
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    notifications.clear();
                    if (value != null) {
                        value.getDocuments().forEach(doc -> {
                            UserNotification notification = doc.toObject(UserNotification.class);
                            if (notification != null) {
                                notification.setId(doc.getId());
                                notifications.add(notification);
                            }
                        });
                    }

                    Collections.sort(notifications, new Comparator<UserNotification>() {
                        @Override
                        public int compare(UserNotification o1, UserNotification o2) {
                            if (o1.getCreatedAt() == null && o2.getCreatedAt() == null) {
                                return 0;
                            }
                            if (o1.getCreatedAt() == null) {
                                return 1;
                            }
                            if (o2.getCreatedAt() == null) {
                                return -1;
                            }
                            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                        }
                    });

                    adapter.notifyDataSetChanged();
                    emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    if (notifications.isEmpty()) {
                        emptyText.setText("No notifications yet");
                    }
                });
    }
}
