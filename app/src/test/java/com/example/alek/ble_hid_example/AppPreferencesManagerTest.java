package com.example.alek.ble_hid_example;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AppPreferencesManagerTest {

    private AppPreferencesManager prefsManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefsManager = new AppPreferencesManager(context);
    }

    @Test
    public void testLastTabPersistence() {
        prefsManager.setLastTab(3);
        assertEquals(3, prefsManager.getLastTab());

        // Reload cache and verify persisted state
        prefsManager.reloadCache();
        assertEquals(3, prefsManager.getLastTab());
    }

    @Test
    public void testGamepadTogglesPersistence() {
        prefsManager.setGamepadLStickEnabled(false);
        assertFalse(prefsManager.isGamepadLStickEnabled());

        prefsManager.setGamepadRStickEnabled(false);
        assertFalse(prefsManager.isGamepadRStickEnabled());

        prefsManager.setGamepadTouchEnabled(false);
        assertFalse(prefsManager.isGamepadTouchEnabled());

        prefsManager.setGamepadLockEnabled(true);
        assertTrue(prefsManager.isGamepadLockEnabled());

        // Reload cache from SharedPreferences
        prefsManager.reloadCache();
        assertFalse(prefsManager.isGamepadLStickEnabled());
        assertFalse(prefsManager.isGamepadRStickEnabled());
        assertFalse(prefsManager.isGamepadTouchEnabled());
        assertTrue(prefsManager.isGamepadLockEnabled());
    }

    @Test
    public void testRestoreNamePersistence() {
        prefsManager.setRestoreNameEnabled(true);
        assertTrue(prefsManager.isRestoreNameEnabled());

        prefsManager.reloadCache();
        assertTrue(prefsManager.isRestoreNameEnabled());
    }
}
