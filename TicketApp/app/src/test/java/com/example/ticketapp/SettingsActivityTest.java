package com.example.ticketapp;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsActivityTest {

    @Test
    public void onCreate_setsSettingsLayout() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        assertNotNull(activity.findViewById(android.R.id.content));
    }
}

