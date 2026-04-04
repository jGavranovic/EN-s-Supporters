package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.ticketapp.services.BookingService;
import com.example.ticketapp.services.EventAdminService;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void setupNavigation_inflatesGuestMenuForGuestUser() throws Exception {
        MainActivity activity = new MainActivity();
        BottomNavigationView nav = mock(BottomNavigationView.class);
        Menu menu = mock(Menu.class);
        when(nav.getMenu()).thenReturn(menu);

        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.GUEST);

        setField(activity, "bottomNav", nav);
        setField(activity, "userSession", session);

        invokeSetupNavigation(activity);

        verify(menu).clear();
        verify(nav).inflateMenu(R.menu.bottom_nav_guest);
    }

    @Test
    public void setupNavigation_inflatesAdminMenuForAdminUser() throws Exception {
        MainActivity activity = new MainActivity();
        BottomNavigationView nav = mock(BottomNavigationView.class);
        Menu menu = mock(Menu.class);
        when(nav.getMenu()).thenReturn(menu);

        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.ADMIN);

        setField(activity, "bottomNav", nav);
        setField(activity, "userSession", session);

        invokeSetupNavigation(activity);

        verify(menu).clear();
        verify(nav).inflateMenu(R.menu.bottom_nav_admin);
    }

    @Test
    public void setupNavigation_inflatesUserMenuForStandardUser() throws Exception {
        MainActivity activity = new MainActivity();
        BottomNavigationView nav = mock(BottomNavigationView.class);
        Menu menu = mock(Menu.class);
        when(nav.getMenu()).thenReturn(menu);

        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);

        setField(activity, "bottomNav", nav);
        setField(activity, "userSession", session);

        invokeSetupNavigation(activity);

        verify(menu).clear();
        verify(nav).inflateMenu(R.menu.bottom_nav_user);
    }

    @Test
    public void onCreate_loadsHomeFragmentWhenFirstLaunch() {
        try (TestStatics ignored = new TestStatics()) {
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

            assertNotNull(activity.findViewById(R.id.bottomNavigation));
            assertTrue(activity.getSupportFragmentManager().getBackStackEntryCount() >= 1);
            assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof HomeFragment);
        }
    }

    @Test
    public void onCreate_withSavedStateSkipsDefaultFragmentLoad() {
        try (TestStatics ignored = new TestStatics()) {
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
            Bundle savedState = new Bundle();
            savedState.putString("k", "v");
            MainActivity activity = controller.create(savedState).start().resume().visible().get();

            assertNull(activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        }
    }

    @Test
    public void onResume_rebuildsMenuForUpdatedRole() {
        try (TestStatics ignored = new TestStatics()) {
            UserSession.getInstance().setUserType(UserSession.UserType.GUEST);
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
            BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);
            assertEquals(2, nav.getMenu().size());

            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            activity.onResume();

            assertEquals(4, nav.getMenu().size());
            assertNotNull(nav.getMenu().findItem(R.id.nav_manage_events));
        }
    }

    @Test
    public void setupNavigation_routesMenuSelectionsToFragments() {
        try (TestStatics ignored = new TestStatics()) {
            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
            BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);

            nav.setSelectedItemId(R.id.nav_tickets);
            activity.getSupportFragmentManager().executePendingTransactions();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof MyTicketsFragment);

            // Admin menu does not include notifications, switch to user to exercise that route.
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            activity.onResume();
            nav.setSelectedItemId(R.id.nav_notifications);
            activity.getSupportFragmentManager().executePendingTransactions();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof NotificationsFragment);

            nav.setSelectedItemId(R.id.nav_settings);
            activity.getSupportFragmentManager().executePendingTransactions();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof SettingsFragment);

            UserSession.getInstance().setUserType(UserSession.UserType.ADMIN);
            activity.onResume();
            nav.setSelectedItemId(R.id.nav_manage_events);
            activity.getSupportFragmentManager().executePendingTransactions();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof ManageEventsFragment);
        }
    }

    @Test
    public void setupNavigation_ignoresUnknownMenuItems() {
        try (TestStatics ignored = new TestStatics()) {
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
            BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);
            int before = activity.getSupportFragmentManager().getBackStackEntryCount();

            MenuItem unknown = nav.getMenu().add(Menu.NONE, 99999, 100, "Unknown");
            nav.getMenu().performIdentifierAction(unknown.getItemId(), 0);

            int after = activity.getSupportFragmentManager().getBackStackEntryCount();
            assertEquals(before, after);
        }
    }

    private static class TestStatics implements AutoCloseable {
        private final MockedStatic<FirebaseFirestore> firestoreStatic;
        private final MockedStatic<BookingService> bookingServiceStatic;
        private final MockedStatic<EventAdminService> eventAdminServiceStatic;

        private TestStatics() {
            firestoreStatic = org.mockito.Mockito.mockStatic(FirebaseFirestore.class);
            bookingServiceStatic = org.mockito.Mockito.mockStatic(BookingService.class);
            eventAdminServiceStatic = org.mockito.Mockito.mockStatic(EventAdminService.class);

            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference events = mock(CollectionReference.class);
            CollectionReference notifications = mock(CollectionReference.class);
            Query notificationQuery = mock(Query.class);
            @SuppressWarnings("unchecked")
            Task<QuerySnapshot> eventsTask = mock(Task.class);
            ListenerRegistration registration = mock(ListenerRegistration.class);

            when(firestore.collection("events")).thenReturn(events);
            when(events.get()).thenReturn(eventsTask);
            when(eventsTask.addOnCompleteListener(any())).thenReturn(eventsTask);

            when(firestore.collection("user_notifications")).thenReturn(notifications);
            when(notifications.whereEqualTo(anyString(), any())).thenReturn(notificationQuery);
            when(notificationQuery.addSnapshotListener(any())).thenReturn(registration);

            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            BookingService bookingService = mock(BookingService.class);
            when(bookingService.listenToUserBookings(any(), any())).thenReturn(registration);
            bookingServiceStatic.when(BookingService::getInstance).thenReturn(bookingService);

            EventAdminService eventAdminService = mock(EventAdminService.class);
            when(eventAdminService.listenToAllEvents(any())).thenReturn(registration);
            eventAdminServiceStatic.when(EventAdminService::getInstance).thenReturn(eventAdminService);
        }

        @Override
        public void close() {
            eventAdminServiceStatic.close();
            bookingServiceStatic.close();
            firestoreStatic.close();
        }
    }

    private void invokeSetupNavigation(MainActivity activity) throws Exception {
        Method method = MainActivity.class.getDeclaredMethod("setupNavigation");
        method.setAccessible(true);
        method.invoke(activity);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}


