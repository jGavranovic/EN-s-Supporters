package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.models.Event;
import com.example.ticketapp.services.EventAdminService;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

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

    @Test
    public void parseEventFromInputs_rejectsAllEmptyFieldBranches() throws Exception {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        ManageEventsFragment fragment = new ManageEventsFragment();

        // title empty
        FormFields titleEmpty = validFields(activity);
        titleEmpty.title.setText(" ");
        Event parsedTitle = invokeParse(fragment, titleEmpty.title, titleEmpty.date, titleEmpty.venue,
                titleEmpty.city, titleEmpty.category, titleEmpty.price, titleEmpty.seats);
        assertNull(parsedTitle);
        assertEquals("Title is required", titleEmpty.title.getError().toString());

        // date empty
        FormFields dateEmpty = validFields(activity);
        dateEmpty.date.setText(" ");
        Event parsedDate = invokeParse(fragment, dateEmpty.title, dateEmpty.date, dateEmpty.venue,
                dateEmpty.city, dateEmpty.category, dateEmpty.price, dateEmpty.seats);
        assertNull(parsedDate);
        assertEquals("Date is required", dateEmpty.date.getError().toString());

        // venue empty
        FormFields venueEmpty = validFields(activity);
        venueEmpty.venue.setText(" ");
        Event parsedVenue = invokeParse(fragment, venueEmpty.title, venueEmpty.date, venueEmpty.venue,
                venueEmpty.city, venueEmpty.category, venueEmpty.price, venueEmpty.seats);
        assertNull(parsedVenue);
        assertEquals("Venue is required", venueEmpty.venue.getError().toString());

        // city empty
        FormFields cityEmpty = validFields(activity);
        cityEmpty.city.setText(" ");
        Event parsedCity = invokeParse(fragment, cityEmpty.title, cityEmpty.date, cityEmpty.venue,
                cityEmpty.city, cityEmpty.category, cityEmpty.price, cityEmpty.seats);
        assertNull(parsedCity);
        assertEquals("City is required", cityEmpty.city.getError().toString());

        // category empty
        FormFields categoryEmpty = validFields(activity);
        categoryEmpty.category.setText(" ");
        Event parsedCategory = invokeParse(fragment, categoryEmpty.title, categoryEmpty.date, categoryEmpty.venue,
                categoryEmpty.city, categoryEmpty.category, categoryEmpty.price, categoryEmpty.seats);
        assertNull(parsedCategory);
        assertEquals("Category is required", categoryEmpty.category.getError().toString());

        // price empty
        FormFields priceEmpty = validFields(activity);
        priceEmpty.price.setText(" ");
        Event parsedPrice = invokeParse(fragment, priceEmpty.title, priceEmpty.date, priceEmpty.venue,
                priceEmpty.city, priceEmpty.category, priceEmpty.price, priceEmpty.seats);
        assertNull(parsedPrice);
        assertEquals("Price is required", priceEmpty.price.getError().toString());

        // seats empty
        FormFields seatsEmpty = validFields(activity);
        seatsEmpty.seats.setText(" ");
        Event parsedSeats = invokeParse(fragment, seatsEmpty.title, seatsEmpty.date, seatsEmpty.venue,
                seatsEmpty.city, seatsEmpty.category, seatsEmpty.price, seatsEmpty.seats);
        assertNull(parsedSeats);
        assertEquals("Seats are required", seatsEmpty.seats.getError().toString());
    }

    @Test
    public void showEventFormDialog_createModeShowsCreateDialog() throws Exception {
        EventAdminService service = mock(EventAdminService.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        when(service.listenToAllEvents(any())).thenReturn(registration);

        try (MockedStatic<EventAdminService> serviceMock = org.mockito.Mockito.mockStatic(EventAdminService.class)) {
            serviceMock.when(EventAdminService::getInstance).thenReturn(service);
            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            ManageEventsFragment fragment = new ManageEventsFragment();
            hostFragment(fragment);

            invokeShowEventFormDialog(fragment, null);
            android.app.AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(dialog);
            assertEquals("Create", dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).getText().toString());
            shadowOf(Looper.getMainLooper()).idle();

            ((EditText) dialog.findViewById(R.id.eventFormTitle)).setText("Launch Party");
            ((EditText) dialog.findViewById(R.id.eventFormDateTime)).setText("2026-06-01 18:00");
            ((EditText) dialog.findViewById(R.id.eventFormVenue)).setText("Main Hall");
            ((EditText) dialog.findViewById(R.id.eventFormCity)).setText("Montreal");
            ((EditText) dialog.findViewById(R.id.eventFormCategory)).setText("Community");
            ((EditText) dialog.findViewById(R.id.eventFormPrice)).setText("10.5");
            ((EditText) dialog.findViewById(R.id.eventFormSeats)).setText("40");

            org.mockito.Mockito.doAnswer(invocation -> {
                EventAdminService.AdminActionCallback callback = invocation.getArgument(1);
                callback.onSuccess();
                return null;
            }).when(service).postEvent(any(), any());

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();

            verify(service).postEvent(any(), any());
            assertEquals("Launch Party", ((EditText) dialog.findViewById(R.id.eventFormTitle)).getText().toString());
            assertEquals("2026-06-01 18:00", ((EditText) dialog.findViewById(R.id.eventFormDateTime)).getText().toString());
        }
    }

    @Test
    public void showEventFormDialog_editModePrefillsAndShowsSaveButton() throws Exception {
        EventAdminService service = mock(EventAdminService.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        when(service.listenToAllEvents(any())).thenReturn(registration);

        try (MockedStatic<EventAdminService> serviceMock = org.mockito.Mockito.mockStatic(EventAdminService.class)) {
            serviceMock.when(EventAdminService::getInstance).thenReturn(service);
            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            ManageEventsFragment fragment = new ManageEventsFragment();
            hostFragment(fragment);

            Event existing = new Event();
            existing.setId("event-77");
            existing.setTitle("Old Title");
            existing.setDate(new Timestamp(new java.util.Date(1780000000000L)));
            existing.setVenue("Old Venue");
            existing.setCity("Old City");
            existing.setCategory("Old Category");
            existing.setPrice(55.0);
            existing.setSeatsAvailable(90);

            invokeShowEventFormDialog(fragment, existing);
            android.app.AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(dialog);
            assertEquals("Save", dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).getText().toString());
            shadowOf(Looper.getMainLooper()).idle();

            assertEquals("Old Title", ((EditText) dialog.findViewById(R.id.eventFormTitle)).getText().toString());
            assertEquals("Old Venue", ((EditText) dialog.findViewById(R.id.eventFormVenue)).getText().toString());
            assertEquals("Old City", ((EditText) dialog.findViewById(R.id.eventFormCity)).getText().toString());
            assertEquals("Old Category", ((EditText) dialog.findViewById(R.id.eventFormCategory)).getText().toString());
            assertEquals("55.0", ((EditText) dialog.findViewById(R.id.eventFormPrice)).getText().toString());
            assertEquals("90", ((EditText) dialog.findViewById(R.id.eventFormSeats)).getText().toString());
            String expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(existing.getDate().toDate());
            assertEquals(expectedDate, ((EditText) dialog.findViewById(R.id.eventFormDateTime)).getText().toString());

            ((EditText) dialog.findViewById(R.id.eventFormTitle)).setText("New Title");

            org.mockito.Mockito.doAnswer(invocation -> {
                EventAdminService.AdminActionCallback callback = invocation.getArgument(2);
                callback.onError("save failed");
                return null;
            }).when(service).putEvent(eq("event-77"), any(), any());

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();

            verify(service).putEvent(eq("event-77"), any(), any());
            assertTrue(dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled());
            assertEquals("New Title", ((EditText) dialog.findViewById(R.id.eventFormTitle)).getText().toString());
        }
    }

    @Test
    public void parseEventFromInputs_rejectsInvalidPriceAndSeatsValues() throws Exception {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        ManageEventsFragment fragment = new ManageEventsFragment();

        FormFields negativePrice = validFields(activity);
        negativePrice.price.setText("-1");
        Event parsedNegativePrice = invokeParse(fragment, negativePrice.title, negativePrice.date, negativePrice.venue,
                negativePrice.city, negativePrice.category, negativePrice.price, negativePrice.seats);
        assertNull(parsedNegativePrice);
        assertEquals("Enter a valid non-negative price", negativePrice.price.getError().toString());

        FormFields badPrice = validFields(activity);
        badPrice.price.setText("abc");
        Event parsedBadPrice = invokeParse(fragment, badPrice.title, badPrice.date, badPrice.venue,
                badPrice.city, badPrice.category, badPrice.price, badPrice.seats);
        assertNull(parsedBadPrice);
        assertEquals("Enter a valid non-negative price", badPrice.price.getError().toString());

        FormFields negativeSeats = validFields(activity);
        negativeSeats.seats.setText("-10");
        Event parsedNegativeSeats = invokeParse(fragment, negativeSeats.title, negativeSeats.date, negativeSeats.venue,
                negativeSeats.city, negativeSeats.category, negativeSeats.price, negativeSeats.seats);
        assertNull(parsedNegativeSeats);
        assertEquals("Enter a valid non-negative seat count", negativeSeats.seats.getError().toString());

        FormFields badSeats = validFields(activity);
        badSeats.seats.setText("x");
        Event parsedBadSeats = invokeParse(fragment, badSeats.title, badSeats.date, badSeats.venue,
                badSeats.city, badSeats.category, badSeats.price, badSeats.seats);
        assertNull(parsedBadSeats);
        assertEquals("Enter a valid non-negative seat count", badSeats.seats.getError().toString());
    }

    @Test
    public void onCreateView_admin_startsListenerAndHandlesUpdateAndError() {
        EventAdminService service = mock(EventAdminService.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventAdminService.EventListListener> listenerCaptor = ArgumentCaptor.forClass(EventAdminService.EventListListener.class);
        when(service.listenToAllEvents(listenerCaptor.capture())).thenReturn(registration);

        try (MockedStatic<EventAdminService> serviceMock = org.mockito.Mockito.mockStatic(EventAdminService.class)) {
            serviceMock.when(EventAdminService::getInstance).thenReturn(service);
            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            ManageEventsFragment fragment = new ManageEventsFragment();
            AppCompatActivity activity = hostFragment(fragment);

            Event event = new Event();
            event.setId("evt1");
            event.setTitle("Loaded");
            listenerCaptor.getValue().onUpdate(Arrays.asList(event));

            TextView noEventsText = activity.findViewById(R.id.noManagedEventsText);
            assertEquals(TextView.GONE, noEventsText.getVisibility());

            listenerCaptor.getValue().onUpdate(Collections.emptyList());
            assertEquals(TextView.VISIBLE, noEventsText.getVisibility());

            listenerCaptor.getValue().onError("Load failed");
            assertEquals("Load failed", noEventsText.getText().toString());
            assertEquals(TextView.VISIBLE, noEventsText.getVisibility());
        }
    }

    @Test
    public void onDestroyView_removesListenerRegistrationWhenPresent() throws Exception {
        ManageEventsFragment fragment = new ManageEventsFragment();
        ListenerRegistration registration = mock(ListenerRegistration.class);
        setPrivateField(fragment, "listenerRegistration", registration);

        fragment.onDestroyView();

        verify(registration).remove();
    }

    @Test
    public void showDeleteConfirmation_handlesGuardAndDeleteCallbacks() throws Exception {
        EventAdminService service = mock(EventAdminService.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        when(service.listenToAllEvents(any())).thenReturn(registration);

        try (MockedStatic<EventAdminService> serviceMock = org.mockito.Mockito.mockStatic(EventAdminService.class)) {
            serviceMock.when(EventAdminService::getInstance).thenReturn(service);
            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            ManageEventsFragment fragment = new ManageEventsFragment();
            hostFragment(fragment);

            Event invalid = new Event();
            invalid.setId(" ");
            invokeShowDeleteConfirmation(fragment, invalid);
            verify(service, never()).deleteEvent(any(), any());

            Event valid = new Event();
            valid.setId("event-del");

            org.mockito.Mockito.doAnswer(invocation -> {
                EventAdminService.AdminActionCallback cb = invocation.getArgument(1);
                cb.onSuccess();
                return null;
            }).when(service).deleteEvent(eq("event-del"), any());

            invokeShowDeleteConfirmation(fragment, valid);
            android.app.AlertDialog confirm = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(confirm);
            confirm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            shadowOf(Looper.getMainLooper()).idle();
            verify(service).deleteEvent(eq("event-del"), any());
            assertEquals("Event deleted", ShadowToast.getTextOfLatestToast());
        }
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

    private void invokeShowEventFormDialog(ManageEventsFragment fragment, Event existing) throws Exception {
        Method method = ManageEventsFragment.class.getDeclaredMethod("showEventFormDialog", Event.class);
        method.setAccessible(true);
        method.invoke(fragment, existing);
    }

    private void invokeShowDeleteConfirmation(ManageEventsFragment fragment, Event event) throws Exception {
        Method method = ManageEventsFragment.class.getDeclaredMethod("showDeleteConfirmation", Event.class);
        method.setAccessible(true);
        method.invoke(fragment, event);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private FormFields validFields(AppCompatActivity activity) {
        FormFields fields = new FormFields();
        fields.title = new EditText(activity);
        fields.date = new EditText(activity);
        fields.venue = new EditText(activity);
        fields.city = new EditText(activity);
        fields.category = new EditText(activity);
        fields.price = new EditText(activity);
        fields.seats = new EditText(activity);

        fields.title.setText("Concert");
        fields.date.setText("2026-05-21 19:30");
        fields.venue.setText("Place Bell");
        fields.city.setText("Laval");
        fields.category.setText("Music");
        fields.price.setText("49.99");
        fields.seats.setText("100");
        return fields;
    }

    private static class FormFields {
        private EditText title;
        private EditText date;
        private EditText venue;
        private EditText city;
        private EditText category;
        private EditText price;
        private EditText seats;
    }
}


