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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_APPEARANCE;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_BATTERY_LEVEL;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_HID_CONTROL_POINT;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_HID_INFORMATION;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_PNP_ID;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_PROTOCOL_MODE;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_REPORT;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_REPORT_MAP;
import static com.example.alek.ble_hid_example.UUIDs.DESC_CCC;
import static com.example.alek.ble_hid_example.UUIDs.DESC_REPORT_REFERENCE;
import static com.example.alek.ble_hid_example.UUIDs.SERVICE_BAS;
import static com.example.alek.ble_hid_example.UUIDs.SERVICE_DIS;
import static com.example.alek.ble_hid_example.UUIDs.SERVICE_HID;

enum SendTo {
    SEND_TO_FIRST,
    SEND_TO_LAST,
    SEND_TO_ALL,
}

enum SecurityLevel {
    SECURITY_LEVEL_1,   // None
    SECURITY_LEVEL_2,   // Encryption
    SECURITY_LEVEL_3,   // Authentication
}

enum ReportField {
    /* Consumer (Report ID 3) */
    REPORT_FIELD_CONSUMER_CONTROL(0, 2),
    REPORT_FIELD_LAUNCHER_BUTTON(2, 2),
    REPORT_FIELD_CONTROL_BUTTON(4, 2),
    /* Keyboard (Report ID 1) */
    REPORT_FIELD_KEYBOARD_META_KEYS(0, 1),
    REPORT_FIELD_KEYBOARD_KEYS(2, 1),
    REPORT_FIELD_KEYBOARD_ALL(0, 2),    // REPORT_FIELD_KEYBOARD_META_KEYS + REPORT_FIELD_KEYBOARD_KEYS
    /* Mouse (Report ID 2) */
    REPORT_FIELD_MOUSE_BUTTONS(0, 1),
    REPORT_FIELD_MOUSE_X(1, 1),
    REPORT_FIELD_MOUSE_Y(2, 1),
    REPORT_FIELD_MOUSE_BUTTONS_XY(0, 3),
    REPORT_FIELD_MOUSE_XY(1, 2),
    REPORT_FIELD_MOUSE_SCROLL(3, 1);

    final static public int REP_CONSUMER = 0x01;
    final static public int REP_MOUSE = 0x02;
    final static public int REP_KEYBOARD = 0x04;
    public final int byte_size;
    public int byte_offset;

    ReportField(int byte_offset, int byte_size) {
        this.byte_offset = byte_offset;
        this.byte_size = byte_size;
    }

    static void updateValues(int features) {
        /* With separate characteristics, each report starts at offset 0 */
        REPORT_FIELD_CONSUMER_CONTROL.byte_offset = 0;
        REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = 2;
        REPORT_FIELD_CONTROL_BUTTON.byte_offset = 4;

        REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset = 0;
        REPORT_FIELD_KEYBOARD_KEYS.byte_offset = 2;
        REPORT_FIELD_KEYBOARD_ALL.byte_offset = 0;

        REPORT_FIELD_MOUSE_BUTTONS.byte_offset = 0;
        REPORT_FIELD_MOUSE_X.byte_offset = 1;
        REPORT_FIELD_MOUSE_Y.byte_offset = 2;
        REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset = 0;
        REPORT_FIELD_MOUSE_XY.byte_offset = 1;
        REPORT_FIELD_MOUSE_SCROLL.byte_offset = 3;
    }
}

class NotificationData {
    public final byte[] value;
    public final BluetoothDevice device;
    public final BluetoothGattCharacteristic characteristic;
    public final boolean responseNeeded;

    NotificationData(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value) {
        this.device = device;
        this.characteristic = characteristic;
        this.value = value;
        this.responseNeeded = false;
    }
}

public class HidBleService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private final List<BluetoothDevice> devices = Collections.synchronizedList(new ArrayList<>());
    private final List<NotificationData> pendingNotifications = Collections.synchronizedList(new ArrayList<>());
    private final List<BluetoothGattService> servicesToAdd = Collections.synchronizedList(new ArrayList<>());
    /* Call proper methods on some BLE events  */
    private MainActivity mainActivity;
    private BluetoothGattServerCallback mGattServerCallback;
    private volatile boolean notificationPossible;
    private BluetoothGattServer gattServer;
    private BluetoothGattCharacteristic charReportKeyboard;
    private BluetoothGattCharacteristic charReportKeyboardOutput;
    private BluetoothGattCharacteristic charReportMouse;
    private BluetoothGattCharacteristic charReportConsumer;
    private String mOriginalName = null;
    private final AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i("BLE", "Advertisement started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.i("BLE", "Advertisement not started - error: " + errorCode);
        }
    };

    private byte readBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        return (byte) batteryLevel;
    }

    public void sendNotification(ReportField rf, int value) {
        sendNotification(rf, value, SendTo.SEND_TO_ALL);
    }

    private void sendNotification(ReportField rf, int value, SendTo st) {
        BluetoothGattCharacteristic report;
        int arraySize;

        if (devices.isEmpty() || gattServer == null) {
            return;
        }

        if (rf.name().contains("KEYBOARD")) {
            report = charReportKeyboard;
            arraySize = 8; // standard 8-byte keyboard report
        } else if (rf.name().contains("MOUSE")) {
            report = charReportMouse;
            arraySize = 4; // buttons + x + y + scroll
        } else if (rf.name().contains("CONSUMER") || rf.name().contains("BUTTON")) {
            report = charReportConsumer;
            arraySize = 6; // consumer control (2) + launcher (2) + control (2)
        } else {
            return;
        }

        if (report == null) return;

        byte[] array = new byte[arraySize];
        Arrays.fill(array, (byte) 0);

        if (rf == ReportField.REPORT_FIELD_KEYBOARD_ALL) {
            array[0] = (byte) (value & 0xff);
            array[2] = (byte) ((value >> 8) & 0xff);
        } else {
            for (int i = rf.byte_offset; i < rf.byte_offset + rf.byte_size; i++) {
                if (i < array.length) {
                    array[i] = (byte) (value & 0xff);
                    value >>= 8;
                }
            }
        }

        synchronized (pendingNotifications) {
            List<BluetoothDevice> targetDevices = new ArrayList<>();
            synchronized (devices) {
                switch (st) {
                    case SEND_TO_FIRST:
                        if (!devices.isEmpty()) targetDevices.add(devices.get(0));
                        break;
                    case SEND_TO_LAST:
                        if (!devices.isEmpty()) targetDevices.add(devices.get(devices.size() - 1));
                        break;
                    case SEND_TO_ALL:
                        targetDevices.addAll(devices);
                        break;
                }
            }

            for (BluetoothDevice device : targetDevices) {
                boolean merged = false;
                if (rf == ReportField.REPORT_FIELD_MOUSE_XY) {
                    // Try to merge with an existing pending mouse move for this device
                    for (int i = pendingNotifications.size() - 1; i >= 0; i--) {
                        NotificationData data = pendingNotifications.get(i);
                        if (data.device.equals(device) && data.characteristic == report) {
                            // Merge X and Y deltas.
                            // X is at offset 1, Y is at offset 2 in the mouse report [buttons, x, y, scroll]
                            int existingX = data.value[1];
                            int existingY = data.value[2];
                            int newX = array[1];
                            int newY = array[2];

                            data.value[1] = (byte) Math.max(-128, Math.min(127, existingX + newX));
                            data.value[2] = (byte) Math.max(-128, Math.min(127, existingY + newY));
                            merged = true;
                            break;
                        }
                    }
                }

                if (!merged) {
                    pendingNotifications.add(new NotificationData(device, report, array));
                }
            }
        }

        processNotificationQueue();
    }

    private synchronized void processNotificationQueue() {
        if (notificationPossible && !pendingNotifications.isEmpty() && gattServer != null) {
            NotificationData toSend = pendingNotifications.remove(0);
            if (devices.contains(toSend.device)) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Log.e("BLE", "Missing BLUETOOTH_CONNECT permission for notification");
                        return;
                    }
                }
                toSend.characteristic.setValue(toSend.value);
                boolean success = gattServer.notifyCharacteristicChanged(toSend.device, toSend.characteristic,
                        toSend.responseNeeded);
                if (success) {
                    notificationPossible = false;
                } else {
                    Log.e("BLE", "Failed to queue notification, will retry on next trigger");
                    // If it failed to queue, we don't set notificationPossible to false,
                    // so we can try the next one or retry this one later.
                    // For now, let's keep it simple.
                }
            } else {
                // Device disconnected, try next one
                processNotificationQueue();
            }
        }
    }

    public void sendNotification(String s) {
        sendNotification(s, SendTo.SEND_TO_ALL);
    }

    private void sendNotification(String s, SendTo st) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            for (int j = 0; j < KeyboardUsage.KEYBOARD_USAGES.length; j++) {
                short val;
                byte m;
                byte k;

                if (Character.toLowerCase(c) == KeyboardUsage.KEYBOARD_USAGES[j].character) {
                    m = KeyboardUsage.KEYBOARD_USAGES[j].meta;
                    k = KeyboardUsage.KEYBOARD_USAGES[j].usage;

                    if (Character.isLetter(c) && Character.isUpperCase(c)) {
                        m |= KeyboardUsage.META_LEFT_SHIFT;
                    }

                    val = (short) (m + (k << 8));
                    sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, (int) val, st);
                    sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0, st);
                    break;
                }
            }
        }

        sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0, st);
    }

    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE", "Missing permissions for advertising");
                return;
            }
        }

        // Configure a short name starting with (HID)
        String originalName = bluetoothAdapter.getName();
        if (mOriginalName == null && originalName != null && !originalName.startsWith("(HID) ")) {
            mOriginalName = originalName;
        }

        if (originalName == null) originalName = "Device";

        String prefix = "(HID) ";
        String newName;
        if (originalName.startsWith(prefix)) {
            newName = originalName;
        } else {
            String shortName = originalName.length() > 10 ? originalName.substring(0, 10) : originalName;
            newName = prefix + shortName;
        }

        if (!newName.equals(originalName)) {
            Log.i("BLE", "Setting device name to: " + newName);
            bluetoothAdapter.setName(newName);
        }

        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            Log.e("BLE", "BluetoothLeAdvertiser is null");
            return;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();

        // Split data to avoid ADVERTISE_FAILED_DATA_TOO_LARGE (error 1)
        AdvertiseData advData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_HID)))
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_DIS)))
                .addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_BAS)))
                .build();

        advertiser.stopAdvertising(advertisingCallback);
        advertiser.startAdvertising(settings, advData, scanResponse, advertisingCallback);
    }

    private void createGattDatabase(BluetoothGattServer gattServer, int level, int features) {
        // Set characteristics' permissions
        final int PERM_READ = (level == SecurityLevel.SECURITY_LEVEL_3.ordinal()) ?
                PERMISSION_READ_ENCRYPTED_MITM : ((level == SecurityLevel.SECURITY_LEVEL_2.ordinal()) ?
                PERMISSION_READ_ENCRYPTED : PERMISSION_READ);
        final int PERM_WRITE = (level == SecurityLevel.SECURITY_LEVEL_3.ordinal()) ?
                PERMISSION_WRITE_ENCRYPTED_MITM : ((level == SecurityLevel.SECURITY_LEVEL_2.ordinal()) ?
                PERMISSION_WRITE_ENCRYPTED : PERMISSION_WRITE);
        final int PERM_READ_WRITE = PERM_READ | PERM_WRITE;

        // Clear current attributes database - avoid adding the same services to database
        gattServer.clearServices();

        ////////////////////////////////////////////////////////////////////////////////////////////
        // HID Service
        //           -> Report Map
        //           -> HID Information
        //           -> HID Control Point
        //           -> Report
        //                   -> Client Characteristic Configuration
        //                   -> Report Reference
        //           ... (another Report characteristics and their descriptors)
        ////////////////////////////////////////////////////////////////////////////////////////////
        byte REPORT_MAP[] = new byte[]{
                // --- CONSUMER (ID 3) - 6 bytes ---
                (byte) 0x05, (byte) 0x0C, /* Usage Page (Consumer Devices) */
                (byte) 0x09, (byte) 0x01, /* Usage (Consumer Control) */
                (byte) 0xA1, (byte) 0x01, /* Collection (Application) */
                (byte) 0x85, (byte) 0x03, /* Report ID (3) */

                // Block 1: Consumer Page (11 bits)
                (byte) 0x05, (byte) 0x0C, /* Usage Page (Consumer Devices) */
                (byte) 0x15, (byte) 0x00, /* Logical Minimum (0) */
                (byte) 0x25, (byte) 0x01, /* Logical Maximum (1) */
                (byte) 0x75, (byte) 0x01, /* Report Size (1) */
                (byte) 0x95, (byte) 0x0B, /* Report Count (11) */
                (byte) 0x09, 0x6F, /* Bright Up */
                (byte) 0x09, 0x70, /* Bright Down */
                (byte) 0x09, (byte) 0xB5, /* Scan Next Track */
                (byte) 0x09, (byte) 0xB6, /* Scan Previous Track */
                (byte) 0x09, (byte) 0xB7, /* Stop */
                (byte) 0x09, (byte) 0xCD, /* Play / Pause */
                (byte) 0x09, (byte) 0xE2, /* Mute */
                (byte) 0x09, (byte) 0xE9, /* Volume Up */
                (byte) 0x09, (byte) 0xEA, /* Volume Down */
                (byte) 0x09, (byte) 0xB8, /* Eject */
                (byte) 0x09, (byte) 0x65, /* Snapshot */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Variable, Absolute) */

                // Block 2: Generic Desktop Page (5 bits)
                (byte) 0x05, (byte) 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x95, (byte) 0x05, /* Report Count (5) */
                (byte) 0x09, (byte) 0x82, /* System Sleep */
                (byte) 0x09, (byte) 0xA8, /* System Hibernate */
                (byte) 0x09, (byte) 0x81, /* System Power Down */
                (byte) 0x09, (byte) 0x8E, /* System Cold Restart */
                (byte) 0x09, (byte) 0x8F, /* System Warm Restart */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Variable, Absolute) */

                // Block 3: Launchers (16 bits)
                (byte) 0x05, (byte) 0x0C, /* Usage Page (Consumer Devices) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x75, (byte) 0x10, /* Report Size (16) */
                (byte) 0x16, (byte) 0x81, (byte) 0x01, /* Logical Minimum (385) */
                (byte) 0x26, (byte) 0xC7, (byte) 0x01, /* Logical Maximum (455) */
                (byte) 0x1A, (byte) 0x81, (byte) 0x01, /* Usage Minimum (385) */
                (byte) 0x2A, (byte) 0xC7, (byte) 0x01, /* Usage Maximum (455) */
                (byte) 0x81, (byte) 0x00, /* Input (Data, Array) */

                // Block 4: Controls (16 bits)
                (byte) 0x05, (byte) 0x0C, /* Usage Page (Consumer Devices) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x75, (byte) 0x10, /* Report Size (16) */
                (byte) 0x16, (byte) 0x01, (byte) 0x02, /* Logical Minimum (513) */
                (byte) 0x26, (byte) 0x9C, (byte) 0x02, /* Logical Maximum (668) */
                (byte) 0x1A, (byte) 0x01, (byte) 0x02, /* Usage Minimum (513) */
                (byte) 0x2A, (byte) 0x9C, (byte) 0x02, /* Usage Maximum (668) */
                (byte) 0x81, (byte) 0x00, /* Input (Data, Array) */
                (byte) 0xC0, /* End Collection */

                // --- KEYBOARD (ID 1) - 8 bytes standard ---
                (byte) 0x05, (byte) 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, (byte) 0x06, /* Usage (Keyboard) */
                (byte) 0xA1, (byte) 0x01, /* Collection (Application) */
                (byte) 0x85, (byte) 0x01, /* Report ID (1) */
                (byte) 0x05, (byte) 0x07, /* Usage Page (Keyboard/Keypad) */
                (byte) 0x19, (byte) 0xE0, /* Usage Minimum (Left Control) */
                (byte) 0x29, (byte) 0xE7, /* Usage Maximum (Right GUI) */
                (byte) 0x15, (byte) 0x00, /* Logical Minimum (0) */
                (byte) 0x25, (byte) 0x01, /* Logical Maximum (1) */
                (byte) 0x75, (byte) 0x01, /* Report Size (1) */
                (byte) 0x95, (byte) 0x08, /* Report Count (8) */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Variable, Absolute) - Modifiers */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x75, (byte) 0x08, /* Report Size (8) */
                (byte) 0x81, (byte) 0x01, /* Input (Constant) - Reserved byte */
                (byte) 0x95, (byte) 0x06, /* Report Count (6) */
                (byte) 0x75, (byte) 0x08, /* Report Size (8) */
                (byte) 0x15, (byte) 0x00, /* Logical Minimum (0) */
                (byte) 0x25, (byte) 0xDF, /* Logical Maximum (223) */
                (byte) 0x05, (byte) 0x07, /* Usage Page (Keyboard/Keypad) */
                (byte) 0x19, (byte) 0x00, /* Usage Minimum (0) */
                (byte) 0x29, (byte) 0xDF, /* Usage Maximum (223) */
                (byte) 0x81, (byte) 0x00, /* Input (Data, Array) - Keys */
                (byte) 0x95, (byte) 0x05, /* Report Count (5) */
                (byte) 0x75, (byte) 0x01, /* Report Size (1) */
                (byte) 0x05, (byte) 0x08, /* Usage Page (LEDs) */
                (byte) 0x19, (byte) 0x01, /* Usage Minimum (Num Lock) */
                (byte) 0x29, (byte) 0x05, /* Usage Maximum (Kana) */
                (byte) 0x91, (byte) 0x02, /* Output (Data, Var, Abs) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x75, (byte) 0x03, /* Report Size (3) */
                (byte) 0x91, (byte) 0x03, /* Output (Const, Var, Abs) */
                (byte) 0xC0, /* End Collection */

                // --- MOUSE (ID 2) ---
                (byte) 0x05, (byte) 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, (byte) 0x02, /* Usage (Mouse) */
                (byte) 0xA1, (byte) 0x01, /* Collection (Application) */
                (byte) 0x85, (byte) 0x02, /* Report ID (2) */
                (byte) 0x09, (byte) 0x01, /* Usage (Pointer) */
                (byte) 0xA1, (byte) 0x00, /* Collection (Physical) */
                (byte) 0x05, (byte) 0x09, /* Usage Page (Button) */
                (byte) 0x19, (byte) 0x01, /* Usage Minimum (Button 1) */
                (byte) 0x29, (byte) 0x05, /* Usage Maximum (Button 5) */
                (byte) 0x15, (byte) 0x00, /* Logical Minimum (0) */
                (byte) 0x25, (byte) 0x01, /* Logical Maximum (1) */
                (byte) 0x95, (byte) 0x05, /* Report Count (5) */
                (byte) 0x75, (byte) 0x01, /* Report Size (1) */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Variable, Absolute) - Buttons */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x75, (byte) 0x03, /* Report Size (3) */
                (byte) 0x81, (byte) 0x01, /* Input (Constant) - Padding */
                (byte) 0x05, (byte) 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, (byte) 0x30, /* Usage (X) */
                (byte) 0x09, (byte) 0x31, /* Usage (Y) */
                (byte) 0x15, (byte) 0x80, /* Logical Minimum (-128) */
                (byte) 0x25, (byte) 0x7F, /* Logical Maximum (127) */
                (byte) 0x75, (byte) 0x08, /* Report Size (8) */
                (byte) 0x95, (byte) 0x02, /* Report Count (2) */
                (byte) 0x81, (byte) 0x06, /* Input (Data, Variable, Relative) - X, Y */
                (byte) 0x09, (byte) 0x38, /* Usage (Wheel) */
                (byte) 0x15, (byte) 0x80, /* Logical Minimum (-128) */
                (byte) 0x25, (byte) 0x7F, /* Logical Maximum (127) */
                (byte) 0x75, (byte) 0x08, /* Report Size (8) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x81, (byte) 0x06, /* Input (Data, Variable, Relative) - Wheel */
                (byte) 0xC0, (byte) 0xC0 /* End Collections */
        };


        // HID Service
        BluetoothGattService serviceHid = new BluetoothGattService(UUID.fromString(SERVICE_HID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Report Map characteristic
        BluetoothGattCharacteristic charReportMap = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT_MAP),
                BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ);

        charReportMap.setValue(REPORT_MAP);

        // HID Information characteristic
        BluetoothGattCharacteristic charHidInformation = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_HID_INFORMATION),
                BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ);

        charHidInformation.setValue(new byte[]{0x01, 0x11, 0x00, 0x03});

        // HID Control Point characteristic
        BluetoothGattCharacteristic charHidControlPoint = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_HID_CONTROL_POINT),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, PERM_WRITE);

        charHidControlPoint.setValue("\0".getBytes());

        // Protocol Mode characteristic
        BluetoothGattCharacteristic charProtocolMode = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_PROTOCOL_MODE),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                PERM_READ_WRITE);
        charProtocolMode.setValue(new byte[]{0x01}); // 0x01 = Report Protocol Mode

        // Report Keyboard characteristic (ID 1)
        charReportKeyboard = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);
        charReportKeyboard.setValue(new byte[8]);
        BluetoothGattDescriptor descReportRefKbd = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefKbd.setValue(new byte[]{0x01, 0x01}); // ID=1, Type=Input
        charReportKeyboard.addDescriptor(descReportRefKbd);
        charReportKeyboard.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));

        // Report Keyboard Output characteristic (ID 1)
        charReportKeyboardOutput = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                PERM_READ_WRITE);
        charReportKeyboardOutput.setValue(new byte[1]);
        BluetoothGattDescriptor descReportRefKbdOut = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefKbdOut.setValue(new byte[]{0x01, 0x02}); // ID=1, Type=Output
        charReportKeyboardOutput.addDescriptor(descReportRefKbdOut);

        // Report Mouse characteristic (ID 2)
        charReportMouse = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);
        charReportMouse.setValue(new byte[4]);
        BluetoothGattDescriptor descReportRefMouse = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefMouse.setValue(new byte[]{0x02, 0x01}); // ID=2, Type=Input
        charReportMouse.addDescriptor(descReportRefMouse);
        charReportMouse.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));

        // Report Consumer characteristic (ID 3)
        charReportConsumer = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);
        charReportConsumer.setValue(new byte[6]);
        BluetoothGattDescriptor descReportRefConsumer = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefConsumer.setValue(new byte[]{0x03, 0x01}); // ID=3, Type=Input
        charReportConsumer.addDescriptor(descReportRefConsumer);
        charReportConsumer.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));

        serviceHid.addCharacteristic(charReportMap);
        serviceHid.addCharacteristic(charHidInformation);
        serviceHid.addCharacteristic(charHidControlPoint);
        serviceHid.addCharacteristic(charProtocolMode);
        serviceHid.addCharacteristic(charReportKeyboard);
        serviceHid.addCharacteristic(charReportKeyboardOutput);
        serviceHid.addCharacteristic(charReportMouse);
        serviceHid.addCharacteristic(charReportConsumer);
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Device Information Service
        //                          -> PnP ID
        ////////////////////////////////////////////////////////////////////////////////////////////
        BluetoothGattService serviceDIS = new BluetoothGattService(UUID.fromString(SERVICE_DIS),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // PnP ID characteristic
        BluetoothGattCharacteristic charPnpId = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_PNP_ID),
                BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ);

        charPnpId.setValue("\2\0\0\0\0\0\0".getBytes());
        serviceDIS.addCharacteristic(charPnpId);

        // Appearance characteristic (Generic HID = 0x03C0)
        // NOTE: According to Bluetooth spec, this should be in the Generic Access Service (0x1800).
        // However, on Android, the 0x1800 service is restricted and adding it manually often fails.
        // We include it here in the Device Information Service as a fallback so hosts can still discover it.
        BluetoothGattCharacteristic charApp = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_APPEARANCE),
                BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ);
        charApp.setValue(new byte[]{(byte) 0xC0, (byte) 0x03});
        serviceDIS.addCharacteristic(charApp);
        
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Battery Service
        //               -> Battery Level
        ////////////////////////////////////////////////////////////////////////////////////////////
        BluetoothGattService serviceBAS = new BluetoothGattService(UUID.fromString(SERVICE_BAS),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // PBattery Level characteristic
        BluetoothGattCharacteristic charBatteryLevel = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_BATTERY_LEVEL),
                BluetoothGattCharacteristic.PROPERTY_READ, PERM_READ);

        charBatteryLevel.setValue(new byte[]{readBatteryLevel()});
        serviceBAS.addCharacteristic(charBatteryLevel);

        servicesToAdd.clear();
        servicesToAdd.add(serviceHid);
        servicesToAdd.add(serviceDIS);
        servicesToAdd.add(serviceBAS);

        gattServer.clearServices();
        if (!servicesToAdd.isEmpty()) {
            gattServer.addService(servicesToAdd.remove(0));
        }
    }

    private void gattServerCbInit() {
        mGattServerCallback = new BluetoothGattServerCallback() {
            private final int DEFAULT_MTU = 23;
            private int currentMtu = DEFAULT_MTU;

            @Override
            public void onConnectionStateChange(final BluetoothDevice device, int status, int newState) {
                Log.e("BLE", "onConnectionStateChange " + device.toString() + " " + status + " " + newState);

                if (newState == STATE_CONNECTED) {
                    currentMtu = DEFAULT_MTU;

                    if (!devices.contains(device)) {
                        devices.add(device);
                    }

                    if (devices.size() == 1) {
                        notificationPossible = true;
                    }
                } else if (newState == STATE_DISCONNECTED) {
                    if (devices.contains(device)) {
                        // If some notification waiting for this device then they will be removed in
                        // proper task
                        devices.remove(device);
                    }

                    if (devices.size() == 0) {
                        notificationPossible = false;
                    }
                }

                if (mainActivity != null) {
                    mainActivity.onConnectionStateChange(devices);
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                    BluetoothGattCharacteristic characteristic) {
                if (gattServer == null) return;
                
                if (Objects.equals(characteristic.getUuid().toString(), CHAR_BATTERY_LEVEL)) {
                    characteristic.setValue(new byte[]{readBatteryLevel()});
                }

                byte[] value = characteristic.getValue();
                if (value == null) value = new byte[0];

                int charLength = value.length;
                int partLength = Math.max(0, Math.min(charLength - offset, currentMtu));
                byte[] bytes = new byte[partLength];

                if (partLength > 0) {
                    System.arraycopy(value, offset, bytes, 0, partLength);
                }

                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, bytes);

                Log.e("BLE", "onCharacteristicReadRequest " + characteristic.getUuid().toString() +
                        " offset " + offset);
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                currentMtu = mtu;
            }

            @Override
            public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId,
                                                 BluetoothGattDescriptor descriptor,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
                if (gattServer == null) return;
                Log.e("BLE", "onDescriptorWriteRequest " + descriptor.getUuid().toString() + " value " + Arrays.toString(value));
                descriptor.setValue(value);
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                     BluetoothGattCharacteristic characteristic,
                                                     boolean preparedWrite, boolean responseNeeded,
                                                     int offset, byte[] value) {
                if (gattServer == null) return;
                Log.i("BLE", "onCharacteristicWriteRequest " + characteristic.getUuid().toString() +
                        " value: " + (value != null ? Arrays.toString(value) : "null"));
                characteristic.setValue(value);

                if (value != null && value.length > 0) {
                    // Check if this is the Keyboard Output Report (LEDs)
                    // We check by reference AND by characteristic properties as a fallback
                    if (characteristic == charReportKeyboardOutput || 
                        (characteristic.getUuid().toString().equals(CHAR_REPORT) && 
                         (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)) {
                        Log.i("BLE", "Keyboard LED status received: " + value[0]);
                        if (mainActivity != null) {
                            mainActivity.onKeyboardLedStatusChanged(value[0]);
                        }
                    }
                }

                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattDescriptor descriptor) {
                if (gattServer == null) return;
                Log.e("BLE", "onDescriptorReadRequest " + descriptor.getUuid().toString());
                byte[] value = descriptor.getValue();
                if (value == null) value = new byte[0];
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                notificationPossible = true;
                processNotificationQueue();
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.i("BLE", "onServiceAdded: " + service.getUuid().toString() + " status: " + status);
                if (!servicesToAdd.isEmpty()) {
                    gattServer.addService(servicesToAdd.remove(0));
                } else {
                    startAdvertising();
                }
            }
        };
    }

    public List<BluetoothDevice> getDevices() {
        return devices;
    }

    public void setActivity(MainActivity ma) {
        mainActivity = ma;
    }

    /* Service handling-related methods */
    public void initializeLE() {
        BluetoothManager mManager;
        int features = 0;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e("BLE", "Bluetooth is not enabled or not supported");
            return;
        }

        if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                ApplicationConfiguration.CONSUMER_FEAT)) {
            features |= ReportField.REP_CONSUMER;
        }

        if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                ApplicationConfiguration.MOUSE_FEAT)) {
            features |= ReportField.REP_MOUSE;
        }

        if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                ApplicationConfiguration.KEYBOARD_FEAT)) {
            features |= ReportField.REP_KEYBOARD;
        }

        ReportField.updateValues(features);
        gattServerCbInit();
        mManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (gattServer != null) {
            gattServer.close();
        }
        gattServer = mManager.openGattServer(this, mGattServerCallback);
        createGattDatabase(gattServer, SecurityLevel.SECURITY_LEVEL_2.ordinal(), features);
        pendingNotifications.clear();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initializeLE();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mOriginalName != null) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                Log.i("BLE", "Restoring original device name: " + mOriginalName);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.setName(mOriginalName);
                    }
                } else {
                    bluetoothAdapter.setName(mOriginalName);
                }
            }
        }

        if (gattServer != null) {
            gattServer.close();
            gattServer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mainActivity = null;
        return false;
    }

    public class LocalBinder extends Binder {
        HidBleService getService() {
            return HidBleService.this;
        }
    }
}
