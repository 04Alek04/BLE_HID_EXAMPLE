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

public final class HidConstants {
    private HidConstants() {}

    // BLE Handshake, Watchdog & Queue Timers (ms)
    public static final long GATT_HANDSHAKE_DEBOUNCE_MS = 300L;
    public static final long QUEUE_WATCHDOG_TIMEOUT_MS = 100L;
    public static final int KEY_HOLD_TIME_STANDARD_MS = 10;
    public static final int KEY_HOLD_TIME_SHIFTED_MS = 15;
    public static final int KEY_RELEASE_DELAY_STANDARD_MS = 10;
    public static final int KEY_RELEASE_DELAY_SHIFTED_MS = 25;
    public static final int ENTER_KEY_DELAY_MS = 20;
    public static final int MAX_PENDING_QUEUE_SIZE = 6;

    // BLE Advertising Constants
    public static final int MAX_ADV_NAME_LENGTH = 18;
    public static final int MAX_NAME_RETRY_COUNT = 5;
    public static final long ADV_NAME_RETRY_DELAY_MS = 300L;

    // UI Refresh & Gesture Timers (ms)
    public static final long DEVICE_FIELD_REFRESH_MS = 1200L;
    public static final long DEVICE_LAYOUT_REFRESH_MS = 300L;
    public static final long TRIPLE_TAP_WINDOW_MS = 500L;
    public static final long BLOCKLY_CLICK_HOLD_MS = 50L;
}
