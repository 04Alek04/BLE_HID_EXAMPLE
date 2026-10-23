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

public final class HidGamepadCodes {
    private HidGamepadCodes() {}

    // Gamepad Button Bitmasks
    public static final int BTN_A = 0x001;
    public static final int BTN_B = 0x002;
    public static final int BTN_X = 0x004;
    public static final int BTN_Y = 0x008;
    public static final int BTN_LB = 0x010;
    public static final int BTN_RB = 0x020;
    public static final int BTN_LT = 0x040;
    public static final int BTN_RT = 0x080;
    public static final int BTN_SELECT = 0x100;
    public static final int BTN_START = 0x200;
    public static final int BTN_L3 = 0x400;
    public static final int BTN_R3 = 0x800;

    // Analog Stick Range Limits
    public static final int STICK_AXIS_MIN = -127;
    public static final int STICK_AXIS_MAX = 127;
    public static final int STICK_AXIS_CENTER = 0;
}
