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
final class UUIDs {
    // HID related UUIDs
    public static final String SERVICE_HID = "00001812-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_REPORT = "00002A4D-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_REPORT_MAP = "00002A4B-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_HID_INFORMATION = "00002A4A-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_HID_CONTROL_POINT = "00002A4C-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_PROTOCOL_MODE = "00002A4E-0000-1000-8000-00805F9B34FB";
    public static final String DESC_REPORT_REFERENCE = "00002908-0000-1000-8000-00805F9B34FB";
    public static final String DESC_CCC = "00002902-0000-1000-8000-00805F9B34FB";

    // DIS related UUIDs
    public static final String SERVICE_DIS = "0000180A-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_PNP_ID = "00002A50-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_APPEARANCE = "00002A01-0000-1000-8000-00805F9B34FB";

    // BAS related UUIDs
    public static final String SERVICE_BAS = "0000180F-0000-1000-8000-00805F9B34FB";
    public static final String CHAR_BATTERY_LEVEL = "00002A19-0000-1000-8000-00805F9B34FB";

    private UUIDs() {
    }
}