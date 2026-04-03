package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.models.Event;
import com.example.ticketapp.services.EventAdminService;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ManageEventsFragmentTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void onCreateView_blocksNonAdminUsers() {
        try (MockedStatic<EventAdminService> serviceMock = org.mockito.Mockito.mockStatic(EventAdminService.class)) {
            serviceMock.when(EventAdminService::getInstance).thenReturn(mock(EventAdminService.class));

            UserSession.getInstance().setUserType(UserSession.UserType.GUEST);
            ManageEventsFragment fragment = new ManageEventsFragment();
            AppCompatActivity activity = hostFragment(fragment);

            TextView infoText = activity.findViewById(R.id.manageEventsInfoText);
            TextView noEventsText = activity.findViewById(R.id.noManagedEventsText);
            Button createButton = activity.findViewById(R.id.createEventButton);

            assertEquals("Only admin accounts can manage events", infoText.getText().toString());
            assertEquals("Admin access required", noEventsText.getText().toString());
            assertEquals(TextView.VISIBLE, noEventsText.getVisibility());
            assertTrue(!createButton.isEnabled());
        }
    }

    @Test
    public void parseEventFromInputs_rejectsBadDateFormat() throws Exception {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        ManageEventsFragment fragment = new ManageEventsFragment();

        EditText title = new EditText(activity);
        EditText date = new EditText(activity);
        EditText venue = new EditText(activity);
        EditText city = new EditText(activity);
        EditText category = new EditText(activity);
        EditText price = new EditText(activity);
        EditText seats = new EditText(activity);

        title.setText("Concert");
        date.setText("04-03-2026");
        venue.setText("Place Bell");
        city.setText("Laval");
        category.setText("Music");
        price.setText("49.99");
        seats.setText("100");

        Event parsed = invokeParse(fragment, title, date, venue, city, category, price, seats);

        assertNull(parsed);
        assertEquals("Use format yyyy-MM-dd HH:mm", date.getError().toString());
    }

    @Test
    public void parseEventFromInputs_acceptsValidForm() throws Exception {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        ManageEventsFragment fragment = new ManageEventsFragment();

        EditText title = new EditText(activity);
        EditText date = new EditText(activity);
        EditText venue = new EditText(activity);
        EditText city = new EditText(activity);
        EditText category = new EditText(activity);
        EditText price = new EditText(activity);
        EditText seats = new EditText(activity);

        title.setText("Tech Meetup");
        date.setText("2026-05-21 19:30");
        venue.setText("Innovation Hall");
        city.setText("Montreal");
        category.setText("Technology");
        price.setText("25.50");
        seats.setText("75");

        Event parsed = invokeParse(fragment, title, date, venue, city, category, price, seats);

        assertNotNull(parsed);
        assertEquals("Tech Meetup", parsed.getTitle());
        assertEquals(25.50, parsed.getPrice(), 0.0001);
        assertEquals(75, parsed.getSeatsAvailable());
        assertNotNull(parsed.getDate());
    }

    private AppCompatActivity hostFragment(ManageEventsFragment fragment) {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        activity.setContentView(container);
        activity.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        return activity;
    }

    private Event invokeParse(ManageEventsFragment fragment,
                              EditText title,
                              EditText date,
                              EditText venue,
                              EditText city,
                              EditText category,
                              EditText price,
                              EditText seats) throws Exception {
        Method method = ManageEventsFragment.class.getDeclaredMethod(
                "parseEventFromInputs",
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class,
                EditText.class);
        method.setAccessible(true);
        return (Event) method.invoke(fragment, title, date, venue, city, category, price, seats);
    }
}


