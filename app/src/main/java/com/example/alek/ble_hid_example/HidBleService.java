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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.bluetooth.BluetoothProfile;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    REPORT_FIELD_KEYBOARD_ALL(0, 3),    // Modifiers(0) + Reserved(1) + Keys(2)
    /* Mouse (Report ID 2) */
    REPORT_FIELD_MOUSE_BUTTONS(0, 1),
    REPORT_FIELD_MOUSE_X(1, 1),
    REPORT_FIELD_MOUSE_Y(2, 1),
    REPORT_FIELD_MOUSE_BUTTONS_XY(0, 3),
    REPORT_FIELD_MOUSE_XY(1, 2),
    REPORT_FIELD_MOUSE_SCROLL(3, 1),
    REPORT_FIELD_MOUSE_HSCROLL(4, 1),
    /* Absolute Mouse (Report ID 7) */
    REPORT_FIELD_ABS_MOUSE_BUTTONS(0, 1),
    REPORT_FIELD_ABS_MOUSE_X(1, 2),
    REPORT_FIELD_ABS_MOUSE_Y(3, 2),
    REPORT_FIELD_ABS_MOUSE_ALL(0, 5),
    /* Gamepad (Report ID 5) */
    REPORT_FIELD_GAMEPAD_BUTTONS(0, 2),
    REPORT_FIELD_GAMEPAD_X(2, 1),
    REPORT_FIELD_GAMEPAD_Y(3, 1),
    REPORT_FIELD_GAMEPAD_Z(4, 1),
    REPORT_FIELD_GAMEPAD_RX(5, 1);

    final static public int REP_CONSUMER = 0x01;
    final static public int REP_MOUSE = 0x02;
    final static public int REP_KEYBOARD = 0x04;
    final static public int REP_BATTERY = 0x08;
    final static public int REP_ABS_MOUSE = 0x20;
    final static public int REP_GAMEPAD = 0x40;
    public final int byte_size;
    public int byte_offset;

    ReportField(int byte_offset, int byte_size) {
        this.byte_offset = byte_offset;
        this.byte_size = byte_size;
    }

    static void updateValues() {
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
        REPORT_FIELD_MOUSE_HSCROLL.byte_offset = 4;

        REPORT_FIELD_ABS_MOUSE_BUTTONS.byte_offset = 0;
        REPORT_FIELD_ABS_MOUSE_X.byte_offset = 1;
        REPORT_FIELD_ABS_MOUSE_Y.byte_offset = 3;
        REPORT_FIELD_ABS_MOUSE_ALL.byte_offset = 0;

        REPORT_FIELD_GAMEPAD_BUTTONS.byte_offset = 0;
        REPORT_FIELD_GAMEPAD_X.byte_offset = 2;
        REPORT_FIELD_GAMEPAD_Y.byte_offset = 3;
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
    private final Map<String, Map<String, byte[]>> mDeviceDescriptorValues = new ConcurrentHashMap<>();
    private final Map<BluetoothDevice, Integer> mNotificationMasks = new ConcurrentHashMap<>();
    private final java.util.Set<String> mDisabledDeviceAddresses = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    
    /* Call proper methods on some BLE events  */
    private MainActivity mainActivity;
    private BluetoothGattServerCallback mGattServerCallback;
    private volatile boolean notificationPossible;
    private BluetoothGattServer gattServer;
    private BluetoothGattCharacteristic charReportKeyboard;
    private BluetoothGattCharacteristic charReportMouse;
    private BluetoothGattCharacteristic charReportConsumer;
    private BluetoothGattCharacteristic charReportBattery;
    private BluetoothGattCharacteristic charReportAbsMouse;
    private BluetoothGattCharacteristic charReportGamepad;
    private BluetoothGattCharacteristic charBatteryLevel;
    private byte lastKeyboardLedStatus = 0; // Bit 0: NumLock, Bit 1: CapsLock, Bit 2: ScrollLock
    private String mOriginalName = null;
    private volatile boolean isAdvertising = false;
    private final AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i("BLE", "Advertisement started successfully");
            isAdvertising = true;
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.i("BLE", "Advertisement not started - error: " + errorCode);
            isAdvertising = false;
        }
    };

    private byte lastBatteryLevel = -1;

    private byte readBatteryLevel() {
        if (lastBatteryLevel >= 0) {
            return lastBatteryLevel;
        }
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (batteryManager != null) {
            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (batteryLevel >= 0) {
                lastBatteryLevel = (byte) batteryLevel;
                return lastBatteryLevel;
            }
        }
        return 100;
    }

    private void sendReport(BluetoothGattCharacteristic characteristic, byte[] value, int maskBit, boolean canMerge, SendTo st) {
        if (devices.isEmpty() || gattServer == null || characteristic == null) return;

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
                if (!isDeviceEnabled(device)) {
                    continue;
                }

                Integer mask = mNotificationMasks.get(device);
                if (mask == null || (mask & maskBit) == 0) {
                    continue;
                }

                boolean merged = false;
                if (canMerge && characteristic == charReportMouse) {
                    // Try to merge with an existing pending mouse move for this device
                    for (int i = pendingNotifications.size() - 1; i >= 0; i--) {
                        NotificationData data = pendingNotifications.get(i);
                        if (data.device.equals(device) && data.characteristic == characteristic) {
                            if (data.value[0] == value[0]) {
                                data.value[1] = (byte) Math.max(-128, Math.min(127, data.value[1] + value[1]));
                                data.value[2] = (byte) Math.max(-128, Math.min(127, data.value[2] + value[2]));
                                merged = true;
                            }
                            break;
                        }
                    }
                }

                if (!merged) {
                    pendingNotifications.add(new NotificationData(device, characteristic, value.clone()));
                }
            }
        }

        processNotificationQueue();
    }

    public void sendKeyboardNotification(ReportField rf, int value, SendTo st) {
        byte[] array = new byte[8];
        if (rf == ReportField.REPORT_FIELD_KEYBOARD_ALL) {
            array[0] = (byte) (value & 0xff);
            array[2] = (byte) ((value >> 8) & 0xff);
        } else {
            int tempVal = value;
            for (int i = rf.byte_offset; i < rf.byte_offset + rf.byte_size; i++) {
                if (i < array.length) {
                    array[i] = (byte) (tempVal & 0xff);
                    tempVal >>= 8;
                }
            }
        }
        sendReport(charReportKeyboard, array, ReportField.REP_KEYBOARD, false, st);
    }

    private int mouseButtonState = 0;

    public void sendMouseNotification(ReportField rf, int value, SendTo st) {
        byte[] array = new byte[5];
        
        if (rf == ReportField.REPORT_FIELD_MOUSE_BUTTONS || rf == ReportField.REPORT_FIELD_MOUSE_BUTTONS_XY) {
            mouseButtonState = value & 0xFF;
        }
        
        int tempVal = value;
        for (int i = rf.byte_offset; i < rf.byte_offset + rf.byte_size; i++) {
            if (i < array.length) {
                array[i] = (byte) (tempVal & 0xff);
                tempVal >>= 8;
            }
        }
        
        // Ensure buttons state is preserved in every mouse report
        array[0] = (byte) mouseButtonState;
        
        sendReport(charReportMouse, array, ReportField.REP_MOUSE, rf == ReportField.REPORT_FIELD_MOUSE_XY || rf == ReportField.REPORT_FIELD_MOUSE_BUTTONS_XY, st);
    }

    public void sendConsumerNotification(ReportField rf, int value, SendTo st) {
        byte[] array = new byte[6];
        int tempVal = value;
        for (int i = rf.byte_offset; i < rf.byte_offset + rf.byte_size; i++) {
            if (i < array.length) {
                array[i] = (byte) (tempVal & 0xff);
                tempVal >>= 8;
            }
        }
        sendReport(charReportConsumer, array, ReportField.REP_CONSUMER, false, st);
    }

    public void sendAbsMouseNotification(int buttons, int x, int y) {
        byte[] array = new byte[5];
        array[0] = (byte) (buttons & 0xFF);
        array[1] = (byte) (x & 0xFF);
        array[2] = (byte) ((x >> 8) & 0xFF);
        array[3] = (byte) (y & 0xFF);
        array[4] = (byte) ((y >> 8) & 0xFF);
        sendReport(charReportAbsMouse, array, ReportField.REP_ABS_MOUSE, false, SendTo.SEND_TO_ALL);
    }

    public void sendGamepadNotification(int buttons, int lx, int ly, int rx, int ry) {
        byte[] array = new byte[6];
        array[0] = (byte) (buttons & 0xFF);
        array[1] = (byte) ((buttons >> 8) & 0x0F);
        array[2] = (byte) (lx & 0xFF);
        array[3] = (byte) (ly & 0xFF);
        array[4] = (byte) (rx & 0xFF);
        array[5] = (byte) (ry & 0xFF);
        sendReport(charReportGamepad, array, ReportField.REP_GAMEPAD, false, SendTo.SEND_TO_ALL);
    }

    private synchronized void processNotificationQueue() {
        if (!notificationPossible || gattServer == null) {
            return;
        }

        while (!pendingNotifications.isEmpty()) {
            NotificationData toSend = pendingNotifications.remove(0);

            if (devices.contains(toSend.device)) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e("BLE", "Missing BLUETOOTH_CONNECT permission for notification");
                    return;
                }

                toSend.characteristic.setValue(toSend.value);
                boolean success = gattServer.notifyCharacteristicChanged(toSend.device, toSend.characteristic,
                        toSend.responseNeeded);

                if (success) {
                    notificationPossible = false;
                    // Exit loop, wait for onNotificationSent to set notificationPossible = true
                    break;
                } else {
                    Log.e("BLE", "Failed to queue notification, skipping this one");
                    // Continue to next item in queue
                }
            }
            // If device was disconnected, the loop continues to the next pending notification
        }
    }

    public void sendKeyboardText(String s) {
        sendKeyboardText(s, SendTo.SEND_TO_ALL);
    }

    private void sendKeyboardText(String s, SendTo st) {
        // Bit 1 of lastKeyboardLedStatus is Caps Lock
        boolean hostCapsLock = (lastKeyboardLedStatus & 0x02) != 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            for (int j = 0; j < KeyboardUsage.KEYBOARD_USAGES.length; j++) {
                if (Character.toLowerCase(c) == KeyboardUsage.KEYBOARD_USAGES[j].character) {
                    byte m = KeyboardUsage.KEYBOARD_USAGES[j].meta;
                    byte k = KeyboardUsage.KEYBOARD_USAGES[j].usage;

                    // SMART SHIFT LOGIC (XOR) for Caps Lock
                    if (Character.isLetter(c)) {
                        boolean charIsUpper = Character.isUpperCase(c);
                        // Shift is needed only if (IsUpper != HostCapsLock)
                        boolean needsShift = charIsUpper ^ hostCapsLock;
                        
                        if (needsShift) m |= KeyboardUsage.META_LEFT_SHIFT;
                        else m &= ~KeyboardUsage.META_LEFT_SHIFT;
                    } else {
                        // For non-letters, follow the mapping but preserve manual shift if needed
                        if (Character.isUpperCase(c) || "!@#$%^&*()_+{}|:\"<>?".indexOf(c) != -1) {
                            m |= KeyboardUsage.META_LEFT_SHIFT;
                        }
                    }

                    int val = (m & 0xFF) | ((k & 0xFF) << 8);
                    sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, val, st);
                    sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0, st);
                    break;
                }
            }
        }

        sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0, st);
    }

    private boolean ensureHidDeviceName() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null) {
            return false;
        }

        SharedPreferences prefs = getSharedPreferences("ble_device_prefs", Context.MODE_PRIVATE);
        String savedOriginalName = prefs.getString("original_device_name", null);

        String currentName = null;
        try {
            currentName = bluetoothAdapter.getName();
        } catch (SecurityException e) {
            Log.e("BLE", "Missing BLUETOOTH_CONNECT permission to get adapter name", e);
        }

        if (currentName != null) {
            if (!currentName.startsWith("(HID) ")) {
                // User changed name in system settings or app started fresh with clean name
                savedOriginalName = currentName;
                prefs.edit().putString("original_device_name", savedOriginalName).apply();
            } else if (savedOriginalName == null) {
                // Name has (HID) from a previous unclosed instance; strip prefix to recover clean name
                savedOriginalName = currentName.substring(6).trim();
                prefs.edit().putString("original_device_name", savedOriginalName).apply();
            }
        }

        mOriginalName = savedOriginalName;

        String baseName = mOriginalName != null ? mOriginalName : "Device";

        String prefix = "(HID) ";
        String newName;
        if (baseName.startsWith(prefix)) {
            newName = baseName;
        } else {
            String shortName = baseName.length() > 10 ? baseName.substring(0, 10) : baseName;
            newName = prefix + shortName;
        }

        if (currentName == null || !currentName.equals(newName)) {
            Log.i("BLE", "Setting device name to: " + newName);
            try {
                bluetoothAdapter.setName(newName);
                return true;
            } catch (SecurityException e) {
                Log.e("BLE", "Missing BLUETOOTH_CONNECT permission to set adapter name", e);
            }
        }
        return false;
    }

    private void stopAdvertising() {
        if (!isAdvertising) return;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter != null) {
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser != null) {
                try {
                    advertiser.stopAdvertising(advertisingCallback);
                } catch (SecurityException e) {
                    Log.e("BLE", "Missing BLUETOOTH_ADVERTISE permission to stop advertising", e);
                }
            }
        }
        isAdvertising = false;
    }

    private void startAdvertising() {
        if (isAdvertising) {
            Log.d("BLE", "Advertising is already active, skipping startAdvertising()");
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null) {
            Log.e("BLE", "BluetoothAdapter is null");
            return;
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

        try {
            advertiser.startAdvertising(settings, advData, scanResponse, advertisingCallback);
        } catch (SecurityException e) {
            Log.e("BLE", "Missing BLUETOOTH_ADVERTISE permission to start advertising", e);
        }
    }

    private void createGattDatabase(BluetoothGattServer gattServer, int level) {
        // Set characteristics' permissions
        final int PERM_READ = (level == SecurityLevel.SECURITY_LEVEL_3.ordinal()) ?
                PERMISSION_READ_ENCRYPTED_MITM : ((level == SecurityLevel.SECURITY_LEVEL_2.ordinal()) ?
                PERMISSION_READ_ENCRYPTED : PERMISSION_READ);
        final int PERM_WRITE = (level == SecurityLevel.SECURITY_LEVEL_3.ordinal()) ?
                PERMISSION_WRITE_ENCRYPTED_MITM : ((level == SecurityLevel.SECURITY_LEVEL_2.ordinal()) ?
                PERMISSION_WRITE_ENCRYPTED : PERMISSION_WRITE);
        final int PERM_READ_WRITE = PERM_READ | PERM_WRITE;

        // Clear current attributes database - avoid adding the same services to database
        try {
            gattServer.clearServices();
        } catch (SecurityException e) {
            Log.e("BLE", "Missing BLUETOOTH_CONNECT permission to clear services", e);
        }

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
                (byte) 0x81, (byte) 0x02, /* Input (Data, Var, Abs) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x75, (byte) 0x03, /* Report Size (3) */
                (byte) 0x81, (byte) 0x01, /* Input (Const, Var, Abs) */
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
                (byte) 0x05, (byte) 0x0C, /* Usage Page (Consumer) */
                (byte) 0x0A, (byte) 0x38, (byte) 0x02, /* Usage (AC Pan) */
                (byte) 0x15, (byte) 0x80, /* Logical Minimum (-128) */
                (byte) 0x25, (byte) 0x7F, /* Logical Maximum (127) */
                (byte) 0x75, (byte) 0x08, /* Report Size (8) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x81, (byte) 0x06, /* Input (Data, Variable, Relative) - AC Pan */
                (byte) 0xC0, /* End Collection (Physical) */
                (byte) 0xC0, /* End Collection (Application) */

                // --- BATTERY SYSTEM (ID 4) ---
                (byte) 0x05, (byte) 0x85, /* Usage Page (Battery System) */
                (byte) 0x09, (byte) 0x01, /* Usage (Battery System) */
                (byte) 0xA1, (byte) 0x01, /* Collection (Application) */
                (byte) 0x85, (byte) 0x04, /* Report ID (4) */
                (byte) 0x09, (byte) 0x66, /* Usage (Remaining Capacity) */
                (byte) 0x15, (byte) 0x00, /* Logical Minimum (0) */
                (byte) 0x25, (byte) 0x64, /* Logical Maximum (100) */
                (byte) 0x75, (byte) 0x08, /* Report Size (8) */
                (byte) 0x95, (byte) 0x01, /* Report Count (1) */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Variable, Absolute) */
                (byte) 0xC0, /* End Collection */

                // --- ABSOLUTE MOUSE / DIGITIZER (ID 7) ---
                (byte) 0x05, 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, 0x02, /* Usage (Mouse) */
                (byte) 0xA1, 0x01, /* Collection (Application) */
                (byte) 0x85, 0x07, /* Report ID (7) */
                (byte) 0x09, 0x01, /* Usage (Pointer) */
                (byte) 0xA1, 0x00, /* Collection (Physical) */
                (byte) 0x05, 0x09, /* Usage Page (Button) */
                (byte) 0x19, 0x01, /* Usage Minimum (Button 1) */
                (byte) 0x29, 0x05, /* Usage Maximum (Button 5) */
                (byte) 0x15, 0x00, /* Logical Minimum (0) */
                (byte) 0x25, 0x01, /* Logical Maximum (1) */
                (byte) 0x95, 0x05, /* Report Count (5) */
                (byte) 0x75, 0x01, /* Report Size (1) */
                (byte) 0x81, 0x02, /* Input (Data, Var, Abs) - Buttons */
                (byte) 0x95, 0x01, /* Report Count (1) */
                (byte) 0x75, 0x03, /* Report Size (3) */
                (byte) 0x81, 0x01, /* Input (Const, Var, Abs) - Padding */
                (byte) 0x05, 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, 0x30, /* Usage (X) */
                (byte) 0x09, 0x31, /* Usage (Y) */
                (byte) 0x15, 0x00, /* Logical Minimum (0) */
                (byte) 0x26, (byte) 0xFF, 0x7F, /* Logical Maximum (32767) */
                (byte) 0x75, 0x10, /* Report Size (16) */
                (byte) 0x95, 0x02, /* Report Count (2) */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Var, Abs) - X, Y */
                (byte) 0xC0, (byte) 0xC0, /* End Collections */

                // --- GAMEPAD (ID 5) ---
                (byte) 0x05, 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, 0x05, /* Usage (Game Pad) */
                (byte) 0xA1, 0x01, /* Collection (Application) */
                (byte) 0x85, 0x05, /* Report ID (5) */
                // 12 Buttons (0x01 to 0x0C)
                (byte) 0x05, 0x09, /* Usage Page (Button) */
                (byte) 0x19, 0x01, /* Usage Minimum (Button 1 - 0x01) */
                (byte) 0x29, 0x0C, /* Usage Maximum (Button 12 - 0x0C) */
                (byte) 0x15, 0x00, /* Logical Minimum (0) */
                (byte) 0x25, 0x01, /* Logical Maximum (1) */
                (byte) 0x95, 0x0C, /* Report Count (12) */
                (byte) 0x75, 0x01, /* Report Size (1) */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Var, Abs) */
                // Padding 4 bits to complete 2 bytes
                (byte) 0x95, 0x01, /* Report Count (1) */
                (byte) 0x75, 0x04, /* Report Size (4) */
                (byte) 0x81, (byte) 0x03, /* Input (Const, Var, Abs) */
                // 4 Joysticks (0x30, 0x31, 0x32, 0x35)
                (byte) 0x05, 0x01, /* Usage Page (Generic Desktop) */
                (byte) 0x09, 0x30, /* Usage (X - Left Stick Horizontal) */
                (byte) 0x09, 0x31, /* Usage (Y - Left Stick Vertical) */
                (byte) 0x09, 0x32, /* Usage (Z - Right Stick Horizontal) */
                (byte) 0x09, 0x35, /* Usage (Rz - Right Stick Vertical) */
                (byte) 0x15, (byte) 0x81, /* Logical Minimum (-127) */
                (byte) 0x25, 0x7F, /* Logical Maximum (127) */
                (byte) 0x75, 0x08, /* Report Size (8) */
                (byte) 0x95, 0x04, /* Report Count (4) */
                (byte) 0x81, (byte) 0x02, /* Input (Data, Var, Abs) */
                (byte) 0xC0, /* End Collection */
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
        BluetoothGattCharacteristic charReportKeyboardOutput = new BluetoothGattCharacteristic(
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
        charReportMouse.setValue(new byte[5]);
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

        // Report Battery characteristic (ID 4)
        charReportBattery = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);
        charReportBattery.setValue(new byte[1]);
        BluetoothGattDescriptor descReportRefBattery = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefBattery.setValue(new byte[]{0x04, 0x01}); // ID=4, Type=Input
        charReportBattery.addDescriptor(descReportRefBattery);
        charReportBattery.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));

        serviceHid.addCharacteristic(charReportMap);
        serviceHid.addCharacteristic(charHidInformation);
        serviceHid.addCharacteristic(charHidControlPoint);
        serviceHid.addCharacteristic(charProtocolMode);
        serviceHid.addCharacteristic(charReportKeyboard);
        serviceHid.addCharacteristic(charReportKeyboardOutput);
        serviceHid.addCharacteristic(charReportMouse);
        serviceHid.addCharacteristic(charReportConsumer);
        serviceHid.addCharacteristic(charReportBattery);

        // Report Absolute Mouse characteristic (ID 7)
        charReportAbsMouse = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);
        charReportAbsMouse.setValue(new byte[5]);
        BluetoothGattDescriptor descReportRefAbsMouse = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefAbsMouse.setValue(new byte[]{0x07, 0x01}); // ID=7, Type=Input
        charReportAbsMouse.addDescriptor(descReportRefAbsMouse);
        charReportAbsMouse.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));
        serviceHid.addCharacteristic(charReportAbsMouse);

        // Report Gamepad characteristic (ID 5)
        charReportGamepad = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_REPORT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ_WRITE);
        charReportGamepad.setValue(new byte[4]); // 2 bytes buttons, 2 bytes axes
        BluetoothGattDescriptor descReportRefGamepad = new BluetoothGattDescriptor(UUID.fromString(DESC_REPORT_REFERENCE), PERM_READ);
        descReportRefGamepad.setValue(new byte[]{0x05, 0x01}); // ID=5, Type=Input
        charReportGamepad.addDescriptor(descReportRefGamepad);
        charReportGamepad.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));
        serviceHid.addCharacteristic(charReportGamepad);
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
        charBatteryLevel = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_BATTERY_LEVEL),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, PERM_READ);

        charBatteryLevel.setValue(new byte[]{readBatteryLevel()});
        charBatteryLevel.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESC_CCC), PERM_READ_WRITE));
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
                    
                    setDeviceEnabled(device, true);
                    updateNotificationMask(device);

                    if (devices.size() == 1) {
                        notificationPossible = true;
                    }

                    // Request Low Latency connection parameters with a delay
                    // to allow initial HID handshake and pairing to complete.
                    new Handler(Looper.getMainLooper()).postDelayed(() -> requestLowLatency(device), 1500);
                    
                    // Resume advertising to allow more devices to connect
                    isAdvertising = false;
                    startAdvertising();
                } else if (newState == STATE_DISCONNECTED) {
                    if (devices.contains(device)) {
                        // If some notification waiting for this device then they will be removed in
                        // proper task
                        devices.remove(device);
                    }
                    mNotificationMasks.remove(device);

                    if (devices.size() == 0) {
                        notificationPossible = false;
                    }

                    // Resume advertising to allow new connections
                    isAdvertising = false;
                    startAdvertising();
                }

                if (mainActivity != null) {
                    mainActivity.onConnectionStateChange(devices);
                }
            }

            private void requestLowLatency(BluetoothDevice device) {
                if (device == null || gattServer == null) return;
                Log.i("BLE", "LowLatency: Requesting HIGH priority for " + device.getAddress());

                try {
                    // Try to find the method in BluetoothGattServer (hidden before API 35)
                    // Method signature: boolean requestConnectionPriority(BluetoothDevice device, int priority)
                    Method method = gattServer.getClass().getMethod("requestConnectionPriority",
                            BluetoothDevice.class, int.class);
                    
                    method.setAccessible(true);
                    method.invoke(gattServer, device, BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                    Log.i("BLE", "LowLatency: SUCCESS via BluetoothGattServer reflection.");
                } catch (Exception e) {
                    Log.w("BLE", "LowLatency: FAILED to request via reflection. Fallback to default.");
                    // Fallback to a simpler, more common hidden method if the above fails
                    try {
                        Method m = device.getClass().getMethod("connectGatt", Context.class, boolean.class, 
                                android.bluetooth.BluetoothGattCallback.class, int.class);
                        // Using transport LE (2)
                        m.invoke(device, HidBleService.this, false, new android.bluetooth.BluetoothGattCallback() {}, 2);
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                    BluetoothGattCharacteristic characteristic) {
                if (gattServer == null) return;
                
                if (characteristic.getUuid().toString().equalsIgnoreCase(CHAR_BATTERY_LEVEL)) {
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
                saveDescriptorValue(device, descriptor, value);
                
                if (descriptor.getUuid().toString().equalsIgnoreCase(DESC_CCC)) {
                    updateNotificationMask(device);
                }
                
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
                
                // Get Report ID from descriptor to be 100% sure
                byte reportId = 0;
                BluetoothGattDescriptor desc = characteristic.getDescriptor(UUID.fromString(DESC_REPORT_REFERENCE));
                if (desc != null && desc.getValue() != null && desc.getValue().length > 0) {
                    reportId = desc.getValue()[0];
                }

                Log.i("BLE", "Write Request - Report ID: " + reportId + ", UUID: " + characteristic.getUuid().toString() + 
                        ", Value: " + (value != null ? Arrays.toString(value) : "null"));
                
                characteristic.setValue(value);

                if (value != null && value.length > 0) {
                    if (reportId == 1) { // Keyboard LEDs
                        lastKeyboardLedStatus = value[0];
                        if (mainActivity != null) mainActivity.onKeyboardLedStatusChanged(value[0]);
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
                byte[] value = getDescriptorValue(device, descriptor);
                if (value == null) value = new byte[0];

                int length = value.length;
                if (offset > length) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                    return;
                }

                byte[] responsePart = new byte[length - offset];
                System.arraycopy(value, offset, responsePart, 0, length - offset);

                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, responsePart);
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

    private String getResolvedAddress(BluetoothDevice device) {
        if (device == null) return "";
        try {
            Method m = device.getClass().getMethod("getIdentityAddress");
            String identity = (String) m.invoke(device);
            if (identity != null && !identity.isEmpty()) {
                return identity;
            }
        } catch (Exception ignored) {}

        String address = device.getAddress();
        try {
            BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = bm != null ? bm.getAdapter() : BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                java.util.Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                if (bondedDevices != null) {
                    for (BluetoothDevice bonded : bondedDevices) {
                        if (bonded.getAddress().equalsIgnoreCase(address)) {
                            return bonded.getAddress();
                        }
                    }
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED && bondedDevices.size() == 1) {
                        return bondedDevices.iterator().next().getAddress();
                    }
                }
            }
        } catch (SecurityException ignored) {}

        return address;
    }

    private void saveDescriptorValue(BluetoothDevice device, BluetoothGattDescriptor descriptor, byte[] value) {
        descriptor.setValue(value);
        String deviceAddress = getResolvedAddress(device);
        String charUuid = descriptor.getCharacteristic().getUuid().toString();
        String descUuid = descriptor.getUuid().toString();

        if (descUuid.equalsIgnoreCase(DESC_CCC)) {
            Map<String, byte[]> deviceMap = mDeviceDescriptorValues.get(deviceAddress);
            if (deviceMap == null) {
                deviceMap = new HashMap<>();
                mDeviceDescriptorValues.put(deviceAddress, deviceMap);
            }
            deviceMap.put(charUuid, value);

            // Persist to SharedPreferences
            getSharedPreferences("ble_ccc_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(deviceAddress + "_" + charUuid, bytesToHex(value))
                    .apply();
        }
    }

    private byte[] getDescriptorValue(BluetoothDevice device, BluetoothGattDescriptor descriptor) {
        if (descriptor == null) return null;
        String deviceAddress = getResolvedAddress(device);
        String charUuid = descriptor.getCharacteristic().getUuid().toString();
        String descUuid = descriptor.getUuid().toString();

        if (descUuid.equalsIgnoreCase(DESC_CCC)) {
            Map<String, byte[]> deviceMap = mDeviceDescriptorValues.get(deviceAddress);
            if (deviceMap != null && deviceMap.containsKey(charUuid)) {
                return deviceMap.get(charUuid);
            }

            // Try loading from SharedPreferences
            String saved = getSharedPreferences("ble_ccc_prefs", Context.MODE_PRIVATE)
                    .getString(deviceAddress + "_" + charUuid, null);
            if (saved != null) {
                byte[] value = hexToBytes(saved);
                Map<String, byte[]> map = mDeviceDescriptorValues.get(deviceAddress);
                if (map == null) {
                    map = new HashMap<>();
                    mDeviceDescriptorValues.put(deviceAddress, map);
                }
                map.put(charUuid, value);
                return value;
            }
            // For bonded HID devices, default to notifications ENABLED (0x01, 0x00) if no value stored
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                return new byte[]{1, 0};
            }
            // For unbonded CCCD, if no value is stored, default disabled (0x00, 0x00)
            return new byte[]{0, 0};
        }
        return descriptor.getValue();
    }

    private boolean isNotificationEnabled(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) return false;
        BluetoothGattDescriptor ccc = characteristic.getDescriptor(UUID.fromString(DESC_CCC));
        if (ccc == null) return false;

        byte[] value = getDescriptorValue(device, ccc);
        return value != null && value.length > 0 && value[0] == 0x01;
    }
    
    private void updateNotificationMask(BluetoothDevice device) {
        int mask = 0;
        if (isNotificationEnabled(device, charReportKeyboard)) mask |= ReportField.REP_KEYBOARD;
        if (isNotificationEnabled(device, charReportMouse)) mask |= ReportField.REP_MOUSE;
        if (isNotificationEnabled(device, charReportConsumer)) mask |= ReportField.REP_CONSUMER;
        if (isNotificationEnabled(device, charReportBattery)) mask |= ReportField.REP_BATTERY;
        if (isNotificationEnabled(device, charReportAbsMouse)) mask |= ReportField.REP_ABS_MOUSE;
        if (isNotificationEnabled(device, charReportGamepad)) mask |= ReportField.REP_GAMEPAD;
        mNotificationMasks.put(device, mask);
        Log.i("BLE", "Updated notification mask for " + device.getAddress() + ": " + mask);
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public void clearDeviceConfigs() {
        mDeviceDescriptorValues.clear();
        mNotificationMasks.clear();
        getSharedPreferences("ble_ccc_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        Log.i("BLE", "Cleared all saved device CCC configurations");
    }

    public List<BluetoothDevice> getDevices() {
        return devices;
    }

    public boolean isDeviceEnabled(BluetoothDevice device) {
        if (device == null) return false;
        String resAddr = getResolvedAddress(device);
        String rawAddr = device.getAddress();
        return !mDisabledDeviceAddresses.contains(resAddr) && !mDisabledDeviceAddresses.contains(rawAddr);
    }

    public void setDeviceEnabled(BluetoothDevice device, boolean enabled) {
        if (device == null) return;
        String resAddr = getResolvedAddress(device);
        String rawAddr = device.getAddress();
        if (enabled) {
            mDisabledDeviceAddresses.remove(resAddr);
            mDisabledDeviceAddresses.remove(rawAddr);
        } else {
            mDisabledDeviceAddresses.add(resAddr);
            mDisabledDeviceAddresses.add(rawAddr);
            synchronized (pendingNotifications) {
                List<NotificationData> toRemove = new ArrayList<>();
                for (NotificationData data : pendingNotifications) {
                    if (data.device.equals(device) || getResolvedAddress(data.device).equalsIgnoreCase(resAddr)) {
                        toRemove.add(data);
                    }
                }
                pendingNotifications.removeAll(toRemove);
            }
        }
    }

    public List<BluetoothDevice> getEnabledDevices() {
        List<BluetoothDevice> enabledList = new ArrayList<>();
        synchronized (devices) {
            for (BluetoothDevice d : devices) {
                if (isDeviceEnabled(d)) {
                    enabledList.add(d);
                }
            }
        }
        return enabledList;
    }

    public void setActivity(MainActivity ma) {
        mainActivity = ma;
    }

    /* Service handling-related methods */
    public void initializeLE() {
        BluetoothManager mManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        BluetoothAdapter bluetoothAdapter = mManager != null ? mManager.getAdapter() : null;
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e("BLE", "Bluetooth is not enabled or not supported");
            return;
        }

        ReportField.updateValues();
        boolean nameChanged = ensureHidDeviceName();
        gattServerCbInit();
        mManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (gattServer != null) {
            gattServer.close();
        }
        gattServer = mManager.openGattServer(this, mGattServerCallback);
        pendingNotifications.clear();

        if (nameChanged) {
            // Wait 300ms for Bluetooth hardware stack to update local name before creating GATT database and advertising
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (gattServer != null) {
                    createGattDatabase(gattServer, SecurityLevel.SECURITY_LEVEL_2.ordinal());
                }
            }, 300);
        } else {
            createGattDatabase(gattServer, SecurityLevel.SECURITY_LEVEL_2.ordinal());
        }
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatteryFromIntent(intent);
        }
    };

    private void updateBatteryFromIntent(Intent intent) {
        if (intent == null) return;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) {
            byte batteryPct = (byte) Math.round((level / (float) scale) * 100);
            if (batteryPct != lastBatteryLevel) {
                lastBatteryLevel = batteryPct;
                notifyBatteryLevelChanged(lastBatteryLevel);
            }
        }
    }

    private void notifyBatteryLevelChanged(byte level) {
        if (gattServer == null || devices.isEmpty()) return;

        // Update standard BAS
        if (charBatteryLevel != null) {
            charBatteryLevel.setValue(new byte[]{level});
            synchronized (devices) {
                for (BluetoothDevice device : devices) {
                    if (isNotificationEnabled(device, charBatteryLevel)) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            try {
                                gattServer.notifyCharacteristicChanged(device, charBatteryLevel, false);
                            } catch (SecurityException ignored) {}
                        }
                    }
                }
            }
        }

        // Update HID Battery Report (ID 4)
        if (charReportBattery != null) {
            charReportBattery.setValue(new byte[]{level});
            synchronized (devices) {
                for (BluetoothDevice device : devices) {
                    if (isNotificationEnabled(device, charReportBattery)) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            try {
                                gattServer.notifyCharacteristicChanged(device, charReportBattery, false);
                            } catch (SecurityException ignored) {}
                        }
                    }
                }
            }
        }
    }

    private final BroadcastReceiver nameChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED.equals(intent.getAction())) {
                String newName = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME);
                if (newName != null && !newName.startsWith("(HID) ")) {
                    Log.i("BLE", "System device name changed by user to: " + newName);
                    mOriginalName = newName;
                    getSharedPreferences("ble_device_prefs", Context.MODE_PRIVATE)
                            .edit().putString("original_device_name", newName).apply();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        initializeLE();

        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent stickyIntent = androidx.core.content.ContextCompat.registerReceiver(this, batteryReceiver, batteryFilter, androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
        if (stickyIntent != null) {
            updateBatteryFromIntent(stickyIntent);
        } else {
            lastBatteryLevel = readBatteryLevel();
            notifyBatteryLevelChanged(lastBatteryLevel);
        }

        IntentFilter nameFilter = new IntentFilter(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
        androidx.core.content.ContextCompat.registerReceiver(this, nameChangeReceiver, nameFilter, androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopAdvertising();

        try {
            unregisterReceiver(batteryReceiver);
        } catch (IllegalArgumentException ignored) {}

        try {
            unregisterReceiver(nameChangeReceiver);
        } catch (IllegalArgumentException ignored) {}

        SharedPreferences prefs = getSharedPreferences("ble_device_prefs", Context.MODE_PRIVATE);
        String nameToRestore = mOriginalName != null ? mOriginalName : prefs.getString("original_device_name", null);

        if (nameToRestore != null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
            if (bluetoothAdapter != null) {
                Log.i("BLE", "Restoring original device name: " + nameToRestore);
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    try {
                        bluetoothAdapter.setName(nameToRestore);
                        prefs.edit().remove("original_device_name").apply();
                    } catch (SecurityException ignored) {}
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
