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

public final class HidKeyCodes {
    private HidKeyCodes() {}

    // Keyboard LED Status Bitmasks
    public static final byte LED_NUM_LOCK = 0x01;
    public static final byte LED_CAPS_LOCK = 0x02;
    public static final byte LED_SCROLL_LOCK = 0x04;

    // Keyboard Usage IDs
    public static final byte USAGE_NUM_LOCK = 0x53;
    public static final byte USAGE_CAPS_LOCK = 0x39;
    public static final byte USAGE_SCROLL_LOCK = 0x47;
    public static final byte USAGE_ENTER = 0x28;
    public static final byte USAGE_BACKSPACE = 0x2A;
    public static final byte USAGE_TAB = 0x2B;

    // Shortcut Usage IDs (CTRL +)
    public static final byte USAGE_UNDO = 0x1D;       // Z
    public static final byte USAGE_REDO = 0x1C;       // Y
    public static final byte USAGE_SAVE = 0x16;       // S
    public static final byte USAGE_SELECT_ALL = 0x04; // A
    public static final byte USAGE_CUT = 0x1B;        // X
    public static final byte USAGE_COPY = 0x06;       // C
    public static final byte USAGE_PASTE = 0x19;      // V
    public static final byte USAGE_FIND = 0x09;       // F
    public static final byte USAGE_REPLACE = 0x0B;    // H
}
