package com.example.ticketapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userSession = UserSession.getInstance();

        // Load the home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
        // Set up bottom navigation
        bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            setupNavigation();
        }
    }

    //nav bar according to user type
    private void setupNavigation() {
        UserSession.UserType userType = userSession.getCurrentUserType();
        // Clear existing menu items
        bottomNav.getMenu().clear();
        if (userType == UserSession.UserType.GUEST) {
            bottomNav.inflateMenu(R.menu.bottom_nav_guest);
        } else if (userType == UserSession.UserType.USER) {
            bottomNav.inflateMenu(R.menu.bottom_nav_user);
        } else if (userType == UserSession.UserType.ADMIN) {
            bottomNav.inflateMenu(R.menu.bottom_nav_admin);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            if (item.getItemId() == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_tickets) {
                fragment = new MyTicketsFragment();
            } else if (item.getItemId() == R.id.nav_notifications) {
                fragment = new NotificationsFragment();
            } else if (item.getItemId() == R.id.nav_manage_events) {
                fragment = new ManageEventsFragment();
            } else if (item.getItemId() == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
    @Override
    public void onResume() {
        super.onResume();
        // Refresh navigation menu in case user type changed
        setupNavigation();
    }
}
