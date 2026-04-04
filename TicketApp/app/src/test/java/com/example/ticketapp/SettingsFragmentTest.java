package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsFragmentTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void onCreateView_guestShowsLoginAndNavigatesToLogin() {
        UserSession.getInstance().logout();
        AppCompatActivity activity = hostFragment(new SettingsFragment());

        Button authButton = activity.findViewById(R.id.authButton);
        assertEquals("Login", authButton.getText().toString());

        authButton.performClick();

        Intent nextIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(LoginActivity.class.getName(), nextIntent.getComponent().getClassName());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void onCreateView_userShowsLogoutSignsOutAndNavigatesToLogin() {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            UserSession.getInstance().setIdentity("user@example.com", "EMAIL");
            AppCompatActivity activity = hostFragment(new SettingsFragment());

            Button authButton = activity.findViewById(R.id.authButton);
            assertEquals("Logout", authButton.getText().toString());

            authButton.performClick();

            verify(firebaseAuth).signOut();
            assertTrue(UserSession.getInstance().isGuest());
            Intent nextIntent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(LoginActivity.class.getName(), nextIntent.getComponent().getClassName());
            assertTrue(activity.isFinishing());
        }
    }

    private AppCompatActivity hostFragment(SettingsFragment fragment) {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        activity.setContentView(container);
        activity.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        return activity;
    }
}

