/*
 * Copyright 2018-2019 Aleksander Drewnicki
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
import static com.example.alek.ble_hid_example.UUIDs.CHAR_BATTERY_LEVEL;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_HID_CONTROL_POINT;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_HID_INFORMATION;
import static com.example.alek.ble_hid_example.UUIDs.CHAR_PNP_ID;
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
    /* Consumer */
    REPORT_FIELD_CONSUMER_CONTROL(0, 2),
    REPORT_FIELD_LAUNCHER_BUTTON(2, 2),
    REPORT_FIELD_CONTROL_BUTTON(4, 2),
    /* Keyboard */
    REPORT_FIELD_KEYBOARD_META_KEYS(6, 1),
    REPORT_FIELD_KEYBOARD_KEYS(7, 1),
    REPORT_FIELD_KEYBOARD_ALL(6, 2),    // REPORT_FIELD_KEYBOARD_META_KEYS + REPORT_FIELD_KEYBOARD_KEYS
    /* Mouse */
    REPORT_FIELD_MOUSE_BUTTONS(8, 1),
    REPORT_FIELD_MOUSE_X(9, 1),
    REPORT_FIELD_MOUSE_Y(10, 1),
    REPORT_FIELD_MOUSE_BUTTONS_XY(8, 3),
    REPORT_FIELD_MOUSE_XY(9, 2),
    REPORT_FIELD_MOUSE_SCROLL(11, 1);

    final static public int REP_CONSUMER = 0x01;
    final static public int REP_MOUSE = 0x02;
    final static public int REP_KEYBOARD = 0x04;
    final static public int REP_BASIC = 0x08;
    public final int byte_size;
    public int byte_offset;

    ReportField(int byte_offset, int byte_size) {
        this.byte_offset = byte_offset;
        this.byte_size = byte_size;
    }

    static void updateValues(int features) {
        /* Restore original value */
        REPORT_FIELD_CONSUMER_CONTROL.byte_offset = 0;
        REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = 2;
        REPORT_FIELD_CONTROL_BUTTON.byte_offset = 4;

        REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset = 6;
        REPORT_FIELD_KEYBOARD_KEYS.byte_offset = 7;
        REPORT_FIELD_KEYBOARD_ALL.byte_offset = 6;

        REPORT_FIELD_MOUSE_BUTTONS.byte_offset = 8;
        REPORT_FIELD_MOUSE_X.byte_offset = 9;
        REPORT_FIELD_MOUSE_Y.byte_offset = 10;
        REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset = 8;
        REPORT_FIELD_MOUSE_XY.byte_offset = 9;
        REPORT_FIELD_MOUSE_SCROLL.byte_offset = 11;

        if ((features & REP_CONSUMER) == 0) {
            REPORT_FIELD_CONSUMER_CONTROL.byte_offset = -1;
            REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = -1;
            REPORT_FIELD_CONTROL_BUTTON.byte_offset = -1;

            REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset -= 6;
            REPORT_FIELD_KEYBOARD_KEYS.byte_offset -= 6;
            REPORT_FIELD_KEYBOARD_ALL.byte_offset -= 6;

            REPORT_FIELD_MOUSE_BUTTONS.byte_offset -= 6;
            REPORT_FIELD_MOUSE_X.byte_offset -= 6;
            REPORT_FIELD_MOUSE_Y.byte_offset -= 6;
            REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset -= 6;
            REPORT_FIELD_MOUSE_XY.byte_offset -= 6;
            REPORT_FIELD_MOUSE_SCROLL.byte_offset -= 6;
        } else if ((features & REP_BASIC) == REP_BASIC) {
            REPORT_FIELD_LAUNCHER_BUTTON.byte_offset = -1;
            REPORT_FIELD_CONTROL_BUTTON.byte_offset = -1;

            REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset -= 4;
            REPORT_FIELD_KEYBOARD_KEYS.byte_offset -= 4;
            REPORT_FIELD_KEYBOARD_ALL.byte_offset -= 4;

            REPORT_FIELD_MOUSE_BUTTONS.byte_offset -= 4;
            REPORT_FIELD_MOUSE_X.byte_offset -= 4;
            REPORT_FIELD_MOUSE_Y.byte_offset -= 4;
            REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset -= 4;
            REPORT_FIELD_MOUSE_XY.byte_offset -= 4;
            REPORT_FIELD_MOUSE_SCROLL.byte_offset -= 4;
        }

        if ((features & REP_KEYBOARD) == 0) {
            REPORT_FIELD_KEYBOARD_META_KEYS.byte_offset = -1;
            REPORT_FIELD_KEYBOARD_KEYS.byte_offset = -1;
            REPORT_FIELD_KEYBOARD_ALL.byte_offset = -1;

            REPORT_FIELD_MOUSE_BUTTONS.byte_offset -= 2;
            REPORT_FIELD_MOUSE_X.byte_offset -= 2;
            REPORT_FIELD_MOUSE_Y.byte_offset -= 2;
            REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset -= 2;
            REPORT_FIELD_MOUSE_XY.byte_offset -= 2;
            REPORT_FIELD_MOUSE_SCROLL.byte_offset -= 2;
        }

        if ((features & REP_MOUSE) == 0) {
            REPORT_FIELD_MOUSE_BUTTONS.byte_offset = -1;
            REPORT_FIELD_MOUSE_X.byte_offset = -1;
            REPORT_FIELD_MOUSE_Y.byte_offset = -1;
            REPORT_FIELD_MOUSE_BUTTONS_XY.byte_offset = -1;
            REPORT_FIELD_MOUSE_XY.byte_offset = -1;
            REPORT_FIELD_MOUSE_SCROLL.byte_offset = -1;
        }
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
    private final ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private final ArrayList<NotificationData> pendingNotifications = new ArrayList<>();
    private final ArrayList<BluetoothGattService> servicesToAdd = new ArrayList<>();
    /* Call proper methods on some BLE events  */
    private MainActivity mainActivity;
    private BluetoothGattServerCallback mGattServerCallback;
    private boolean notificationPossible;
    private BluetoothGattServer gattServer;
    private Thread check_thread = null;
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
        // Assume one report characteristic or that the notification should be sent from the first
        BluetoothGattCharacteristic report;
        byte[] array = new byte[rf.byte_offset + rf.byte_size];

        if (devices.size() == 0) {
            return;
        }

        if (rf.byte_offset < 0) {
            return;
        }

        report = gattServer.getService(UUID.fromString(UUIDs.SERVICE_HID)).getCharacteristic(
                UUID.fromString(UUIDs.CHAR_REPORT));

        Arrays.fill(array, (byte) 0);

        for (int i = rf.byte_offset; i < rf.byte_offset + rf.byte_size; i++) {
            array[i] = (byte) (value & 0xff);
            value >>= 8;
        }

        switch (st) {
            case SEND_TO_FIRST:
                pendingNotifications.add(new NotificationData(devices.get(0), report, array));
                break;
            case SEND_TO_LAST:
                pendingNotifications.add(new NotificationData(devices.get(devices.size() - 1),
                        report, array));
                break;
            case SEND_TO_ALL:
                for (int i = 0; i < devices.size(); i++) {
                    pendingNotifications.add(new NotificationData(devices.get(i), report, array
                    ));
                }
                break;
        }

        if (notificationPossible) {
            NotificationData toSend = pendingNotifications.remove(0);
            toSend.characteristic.setValue(toSend.value);
            gattServer.notifyCharacteristicChanged(toSend.device, toSend.characteristic,
                    toSend.responseNeeded);
            notificationPossible = false;
            return;
        }

        if (check_thread != null) {
            return;
        }

        check_thread = new Thread() {
            public void run() {
                NotificationData toSend;

                while (!notificationPossible) {
                    if (devices.size() == 0) {
                        pendingNotifications.clear();
                        return;
                    }
                }

                if (pendingNotifications.size() < 1 || devices.size() == 0) {
                    check_thread = null;
                    return;
                }

                toSend = pendingNotifications.remove(0);

                if (devices.contains(toSend.device)) {
                    /* Device could disconnect in the mean time */
                    toSend.characteristic.setValue(toSend.value);
                    gattServer.notifyCharacteristicChanged(toSend.device, toSend.characteristic,
                            toSend.responseNeeded);
                }

                notificationPossible = false;
            }
        };
        check_thread.start();
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

        String currentName = bluetoothAdapter.getName();
        if (currentName != null && !currentName.endsWith(" (HID)")) {
            bluetoothAdapter.setName(currentName + " (HID)");
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
        // Report Map - for basic mode
        final byte REPORT_MAP_BASIC[] = {
                (byte) 0x05, (byte) 0x0C, /*        Usage Page (Consumer Devices)       */
                (byte) 0x09, (byte) 0x01, /*        Usage (Consumer Control)            */
                (byte) 0xA1, (byte) 0x01, /*        Collection (Application)            */
                (byte) 0x85, (byte) 0x02, /*        Report ID=2                         */
                (byte) 0x05, (byte) 0x0C, /*        Usage Page (Consumer Devices)       */
                (byte) 0x15, (byte) 0x00, /*        Logical Minimum (0)                 */
                (byte) 0x25, (byte) 0x01, /*        Logical Maximum (1)                 */
                (byte) 0x75, (byte) 0x01, /*        Report Size (1)                     */
                (byte) 0x95, (byte) 0x0B, /*        Report Count (11)                   */

                (byte) 0x09, (byte) 0x6F, /* 1       Usage (Bright Up)                  */
                (byte) 0x09, (byte) 0x70, /* 2       Usage (Bright Down)                */

                (byte) 0x09, (byte) 0xB5, /* 3       Usage (Scan Next Track)            */
                (byte) 0x09, (byte) 0xB6, /* 4       Usage (Scan Previous Track)        */
                (byte) 0x09, (byte) 0xB7, /* 5       Usage (Stop)                       */
                (byte) 0x09, (byte) 0xCD, /* 6       Usage (Play / Pause)               */

                (byte) 0x09, (byte) 0xE2, /* 7       Usage (Mute)                       */
                (byte) 0x09, (byte) 0xE9, /* 8       Usage (Volume Up)                  */
                (byte) 0x09, (byte) 0xEA, /* 9       Usage (Volume Down)                */

                (byte) 0x09, (byte) 0xB8, /* 10      Usage (Eject)                      */
                (byte) 0x09, (byte) 0xb8, /* 11      Usage (Snapshot)                   */

                (byte) 0x81, (byte) 0x02, /*        Input (Data, Variable, Absolute)    */
                (byte) 0x95, (byte) 0x05, /*        Report Count (5)                    */
                (byte) 0x81, (byte) 0x01, /*        Input (Constant)                    */
                (byte) 0xC0,
        };

        // Report Map - concatenation of few arrays
        final byte REPORT_MAP_START[] = {
                (byte) 0x05, (byte) 0x0C, /*        Usage Page (Consumer Devices)       */
                (byte) 0x09, (byte) 0x01, /*        Usage (Consumer Control)            */
                (byte) 0xA1, (byte) 0x01, /*        Collection (Application)            */
                (byte) 0x85, (byte) 0x02, /*        Report ID=2                         */
        };
        final byte REPORT_MAP_CONSUMER[] =
                ((features & ReportField.REP_CONSUMER) == ReportField.REP_CONSUMER) ? new byte[]{
                /*========================== Consumer control ==========================*/
                        (byte) 0xA1, (byte) 0x02, /*        Collection (Logical)            */
                        (byte) 0x05, (byte) 0x0C, /*        Usage Page (Consumer Devices)       */
                        (byte) 0x15, (byte) 0x00, /*        Logical Minimum (0)                 */
                        (byte) 0x25, (byte) 0x01, /*        Logical Maximum (1)                 */
                        (byte) 0x75, (byte) 0x01, /*        Report Size (1)                     */
                        (byte) 0x95, (byte) 0x10, /*        Report Count (16)                   */

                        (byte) 0x09, (byte) 0x6F, /* 1       Usage (Bright Up)                  */
                        (byte) 0x09, (byte) 0x70, /* 2       Usage (Bright Down)                */

                        (byte) 0x09, (byte) 0xB5, /* 3       Usage (Scan Next Track)            */
                        (byte) 0x09, (byte) 0xB6, /* 4       Usage (Scan Previous Track)        */
                        (byte) 0x09, (byte) 0xB7, /* 5       Usage (Stop)                       */
                        (byte) 0x09, (byte) 0xCD, /* 6       Usage (Play / Pause)               */

                        (byte) 0x09, (byte) 0xE2, /* 7       Usage (Mute)                       */
                        (byte) 0x09, (byte) 0xE9, /* 8       Usage (Volume Up)                  */
                        (byte) 0x09, (byte) 0xEA, /* 9       Usage (Volume Down)                */

                        (byte) 0x09, (byte) 0xB8, /* 10      Usage (Eject)                      */
                        (byte) 0x09, (byte) 0x65, /* 11      Usage (Snapshot)                   */

                        (byte) 0x05, (byte) 0x01, /*        Usage Page (Generic Desktop)        */
                        (byte) 0x09, (byte) 0x82, /* 12      Usage (System Sleep)               */
                        (byte) 0x09, (byte) 0xA8, /* 13      Usage (System Hibernate)           */
                        (byte) 0x09, (byte) 0x81, /* 14      Usage (System Power Down)          */
                        (byte) 0x09, (byte) 0x8E, /* 15      Usage (System Cold Restart)        */
                        (byte) 0x09, (byte) 0x8F, /* 16      Usage (System Warm Restart)        */

                        (byte) 0x81, (byte) 0x02, /*        Input (Data, Variable, Absolute)    */
//                (byte) 0x95, (byte) 0x00, /*        Report Count (1)                    */
//                (byte) 0x81, (byte) 0x01, /*        Input (Constant)                    */

                /*==================== Application Launcher Buttons ====================*/
                        (byte) 0x05, (byte) 0x0C,              /* Usage Page (Consumer Devices) */
                        (byte) 0x95, (byte) 0x01,              /* Report Count (1)              */
                        (byte) 0x75, (byte) 0x10,              /* Report Size (16)              */
                        (byte) 0x16, (byte) 0x81, (byte) 0x01, /* Logical Minimum (385)         */
                        (byte) 0x26, (byte) 0xC7, (byte) 0x01, /* Logical Maximum (455)         */
                        (byte) 0x05, (byte) 0x0C,              /* Usage Page (Consumer Devices) */
                        (byte) 0x1a, (byte) 0x81, (byte) 0x01, /* Usage Minimum (385)           */
                        (byte) 0x2a, (byte) 0xC7, (byte) 0x01, /* Usage Maximum (455)           */
                        (byte) 0x81, (byte) 0x00,              /* Input (Data, Array)           */

                /*==================== Application Control Buttons =====================*/
                        (byte) 0x05, (byte) 0x0C,              /* Usage Page (Consumer Devices) */
                        (byte) 0x95, (byte) 0x01,              /* Report Count (1)              */
                        (byte) 0x75, (byte) 0x10,              /* Report Size (16)              */
                        (byte) 0x16, (byte) 0x01, (byte) 0x02, /* Logical Minimum (385)         */
                        (byte) 0x26, (byte) 0x9C, (byte) 0x02, /* Logical Maximum (455)         */
                        (byte) 0x05, (byte) 0x0C,              /* Usage Page (Consumer Devices) */
                        (byte) 0x1a, (byte) 0x01, (byte) 0x02, /* Usage Minimum (385)           */
                        (byte) 0x2a, (byte) 0x9C, (byte) 0x02, /* Usage Maximum (455)           */
                        (byte) 0x81, (byte) 0x00,              /* Input (Data, Array)           */
                        (byte) 0xC0,              /*       End Collection                       */
                } : new byte[]{};
        final byte REPORT_MAP_KEYBOARD[] =
                ((features & ReportField.REP_KEYBOARD) == ReportField.REP_KEYBOARD) ? new byte[]{
                /*============================== Keyboard ==============================*/
                        (byte) 0x05, (byte) 0x07, /*        Usage Page (Keyboard/Keypad)        */
                        (byte) 0x15, (byte) 0x00, /*        Logical Minimum (0)                 */
                        (byte) 0x25, (byte) 0x01, /*        Logical Maximum (1)                 */
                        (byte) 0x75, (byte) 0x01, /*        Report Size (1)                     */
                        (byte) 0x95, (byte) 0x08, /*        Report Count (8)                    */

                        (byte) 0x09, (byte) 0xE0, /* 1       Usage (LeftControl)                */
                        (byte) 0x09, (byte) 0xE1, /* 2       Usage (LeftShift)                  */
                        (byte) 0x09, (byte) 0xE2, /* 3       Usage (LeftAlt)                    */
                        (byte) 0x09, (byte) 0xE3, /* 4       Usage (LeftGUI)                    */
                        (byte) 0x09, (byte) 0xE4, /* 5       Usage (RightControl)               */
                        (byte) 0x09, (byte) 0xE5, /* 6       Usage (RightShift)                 */
                        (byte) 0x09, (byte) 0xE6, /* 7       Usage (RightAlt)                   */
                        (byte) 0x09, (byte) 0xE7, /* 8       Usage (RightGUI)                   */
                        (byte) 0x81, (byte) 0x02, /*        Input (Data, Variable, Absolute)    */

                        (byte) 0x05, (byte) 0x07, /*        Usage Page (Keyboard/Keypad)        */
                        (byte) 0x95, (byte) 0x01, /*        Report Count (1)                    */
                        (byte) 0x75, (byte) 0x08, /*        Report Size (8)                     */
                        (byte) 0x15, (byte) 0x04, /*        Logical Minimum (4)                 */
                        (byte) 0x25, (byte) 0xDF, /*        Logical Maximum (223)               */
                        (byte) 0x05, (byte) 0x07, /*        Usage Page (Key codes)              */
                        (byte) 0x19, (byte) 0x04, /*        Usage Minimum (4)                   */
                        (byte) 0x29, (byte) 0xDF, /*        Usage Maximum (223)                 */
                        (byte) 0x81, (byte) 0x00, /*        Input (Data, Array)                 */
                } : new byte[]{};
        final byte REPORT_MAP_MOUSE[] =
                ((features & ReportField.REP_MOUSE) == ReportField.REP_MOUSE) ? new byte[]{
                /*================================ Mouse ===============================*/
                        (byte) 0x05, (byte) 0x01, /*        Usage Page (Generic Desktop)        */
                        (byte) 0x09, (byte) 0x02, /*        Usage (Mouse)                       */
                        (byte) 0xa1, (byte) 0x01, /*         Collection (Application)            */
                        (byte) 0x85, (byte) 0x02,
                        (byte) 0x09, (byte) 0x01, /*        Usage (Consumer Control)            */
                        (byte) 0xa1, (byte) 0x00, /*        Collection (Physical)               */
                        (byte) 0x05, (byte) 0x09, /*        Usage Page (Button)                 */
                        (byte) 0x19, (byte) 0x01, /*        Usage Minimum (1)                   */
                        (byte) 0x29, (byte) 0x05, /*        Usage Maximum (5)                   */
                        (byte) 0x15, (byte) 0x00, /*        Logical Minimum (0)                 */
                        (byte) 0x25, (byte) 0x01, /*        Logical Maximum (1)                 */
                        (byte) 0x95, (byte) 0x05, /*        Report Count (5)                    */
                        (byte) 0x75, (byte) 0x01, /*        Report Size (1)                     */
                        (byte) 0x81, (byte) 0x02, /*        Input (Variable, Absolute)          */

                        (byte) 0x95, (byte) 0x01, /*        Report Count (1)                    */
                        (byte) 0x75, (byte) 0x03, /*        Report Size (3)                     */
                        (byte) 0x81, (byte) 0x01, /*        Input (Constant)                    */

                        (byte) 0x05, (byte) 0x01, /*        Usage Page (Generic Desktop)        */
                        (byte) 0x09, (byte) 0x30, /*        Usage (X)                           */
                        (byte) 0x09, (byte) 0x31, /*        Usage (Y)                           */
                        (byte) 0x15, (byte) 0x81, /*        Logical Minimum (-127)              */
                        (byte) 0x25, (byte) 0x7f, /*        Logical Maximum (127)               */
                        (byte) 0x75, (byte) 0x08, /*        Report Size (8)                     */
                        (byte) 0x95, (byte) 0x02, /*        Report Count (2)                    */
                        (byte) 0x81, (byte) 0x06, /*        Input (Variable, Relative)          */

                        (byte) 0x09, (byte) 0x38, /*        Usage (Wheel)                       */
                        (byte) 0x15, (byte) 0x81, /*        Logical Minimum (-127)              */
                        (byte) 0x25, (byte) 0x7f, /*        Logical Maximum (127)               */
                        (byte) 0x75, (byte) 0x08, /*        Report Size (8)                     */
                        (byte) 0x95, (byte) 0x01, /*        Report Count (1)                    */
                        (byte) 0x81, (byte) 0x06, /*        Input (Variable, Relative)          */
                        (byte) 0xC0,              /*       End Collection                       */
                        (byte) 0xC0,              /*       End Collection                       */
                } : new byte[]{};
        final byte REPORT_MAP_END[] = {
                (byte) 0xC0,              /*       End Collection                       */
        };

        int position = 0;
        byte REPORT_MAP[];


        if ((features & ReportField.REP_BASIC) == ReportField.REP_BASIC) {
            REPORT_MAP = REPORT_MAP_BASIC;
        } else {
            REPORT_MAP = new byte[REPORT_MAP_START.length + REPORT_MAP_CONSUMER.length +
                    REPORT_MAP_KEYBOARD.length + REPORT_MAP_MOUSE.length + REPORT_MAP_END.length];

            System.arraycopy(REPORT_MAP_START, 0, REPORT_MAP, position, REPORT_MAP_START.length);
            position += REPORT_MAP_START.length;
            System.arraycopy(REPORT_MAP_CONSUMER, 0, REPORT_MAP, position, REPORT_MAP_CONSUMER.length);
            position += REPORT_MAP_CONSUMER.length;
            System.arraycopy(REPORT_MAP_KEYBOARD, 0, REPORT_MAP, position, REPORT_MAP_KEYBOARD.length);
            position += REPORT_MAP_KEYBOARD.length;
            System.arraycopy(REPORT_MAP_MOUSE, 0, REPORT_MAP, position, REPORT_MAP_MOUSE.length);
            position += REPORT_MAP_MOUSE.length;
            System.arraycopy(REPORT_MAP_END, 0, REPORT_MAP, position, REPORT_MAP_END.length);
        }


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

        // Report characteristic
        BluetoothGattCharacteristic charReport1 = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);

        charReport1.setValue("\0\0".getBytes());

        /// Report Reference descriptor
        BluetoothGattDescriptor descReportReference1 = new BluetoothGattDescriptor(
                UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);

        descReportReference1.setValue("\2\1".getBytes());

        /// Client Characteristic Configuration descriptor
        BluetoothGattDescriptor descCCC1 = new BluetoothGattDescriptor(
                UUID.fromString(DESC_CCC), PERM_READ_WRITE);

        descCCC1.setValue("\0\0".getBytes());

        charReport1.addDescriptor(descReportReference1);
        charReport1.addDescriptor(descCCC1);

        serviceHid.addCharacteristic(charReportMap);
        serviceHid.addCharacteristic(charHidInformation);
        serviceHid.addCharacteristic(charHidControlPoint);
        serviceHid.addCharacteristic(charReport1);
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

                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

                    if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                            ApplicationConfiguration.FORCE_BOND_FEAT)) {
                        if (device.getBondState() != BluetoothDevice.BOND_BONDING &&
                                device.getBondState() != BluetoothDevice.BOND_BONDED) {
                            device.createBond();
                        } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                            device.setPairingConfirmation(true);
                        }
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
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                int charLength = characteristic.getValue().length;
                int partLength = (currentMtu > charLength - offset) ? charLength - offset : currentMtu;
                byte bytes[] = new byte[partLength];

                if (Objects.equals(characteristic.getUuid().toString(), CHAR_BATTERY_LEVEL)) {
                    characteristic.setValue(new byte[]{readBatteryLevel()});
                }

                for (int i = 0; i < partLength; i++) {
                    bytes[i] = characteristic.getValue()[i + offset];
                }

                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, bytes);

                Log.e("BLE", "onCharacteristicReadRequest " + characteristic.getUuid().toString() +
                        " offset " + offset + " " + new String(bytes));
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
                Log.e("BLE", "onDescriptorWriteRequest " + descriptor.getUuid().toString() + "value"
                        + value[0] + value[1]);
                descriptor.setValue(value);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                     BluetoothGattCharacteristic characteristic,
                                                     boolean preparedWrite, boolean responseNeeded,
                                                     int offset, byte[] value) {
                Log.e("BLE", "onCharacteristicWriteRequest " + characteristic.getUuid().toString() +
                        " offset " + offset);
                characteristic.setValue(value);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattDescriptor descriptor) {
                Log.e("BLE", "onDescriptorReadRequest " + descriptor.getUuid().toString());
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, descriptor.getValue());
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.e("BLE", "onNotificationSent - left: " + pendingNotifications.size());
                if (pendingNotifications.size() > 0) {
                    NotificationData toSend = pendingNotifications.remove(0);
                    toSend.characteristic.setValue(toSend.value);
                    gattServer.notifyCharacteristicChanged(toSend.device, toSend.characteristic,
                            toSend.responseNeeded);
                    notificationPossible = false;
                } else {
                    notificationPossible = true;
                }
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

    public ArrayList<BluetoothDevice> getDevices() {
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

        if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                ApplicationConfiguration.BASIC_FEAT)) {
            features |= ReportField.REP_BASIC;
        }

        ReportField.updateValues(features);
        gattServerCbInit();
        mManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (gattServer != null) {
            gattServer.close();
        }
        gattServer = mManager.openGattServer(getBaseContext(), mGattServerCallback);
        createGattDatabase(gattServer, SecurityLevel.SECURITY_LEVEL_2.ordinal(), features);
        pendingNotifications.clear();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initializeLE();
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