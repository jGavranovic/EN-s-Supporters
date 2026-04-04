package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.models.Booking;
import com.example.ticketapp.services.BookingService;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MyTicketsFragmentTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void onCreateView_guestShowsLoginMessage() {
        BookingService bookingService = mock(BookingService.class);
        try (MockedStatic<BookingService> bookingServiceMock = org.mockito.Mockito.mockStatic(BookingService.class)) {
            bookingServiceMock.when(BookingService::getInstance).thenReturn(bookingService);
            UserSession.getInstance().logout();

            MyTicketsFragment fragment = new MyTicketsFragment();
            AppCompatActivity activity = hostFragment(fragment);

            TextView noTickets = activity.findViewById(R.id.noTicketsText);
            assertEquals("Login to view and manage your tickets", noTickets.getText().toString());
            assertEquals(TextView.VISIBLE, noTickets.getVisibility());
            verify(bookingService, never()).listenToUserBookings(any(), any());
        }
    }

    @Test
    public void onCreateView_userStartsListenerAndHandlesUpdateAndError() {
        BookingService bookingService = mock(BookingService.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<BookingService.BookingsListener> listenerCaptor = ArgumentCaptor.forClass(BookingService.BookingsListener.class);
        when(bookingService.listenToUserBookings(any(), listenerCaptor.capture())).thenReturn(registration);

        try (MockedStatic<BookingService> bookingServiceMock = org.mockito.Mockito.mockStatic(BookingService.class)) {
            bookingServiceMock.when(BookingService::getInstance).thenReturn(bookingService);
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            UserSession.getInstance().setIdentity("user@example.com", "EMAIL");

            MyTicketsFragment fragment = new MyTicketsFragment();
            AppCompatActivity activity = hostFragment(fragment);
            TextView noTickets = activity.findViewById(R.id.noTicketsText);

            Booking booking = new Booking();
            booking.setId("b1");
            listenerCaptor.getValue().onUpdate(Arrays.asList(booking));
            assertEquals(TextView.GONE, noTickets.getVisibility());

            listenerCaptor.getValue().onUpdate(Collections.emptyList());
            assertEquals(TextView.VISIBLE, noTickets.getVisibility());

            listenerCaptor.getValue().onError("load failed");
            assertEquals("load failed", noTickets.getText().toString());
            assertEquals(TextView.VISIBLE, noTickets.getVisibility());
        }
    }

    @Test
    public void onDestroyView_removesBookingListener() throws Exception {
        MyTicketsFragment fragment = new MyTicketsFragment();
        ListenerRegistration registration = mock(ListenerRegistration.class);
        setPrivateField(fragment, "bookingListener", registration);

        fragment.onDestroyView();

        verify(registration).remove();
    }

    @Test
    public void handleCancelTicket_showsSuccessAndErrorToasts() throws Exception {
        BookingService bookingService = mock(BookingService.class);
        ListenerRegistration registration = mock(ListenerRegistration.class);
        when(bookingService.listenToUserBookings(any(), any())).thenReturn(registration);

        try (MockedStatic<BookingService> bookingServiceMock = org.mockito.Mockito.mockStatic(BookingService.class)) {
            bookingServiceMock.when(BookingService::getInstance).thenReturn(bookingService);
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            UserSession.getInstance().setIdentity("user@example.com", "EMAIL");

            MyTicketsFragment fragment = new MyTicketsFragment();
            hostFragment(fragment);

            Booking booking = new Booking();
            booking.setId("b22");

            org.mockito.Mockito.doAnswer(invocation -> {
                BookingService.CancellationCallback callback = invocation.getArgument(1);
                callback.onSuccess();
                return null;
            }).when(bookingService).cancelBooking(eq(booking), any());

            invokeHandleCancelTicket(fragment, booking);
            assertEquals("Ticket cancelled", ShadowToast.getTextOfLatestToast());

            org.mockito.Mockito.doAnswer(invocation -> {
                BookingService.CancellationCallback callback = invocation.getArgument(1);
                callback.onError("cancel failed");
                return null;
            }).when(bookingService).cancelBooking(eq(booking), any());

            invokeHandleCancelTicket(fragment, booking);
            assertEquals("cancel failed", ShadowToast.getTextOfLatestToast());
        }
    }

    @Test
    public void handleCancelTicket_withNoContext_returnsEarly() throws Exception {
        BookingService bookingService = mock(BookingService.class);
        MyTicketsFragment fragment = new MyTicketsFragment();
        setPrivateField(fragment, "bookingService", bookingService);

        invokeHandleCancelTicket(fragment, new Booking());

        verify(bookingService, never()).cancelBooking(any(), any());
    }

    private AppCompatActivity hostFragment(MyTicketsFragment fragment) {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
        FrameLayout container = new FrameLayout(activity);
        container.setId(android.R.id.content);
        activity.setContentView(container);
        activity.getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        return activity;
    }

    private void invokeHandleCancelTicket(MyTicketsFragment fragment, Booking booking) throws Exception {
        Method method = MyTicketsFragment.class.getDeclaredMethod("handleCancelTicket", Booking.class);
        method.setAccessible(true);
        method.invoke(fragment, booking);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

