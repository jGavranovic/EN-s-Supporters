package com.example.ticketapp;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MyTicketsActivityTest {

    @Test
    public void onCreate_setsContentView() {
        MyTicketsActivity activity = Robolectric.buildActivity(MyTicketsActivity.class).setup().get();
        assertNotNull(activity.findViewById(android.R.id.content));
    }
}

