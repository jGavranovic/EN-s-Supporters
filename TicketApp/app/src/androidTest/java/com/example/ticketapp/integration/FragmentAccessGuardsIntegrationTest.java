package com.example.ticketapp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketapp.MainActivity;
import com.example.ticketapp.ManageEventsFragment;
import com.example.ticketapp.MyTicketsFragment;
import com.example.ticketapp.NotificationsFragment;
import com.example.ticketapp.R;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FragmentAccessGuardsIntegrationTest {

    @After
    public void tearDown() {
        IntegrationTestSessionHelper.setGuest();
    }

    @Test
    public void manageEvents_nonAdminUser_showsAccessRequiredState() {
        IntegrationTestSessionHelper.setUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                replaceFragment(activity, new ManageEventsFragment());

                TextView infoText = activity.findViewById(R.id.manageEventsInfoText);
                TextView noEventsText = activity.findViewById(R.id.noManagedEventsText);
                Button createEventButton = activity.findViewById(R.id.createEventButton);

                assertNotNull(infoText);
                assertNotNull(noEventsText);
                assertNotNull(createEventButton);

                assertEquals("Only admin accounts can manage events", infoText.getText().toString());
                assertEquals("Admin access required", noEventsText.getText().toString());
                assertEquals(View.VISIBLE, noEventsText.getVisibility());
                assertFalse(createEventButton.isEnabled());
            });
        }
    }

    @Test
    public void myTickets_guest_showsLoginPrompt() {
        IntegrationTestSessionHelper.setGuest();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                replaceFragment(activity, new MyTicketsFragment());

                TextView noTicketsText = activity.findViewById(R.id.noTicketsText);
                assertNotNull(noTicketsText);
                assertEquals(View.VISIBLE, noTicketsText.getVisibility());
                assertEquals("Login to view and manage your tickets", noTicketsText.getText().toString());
            });
        }
    }

    @Test
    public void notifications_guest_showsLoginPrompt() {
        IntegrationTestSessionHelper.setGuest();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                replaceFragment(activity, new NotificationsFragment());

                TextView noNotificationsText = activity.findViewById(R.id.noNotificationsText);
                assertNotNull(noNotificationsText);
                assertEquals(View.VISIBLE, noNotificationsText.getVisibility());
                assertEquals("Login to view notifications", noNotificationsText.getText().toString());
            });
        }
    }

    private void replaceFragment(MainActivity activity, Fragment fragment) {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitNow();
    }
}

