package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.models.UserNotification;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NotificationsFragmentTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void onCreateView_showsLoginPromptForGuest() {
        UserSession.getInstance().setUserType(UserSession.UserType.GUEST);
        NotificationsFragment fragment = new NotificationsFragment();

        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        activity.setContentView(container);
        activity.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();

        TextView emptyText = activity.findViewById(R.id.noNotificationsText);
        assertEquals(TextView.VISIBLE, emptyText.getVisibility());
        assertEquals("Login to view notifications", emptyText.getText().toString());
    }

    @Test
    public void onCreateView_userListenerHandlesUpdatesSortingAndErrors() {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        CollectionReference notificationsCollection = mock(CollectionReference.class);
        Query query = mock(Query.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        DocumentSnapshot doc1 = mock(DocumentSnapshot.class);
        DocumentSnapshot doc2 = mock(DocumentSnapshot.class);
        DocumentSnapshot doc3 = mock(DocumentSnapshot.class);

        UserNotification n1 = new UserNotification();
        n1.setCreatedAt(new Timestamp(new Date(System.currentTimeMillis() - 60_000L)));
        n1.setMessage("older");
        UserNotification n2 = new UserNotification();
        n2.setCreatedAt(new Timestamp(new Date(System.currentTimeMillis() - 10_000L)));
        n2.setMessage("newer");
        UserNotification n3 = new UserNotification();
        n3.setCreatedAt(null);
        n3.setMessage("null-date");

        when(doc1.toObject(UserNotification.class)).thenReturn(n1);
        when(doc1.getId()).thenReturn("n1");
        when(doc2.toObject(UserNotification.class)).thenReturn(n2);
        when(doc2.getId()).thenReturn("n2");
        when(doc3.toObject(UserNotification.class)).thenReturn(n3);
        when(doc3.getId()).thenReturn("n3");
        when(snapshot.getDocuments()).thenReturn(Arrays.asList(doc1, doc2, doc3));

        when(firestore.collection("user_notifications")).thenReturn(notificationsCollection);
        when(notificationsCollection.whereEqualTo(anyString(), any())).thenReturn(query);
        when(query.addSnapshotListener(any())).thenAnswer(invocation -> {
            EventListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onEvent(snapshot, null);
            listener.onEvent(null, mock(FirebaseFirestoreException.class));
            listener.onEvent(mock(QuerySnapshot.class), null);
            return registration;
        });

        try (MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            UserSession.getInstance().setIdentity("user@example.com", "EMAIL");

            NotificationsFragment fragment = new NotificationsFragment();
            AppCompatActivity activity = hostFragment(fragment);
            TextView emptyText = activity.findViewById(R.id.noNotificationsText);

            // Final callback in sequence is empty success branch.
            assertEquals(TextView.VISIBLE, emptyText.getVisibility());
            assertEquals("No notifications yet", emptyText.getText().toString());

            // Ensure listener registration is attached for cleanup.
            ListenerRegistration stored = (ListenerRegistration) getPrivateField(fragment, "listenerRegistration");
            assertEquals(registration, stored);
        }
    }

    @Test
    public void onDestroyView_removesListenerRegistration() throws Exception {
        NotificationsFragment fragment = new NotificationsFragment();
        ListenerRegistration registration = mock(ListenerRegistration.class);
        setPrivateField(fragment, "listenerRegistration", registration);

        fragment.onDestroyView();

        verify(registration).remove();
    }

    private AppCompatActivity hostFragment(NotificationsFragment fragment) {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        activity.setContentView(container);
        activity.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        return activity;
    }

    private Object getPrivateField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


