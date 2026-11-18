package com.example.watchchat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Reproduce the settings-entry crash on a fresh device (empty prefs). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public final class SettingsLaunchTest {
    @Test public void settingsLaunchesOnFreshDevice() {
        ActivityController<SettingsActivity> c = Robolectric.buildActivity(SettingsActivity.class).setup();
        if (c.get().isFinishing()) throw new AssertionError("settings finished itself (fallback page shown)");
    }
}
