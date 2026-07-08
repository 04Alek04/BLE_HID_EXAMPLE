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

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements KeyEvent.Callback {
    private final Handler threadHandler = new Handler();
    public CustomViewPager pager = null;
    public PagerAdapter pagerAdapter = null;
    private volatile List<BluetoothDevice> devices = new ArrayList<>();
    private final Runnable deviceLayoutUpdate = new Runnable() {
        volatile List<BluetoothDevice> devices_copy = new ArrayList<>();

        public void run() {
            final long refreshLayoutPeriodMs = 300;
            final LinearLayout ll = (LinearLayout) findViewById(R.id.conf_linear_layout_devices);

            devices_copy = new ArrayList<>(devices);

            if (ll == null) {
                threadHandler.postDelayed(this, refreshLayoutPeriodMs);
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ll.removeAllViews();
                }
            });

            if (devices_copy == null) {
                threadHandler.postDelayed(this, refreshLayoutPeriodMs);
                return;
            }

            for (int k = 0; k < devices_copy.size(); k++) {
                BluetoothDevice bd;
                final TextView textView = new TextView(getApplicationContext());

                try {
                    bd = devices_copy.get(k);
                } catch (IndexOutOfBoundsException e) {
                    break;
                }

                textView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                textView.setWidth(ll.getWidth());
                textView.setBackgroundColor(0xff2546e9);
                textView.setTextColor(Color.WHITE);
                textView.setText("" + bd.getName() + "  (" + bd.getAddress() + ")");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ll.addView(textView);
                    }
                });
            }

            threadHandler.postDelayed(this, refreshLayoutPeriodMs);
        }
    };
    private final Runnable deviceFieldUpdate = new Runnable() {
        volatile List<BluetoothDevice> devices_copy = new ArrayList<>();
        private int i = 0;

        public void run() {
            final long refreshTextViewPeriodMs = 1200;
            String text;
            int text_color;
            int bg_color;

            devices_copy = new ArrayList<>(devices);

            if (devices_copy != null && devices_copy.size() > 0) {
                BluetoothDevice d;

                try {
                    d = devices_copy.get(i);
                } catch (IndexOutOfBoundsException e) {
                    i = 0;
                    threadHandler.postDelayed(this, refreshTextViewPeriodMs);
                    return;
                }

                text = "" + d.getName() + "  (" + d.getAddress() + ")";
                text_color = Color.WHITE;
                bg_color = 0xff2546e9;

                if (devices_copy.size() > 1) {
                    text += " [" + i + " / " + (devices_copy.size() - 1) + "]";
                }
            } else {
                text = "Device not connected ";
                text_color = Color.BLACK;
                bg_color = Color.GRAY;
            }

            updateDeviceInfoField(text, text_color, bg_color);
            i = (devices_copy == null || devices_copy.size() == 0) ? 0 : (i + 1) % devices_copy.size();
            threadHandler.postDelayed(this, refreshTextViewPeriodMs);
        }
    };
    private HidBleService mService = null;
    private byte lastKeyboardLedStatus = 0;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            HidBleService.LocalBinder binder = (HidBleService.LocalBinder) service;
            mService = binder.getService();
            mService.setActivity(MainActivity.this);
            devices = mService.getDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    private void updateDeviceInfoField(final String text, final int text_color, final int bg_color) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int[] ids = {
                        R.id.cons_text_device_info, R.id.mouse_text_device_info,
                        R.id.keyboard_text_device_info, R.id.d_pad_text_device_info
                };

                for (int id : ids) {
                    TextView textView = (TextView) findViewById(id);

                    if (textView == null) {
                        continue;
                    }

                    textView.setBackgroundColor(bg_color);
                    textView.setTextColor(text_color);
                    textView.setText(text);
                    textView.refreshDrawableState();
                }
            }
        });
    }

    public void onConnectionStateChange(List<BluetoothDevice> dev_array) {
        devices = new ArrayList<>(dev_array);
    }

    public void onKeyboardLedStatusChanged(final byte status) {
        Log.i("BLE", "MainActivity received LED status: " + status);
        lastKeyboardLedStatus = status;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pagerAdapter != null) {
                    for (int i = 0; i < pagerAdapter.getCount(); i++) {
                        Fragment f = pagerAdapter.getRegisteredFragment(i);
                        if (f instanceof KeyboardFragment) {
                            ((KeyboardFragment) f).updateLedStatus(status);
                        }
                    }
                }
            }
        });
    }

    public byte getLastKeyboardLedStatus() {
        return lastKeyboardLedStatus;
    }

    public void sendNotification(ReportField rf, int value) {
        if (mService == null) {
            // Service is not bound yet
            return;
        }

        mService.sendNotification(rf, value);
    }

    public void sendNotification(String s) {
        if (mService == null) {
            // Service is not bound yet
            return;
        }

        mService.sendNotification(s);
    }

    public void restartGattDatabase() {
        if (mService == null) {
            return;
        }

        mService.initializeLE();
    }

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        checkPermissions();
        checkBluetoothEnabled();

        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        pager = (CustomViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(1);
        pager.setHorizontalFadingEdgeEnabled(false);
        pager.swipePossible(true);

        Intent intent = new Intent(this, HidBleService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        ApplicationConfiguration.initializeConfiguration(getApplicationContext());
        threadHandler.post(deviceFieldUpdate);
        threadHandler.post(deviceLayoutUpdate);
    }

    private void checkBluetoothEnabled() {
        android.bluetooth.BluetoothAdapter bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void checkPermissions() {
        ArrayList<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            restartGattDatabase();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                checkBluetoothEnabled();
                restartGattDatabase();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
        threadHandler.removeCallbacks(deviceFieldUpdate);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        byte key = 0;
        byte meta_key = 0;
        short val;

        if (pager.getCurrentItem() == PagerAdapter.MOUSE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                sendNotification(ReportField.REPORT_FIELD_MOUSE_SCROLL, -1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                sendNotification(ReportField.REPORT_FIELD_MOUSE_SCROLL, 1);
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return false;
        }

        for (int i = 0; i < KeyboardUsage.KEYBOARD_USAGES.length; i++) {
            if (keyCode == KeyboardUsage.KEYBOARD_USAGES[i].key_code) {
                key = KeyboardUsage.KEYBOARD_USAGES[i].usage;
                meta_key = KeyboardUsage.KEYBOARD_USAGES[i].meta;
                break;
            }
        }

        if (key == 0) {
            Log.e("BLE", "Pressed unhandled keyboard button " + keyCode);
            return super.onKeyDown(keyCode, event);

        }

        if (event.isCtrlPressed()) {
            meta_key |= KeyboardUsage.META_LEFT_CONTROL;
        }

        if (event.isShiftPressed()) {
            meta_key |= KeyboardUsage.META_LEFT_SHIFT;
        }

        if (event.isAltPressed()) {
            meta_key |= KeyboardUsage.META_LEFT_ALT;
        }

        if (event.isMetaPressed()) {
            meta_key |= KeyboardUsage.META_LEFT_META;
        }

        val = (short) (meta_key + (key << 8));
        sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, (int) val);

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int key = 0;

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (pager.getCurrentItem() == PagerAdapter.MOUSE) {
                sendNotification(ReportField.REPORT_FIELD_MOUSE_SCROLL, 0);
                return true;
            } else {
                return false;
            }
        }

        sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, key);

        return true;
    }

    class PagerAdapter extends FragmentPagerAdapter {
        static final int CONFIGURATION = 0;
        static final int CONSUMER = 1;
        static final int MOUSE = 2;
        static final int KEYBOARD = 3;
        static final int D_PAD = 4;

        private final SparseArray<Fragment> registeredFragments = new SparseArray<>();

        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public Fragment getItem(int pos) {
            Fragment[] fragments = new Fragment[5];
            int index = 2;

            fragments[0] = ConfigurationFragment.newInstance();
            fragments[1] = ConsumerFragment.newInstance();
            fragments[2] = ConfigurationFragment.newInstance();
            fragments[3] = ConfigurationFragment.newInstance();
            fragments[4] = ConfigurationFragment.newInstance();

            // Consumer fragment is always available
            if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                    ApplicationConfiguration.MOUSE_FEAT)) {
                fragments[index] = MouseFragment.newInstance();
                ++index;
            }

            if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                    ApplicationConfiguration.KEYBOARD_FEAT)) {
                fragments[index] = KeyboardFragment.newInstance();
                ++index;
                fragments[index] = DPadFragment.newInstance();
                ++index;
            }

            return fragments[pos];
        }

        @Override
        public int getCount() {
            int count = 2;

            // Consumer fragment is always available
            if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                    ApplicationConfiguration.MOUSE_FEAT)) {
                count += 1;
            }

            if (ApplicationConfiguration.getConfigurationField(getApplicationContext(),
                    ApplicationConfiguration.KEYBOARD_FEAT)) {
                count += 2;
            }

            return count;
        }
    }
}