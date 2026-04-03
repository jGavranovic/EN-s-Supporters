package com.example.ticketapp;

import static org.junit.Assert.assertEquals;

import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
}


