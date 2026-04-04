package com.example.ticketapp.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.Booking;
import com.google.firebase.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BookingAdapterTest {

    @Test
    public void onBindViewHolder_bindsConfirmedBookingAndTriggersCancel() {
        Booking booking = sampleBooking();
        booking.setStatus(Booking.STATUS_CONFIRMED);
        booking.setConfirmationChannel("EMAIL");
        booking.setConfirmationCode("CODE123");
        booking.setPaymentMethod("VISA");
        booking.setPaymentStatus(Booking.PAYMENT_STATUS_PAID);
        booking.setPaymentReference("PAY-1");
        booking.setNotificationStatus(Booking.NOTIFICATION_STATUS_PENDING);
        booking.setNotificationMessage("Queued");

        AtomicReference<Booking> cancelled = new AtomicReference<>();
        BookingAdapter adapter = new BookingAdapter(Arrays.asList(booking), cancelled::set);
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        BookingAdapter.BookingViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Rock Show", text(holder.itemView, R.id.bookingEventTitle));
        assertEquals(format(booking.getEventDate()), text(holder.itemView, R.id.bookingEventDate));
        assertEquals("Bell Centre, Montreal", text(holder.itemView, R.id.bookingEventLocation));
        assertEquals("$50.00", text(holder.itemView, R.id.bookingEventPrice));
        assertEquals("Status: CONFIRMED", text(holder.itemView, R.id.bookingStatus));
        assertEquals("Confirmation: CODE123 (EMAIL)", text(holder.itemView, R.id.bookingConfirmation));
        assertEquals("Payment: VISA | PAID | Ref PAY-1", text(holder.itemView, R.id.bookingPayment));
        assertEquals("Notification: PENDING | Queued", text(holder.itemView, R.id.bookingNotification));

        Button cancelButton = holder.itemView.findViewById(R.id.cancelTicketButton);
        assertTrue(cancelButton.isEnabled());
        assertEquals(1f, cancelButton.getAlpha(), 0.0001f);

        cancelButton.performClick();
        assertEquals(booking, cancelled.get());
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void onBindViewHolder_nonConfirmedBookingDisablesCancelAndUsesFallbacks() {
        Booking booking = sampleBooking();
        booking.setEventDate(null);
        booking.setStatus(Booking.STATUS_CANCELLED);
        booking.setConfirmationChannel(null);
        booking.setConfirmationCode(null);
        booking.setPaymentMethod(" ");
        booking.setPaymentStatus(null);
        booking.setPaymentReference("  ");
        booking.setNotificationStatus(null);
        booking.setNotificationMessage("   ");

        AtomicReference<Booking> cancelled = new AtomicReference<>();
        BookingAdapter adapter = new BookingAdapter(Arrays.asList(booking), cancelled::set);
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        BookingAdapter.BookingViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Date unavailable", text(holder.itemView, R.id.bookingEventDate));
        assertEquals("Confirmation: N/A (N/A)", text(holder.itemView, R.id.bookingConfirmation));
        assertEquals("Payment: N/A | N/A | Ref N/A", text(holder.itemView, R.id.bookingPayment));
        assertEquals("Notification: N/A | N/A", text(holder.itemView, R.id.bookingNotification));

        Button cancelButton = holder.itemView.findViewById(R.id.cancelTicketButton);
        assertFalse(cancelButton.isEnabled());
        assertEquals(0.5f, cancelButton.getAlpha(), 0.0001f);

        cancelButton.performClick();
        assertNull(cancelled.get());
    }

    @Test
    public void onBindViewHolder_nullListenerDoesNotCrash() {
        Booking booking = sampleBooking();
        booking.setStatus(Booking.STATUS_CONFIRMED);

        BookingAdapter adapter = new BookingAdapter(Arrays.asList(booking), null);
        ViewGroup parent = new FrameLayout(Robolectric.buildActivity(Activity.class).setup().get());
        BookingAdapter.BookingViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        holder.itemView.<Button>findViewById(R.id.cancelTicketButton).performClick();

        assertTrue(true);
    }

    private Booking sampleBooking() {
        Booking booking = new Booking();
        booking.setId("booking-1");
        booking.setEventTitle("Rock Show");
        booking.setEventDate(new Timestamp(new Date(System.currentTimeMillis() + 60_000L)));
        booking.setVenue("Bell Centre");
        booking.setCity("Montreal");
        booking.setPrice(50.0);
        return booking;
    }

    private String format(Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp.toDate());
    }

    private String text(android.view.View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }
}



