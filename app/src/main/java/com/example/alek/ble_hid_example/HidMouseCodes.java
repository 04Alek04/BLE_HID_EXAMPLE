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

public final class HidMouseCodes {
    private HidMouseCodes() {}

    // Mouse Button Bitmasks
    public static final int BUTTON_LEFT = 0x01;
    public static final int BUTTON_RIGHT = 0x02;
    public static final int BUTTON_MIDDLE = 0x04;
    public static final int BUTTON_BACK = 0x08;
    public static final int BUTTON_FORWARD = 0x10;

    // Absolute Mouse / Touchpad Coordinates
    public static final int ABS_MOUSE_MAX_COORD = 32767;
    public static final int ABS_MOUSE_CENTER_COORD = 16384;
}
