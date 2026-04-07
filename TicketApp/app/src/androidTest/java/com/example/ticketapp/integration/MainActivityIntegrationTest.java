package com.example.ticketapp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketapp.MainActivity;
import com.example.ticketapp.R;
import com.example.ticketapp.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityIntegrationTest {

    @After
    public void tearDown() {
        IntegrationTestSessionHelper.setGuest();
    }

    @Test
    public void guestRole_bottomNavigationShowsGuestTabs() {
        IntegrationTestSessionHelper.setGuest();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
                assertNotNull(bottomNav);

                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_home));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_settings));
                assertEquals(2, bottomNav.getMenu().size());
            });
        }
    }

    @Test
    public void userRole_bottomNavigationShowsUserTabs() {
        IntegrationTestSessionHelper.setUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
                assertNotNull(bottomNav);

                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_home));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_tickets));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_notifications));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_settings));
                assertEquals(4, bottomNav.getMenu().size());
            });
        }
    }

    @Test
    public void adminRole_bottomNavigationShowsAdminTabs() {
        IntegrationTestSessionHelper.setAdmin();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
                assertNotNull(bottomNav);

                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_home));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_tickets));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_manage_events));
                assertNotNull(bottomNav.getMenu().findItem(R.id.nav_settings));
                assertEquals(4, bottomNav.getMenu().size());
            });
        }
    }

    @Test
    public void settingsScreen_guestSession_showsLoginAction() {
        IntegrationTestSessionHelper.setGuest();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
                assertNotNull(bottomNav);

                bottomNav.setSelectedItemId(R.id.nav_settings);
                activity.getSupportFragmentManager().executePendingTransactions();

                Button authButton = activity.findViewById(R.id.authButton);
                assertNotNull(authButton);
                assertEquals("Login", authButton.getText().toString());
            });
        }
    }

    @Test
    public void settingsScreen_userSession_showsLogoutAction() {
        IntegrationTestSessionHelper.setUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
                assertNotNull(bottomNav);

                bottomNav.setSelectedItemId(R.id.nav_settings);
                activity.getSupportFragmentManager().executePendingTransactions();

                Button authButton = activity.findViewById(R.id.authButton);
                assertNotNull(authButton);
                assertEquals("Logout", authButton.getText().toString());
            });
        }
    }

    @Test
    public void settingsScreen_logoutButton_resetsSessionToGuest() {
        IntegrationTestSessionHelper.setUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
                assertNotNull(bottomNav);
                assertFalse(UserSession.getInstance().isGuest());

                bottomNav.setSelectedItemId(R.id.nav_settings);
                activity.getSupportFragmentManager().executePendingTransactions();

                Button authButton = activity.findViewById(R.id.authButton);
                assertNotNull(authButton);
                assertEquals("Logout", authButton.getText().toString());

                authButton.performClick();

                assertTrue(UserSession.getInstance().isGuest());
            });
        }
    }
}


