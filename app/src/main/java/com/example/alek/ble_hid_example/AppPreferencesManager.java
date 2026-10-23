/*
 * Copyright 2018 Aleksander Drewnicki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.alek.ble_hid_example;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferencesManager {
    private static final String PREF_APP = "app_prefs";
    private static final String PREF_DEVICE = "ble_device_prefs";

    private static final String KEY_LAST_TAB = "last_tab";
    private static final String KEY_GAMEPAD_L_STICK = "gamepad_l_stick";
    private static final String KEY_GAMEPAD_TOUCH = "gamepad_touch";
    private static final String KEY_GAMEPAD_LOCK = "gamepad_lock";
    private static final String KEY_GAMEPAD_R_STICK = "gamepad_r_stick";
    private static final String KEY_RESTORE_NAME = "restore_original_name";

    private static volatile AppPreferencesManager instance;

    private final SharedPreferences appPrefs;
    private final SharedPreferences devicePrefs;

    // In-Memory Cache for 0ms reads
    private volatile int cachedLastTab;
    private volatile boolean cachedGamepadLStick;
    private volatile boolean cachedGamepadTouch;
    private volatile boolean cachedGamepadLock;
    private volatile boolean cachedGamepadRStick;
    private volatile boolean cachedRestoreName;

    public AppPreferencesManager(Context context) {
        Context appContext = context.getApplicationContext();
        appPrefs = appContext.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE);
        devicePrefs = appContext.getSharedPreferences(PREF_DEVICE, Context.MODE_PRIVATE);
        reloadCache();
    }

    public static AppPreferencesManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AppPreferencesManager.class) {
                if (instance == null) {
                    instance = new AppPreferencesManager(context);
                }
            }
        }
        return instance;
    }

    public synchronized void reloadCache() {
        cachedLastTab = appPrefs.getInt(KEY_LAST_TAB, 1);
        cachedGamepadLStick = appPrefs.getBoolean(KEY_GAMEPAD_L_STICK, true);
        cachedGamepadTouch = appPrefs.getBoolean(KEY_GAMEPAD_TOUCH, true);
        cachedGamepadLock = appPrefs.getBoolean(KEY_GAMEPAD_LOCK, false);
        cachedGamepadRStick = appPrefs.getBoolean(KEY_GAMEPAD_R_STICK, true);
        cachedRestoreName = devicePrefs.getBoolean(KEY_RESTORE_NAME, false);
    }

    // --- Tab Navigation ---
    public int getLastTab() {
        return cachedLastTab;
    }

    public void setLastTab(int position) {
        if (cachedLastTab != position) {
            cachedLastTab = position;
            appPrefs.edit().putInt(KEY_LAST_TAB, position).apply();
        }
    }

    // --- Gamepad Toggles ---
    public boolean isGamepadLStickEnabled() {
        return cachedGamepadLStick;
    }

    public void setGamepadLStickEnabled(boolean enabled) {
        if (cachedGamepadLStick != enabled) {
            cachedGamepadLStick = enabled;
            appPrefs.edit().putBoolean(KEY_GAMEPAD_L_STICK, enabled).apply();
        }
    }

    public boolean isGamepadTouchEnabled() {
        return cachedGamepadTouch;
    }

    public void setGamepadTouchEnabled(boolean enabled) {
        if (cachedGamepadTouch != enabled) {
            cachedGamepadTouch = enabled;
            appPrefs.edit().putBoolean(KEY_GAMEPAD_TOUCH, enabled).apply();
        }
    }

    public boolean isGamepadLockEnabled() {
        return cachedGamepadLock;
    }

    public void setGamepadLockEnabled(boolean enabled) {
        if (cachedGamepadLock != enabled) {
            cachedGamepadLock = enabled;
            appPrefs.edit().putBoolean(KEY_GAMEPAD_LOCK, enabled).apply();
        }
    }

    public boolean isGamepadRStickEnabled() {
        return cachedGamepadRStick;
    }

    public void setGamepadRStickEnabled(boolean enabled) {
        if (cachedGamepadRStick != enabled) {
            cachedGamepadRStick = enabled;
            appPrefs.edit().putBoolean(KEY_GAMEPAD_R_STICK, enabled).apply();
        }
    }

    // --- Bluetooth Device Settings ---
    public boolean isRestoreNameEnabled() {
        return cachedRestoreName;
    }

    public void setRestoreNameEnabled(boolean enabled) {
        if (cachedRestoreName != enabled) {
            cachedRestoreName = enabled;
            devicePrefs.edit().putBoolean(KEY_RESTORE_NAME, enabled).apply();
        }
    }
}
