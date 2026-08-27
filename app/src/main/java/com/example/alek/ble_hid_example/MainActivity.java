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
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements KeyEvent.Callback {
    private final Handler threadHandler = new Handler();
    public CustomViewPager pager = null;
    public PagerAdapter pagerAdapter = null;
    private volatile List<BluetoothDevice> devices = new ArrayList<>();
    
    public boolean isDeviceEnabled(BluetoothDevice device) {
        return mService == null || mService.isDeviceEnabled(device);
    }

    public void setDeviceEnabled(BluetoothDevice device, boolean enabled) {
        if (mService != null) {
            mService.setDeviceEnabled(device, enabled);
        }
    }

    public List<BluetoothDevice> getEnabledDevices() {
        return mService != null ? mService.getEnabledDevices() : new ArrayList<>(devices);
    }

    private final Runnable deviceLayoutUpdate = new Runnable() {
        private List<BluetoothDevice> lastDevices = new ArrayList<>();
        private LinearLayout lastLayout = null;

        public void run() {
            final long refreshLayoutPeriodMs = 300;
            final List<BluetoothDevice> currentDevices = new ArrayList<>(devices);
            final LinearLayout ll = findViewById(R.id.conf_linear_layout_devices);
            
            if (ll == null) {
                lastLayout = null;
                threadHandler.postDelayed(this, refreshLayoutPeriodMs);
                return;
            }

            if (ll == lastLayout && currentDevices.equals(lastDevices) && ll.getChildCount() > 0) {
                threadHandler.postDelayed(this, refreshLayoutPeriodMs);
                return;
            }

            lastDevices = currentDevices;
            lastLayout = ll;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ll.removeAllViews();
                    android.view.LayoutInflater inflater = android.view.LayoutInflater.from(MainActivity.this);
                    for (BluetoothDevice bd : currentDevices) {
                        View itemView = inflater.inflate(R.layout.device_item, ll, false);
                        TextView infoView = itemView.findViewById(R.id.device_item_info);
                        androidx.appcompat.widget.SwitchCompat switchView = itemView.findViewById(R.id.device_item_switch);

                        String name = "Unknown Device";
                        try {
                            name = bd.getName() != null ? bd.getName() : "Unknown Device";
                        } catch (SecurityException ignored) {}

                        if (infoView != null) {
                            infoView.setText(name + "\n" + bd.getAddress());
                        }

                        if (switchView != null) {
                            switchView.setOnCheckedChangeListener(null);
                            switchView.setChecked(isDeviceEnabled(bd));
                            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                setDeviceEnabled(bd, isChecked);
                                threadHandler.removeCallbacks(deviceFieldUpdate);
                                threadHandler.post(deviceFieldUpdate);
                            });
                        }
                        ll.addView(itemView);
                    }
                }
            });

            threadHandler.postDelayed(this, refreshLayoutPeriodMs);
        }
    };

    private final Runnable deviceFieldUpdate = new Runnable() {
        private int i = 0;

        public void run() {
            final long refreshTextViewPeriodMs = 1200;
            final List<BluetoothDevice> currentDevices = getEnabledDevices();
            
            String text;
            int text_color;
            int bg_color;

            if (!currentDevices.isEmpty()) {
                if (i >= currentDevices.size()) i = 0;
                BluetoothDevice d = currentDevices.get(i);
                String name = "Unknown Device";
                try {
                    name = d.getName() != null ? d.getName() : "Unknown Device";
                } catch (SecurityException ignored) {}
                text = name + "  (" + d.getAddress() + ")";
                text_color = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
                bg_color = 0; // Not used but kept for signature

                if (currentDevices.size() > 1) {
                    text += " [" + (i + 1) + " / " + currentDevices.size() + "]";
                }
                i = (i + 1) % currentDevices.size();
            } else {
                text = getString(R.string.device_not_connected) + " ";
                text_color = ContextCompat.getColor(getApplicationContext(), R.color.colorGrey);
                bg_color = 0;
                i = 0;
            }

            updateDeviceInfoField(text, text_color, bg_color);
            threadHandler.postDelayed(this, refreshTextViewPeriodMs);
        }
    };

    private HidBleService mService = null;
    private byte lastKeyboardLedStatus = 0;
    private int absMouseButtons = 0;
    private int lastAbsX = 16384, lastAbsY = 16384;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            HidBleService.LocalBinder binder = (HidBleService.LocalBinder) service;
            mService = binder.getService();
            mService.setActivity(MainActivity.this);
            devices = mService.getDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Older Android versions
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void updateDeviceInfoField(final String text, final int text_color, final int bg_color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int[] ids = {
                        R.id.cons_text_device_info, R.id.mouse_text_device_info,
                        R.id.keyboard_text_device_info, R.id.gamepad_text_device_info,
                        R.id.abs_mouse_text_device_info, R.id.conf_text_device_info
                };

                for (int id : ids) {
                    TextView textView = findViewById(id);
                    if (textView == null) continue;
                    textView.setVisibility(View.VISIBLE);
                    // textView.setBackgroundColor(bg_color); // Removed to keep custom drawable
                    textView.setTextColor(text_color);
                    textView.setText(text);
                }
            }
        });
    }

    public void onConnectionStateChange(List<BluetoothDevice> dev_array) {
        devices = new ArrayList<>(dev_array);
    }

    public void saveBlocklyToFile(String encodedXml) {
        try {
            // Decode from Base64
            byte[] data = android.util.Base64.decode(encodedXml, android.util.Base64.DEFAULT);
            String xml = new String(data, "UTF-8");

            java.io.File file = new java.io.File(getExternalFilesDir(null), "macro_autosave.xml");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(xml);
            writer.close();
            Toast.makeText(this, R.string.blockly_macro_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Blockly", "Save failed", e);
        }
    }

    public void openFilePickerForBlockly() {
        try {
            java.io.File file = new java.io.File(getExternalFilesDir(null), "macro_autosave.xml");
            if (file.exists()) {
                StringBuilder text = new StringBuilder();
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
                Fragment f = pagerAdapter.getRegisteredFragment(pager.getCurrentItem());
                if (f instanceof BlocklyFragment) {
                    ((BlocklyFragment) f).loadBlocksToWorkspace(text.toString());
                }
            } else {
                Toast.makeText(this, R.string.blockly_no_macro, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Blockly", "Load failed", e);
        }
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

    public int getAbsMouseButtons() {
        return absMouseButtons;
    }

    public void sendAbsMouseReport(int buttons, int x, int y) {
        lastAbsX = x;
        lastAbsY = y;
        if (mService != null) {
            mService.sendAbsMouseNotification(buttons, x, y);
        }
    }

    public void exitAbsoluteMouseMode() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pager != null) {
                    pager.swipePossible(true);
                    pager.setCurrentItem(PagerAdapter.MOUSE);
                }
            }
        });
    }

    public void sendKeyboardNotification(ReportField rf, int value) {
        if (mService != null) mService.sendKeyboardNotification(rf, value, SendTo.SEND_TO_ALL);
    }

    public void sendMouseNotification(ReportField rf, int value) {
        if (mService != null) mService.sendMouseNotification(rf, value, SendTo.SEND_TO_ALL);
    }

    public void sendConsumerNotification(ReportField rf, int value) {
        if (mService != null) mService.sendConsumerNotification(rf, value, SendTo.SEND_TO_ALL);
    }

    public void sendGamepadNotification(int buttons, int lx, int ly, int rx, int ry) {
        if (mService != null) mService.sendGamepadNotification(buttons, lx, ly, rx, ry);
    }

    public void sendKeyboardText(String s) {
        if (mService != null) {
            mService.sendKeyboardText(s);
        }
    }

    public void restartGattDatabase() {
        if (mService != null) mService.initializeLE();
    }

    public void clearDeviceConfigs() {
        if (mService != null) mService.clearDeviceConfigs();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        checkPermissions();
        
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        pager = findViewById(R.id.viewPager);
        pager.setAdapter(pagerAdapter);
        
        // Restore last used fragment
        int lastTab = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getInt("last_tab", 1);
        pager.setCurrentItem(lastTab);
        
        pager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                // Save last used fragment
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putInt("last_tab", position).apply();

                if (position == PagerAdapter.ABS_MOUSE) {
                    pager.swipePossible(false);
                } else if (position == PagerAdapter.GAMEPAD) {
                    boolean lockChecked = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_lock", false);
                    pager.swipePossible(!lockChecked);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    pager.swipePossible(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        Intent intent = new Intent(this, HidBleService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        threadHandler.post(deviceLayoutUpdate);
        threadHandler.post(deviceFieldUpdate);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (pager.getCurrentItem() == PagerAdapter.ABS_MOUSE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (event.getRepeatCount() == 0) {
                    absMouseButtons |= 0x01;
                    sendAbsMouseReport(absMouseButtons, lastAbsX, lastAbsY);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (event.getRepeatCount() == 0) {
                    absMouseButtons |= 0x02;
                    sendAbsMouseReport(absMouseButtons, lastAbsX, lastAbsY);
                }
                return true;
            }
        } else if (pager.getCurrentItem() == PagerAdapter.GAMEPAD) {
            Fragment f = pagerAdapter.getRegisteredFragment(PagerAdapter.GAMEPAD);
            if (f instanceof GamepadFragment) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (event.getRepeatCount() == 0) ((GamepadFragment) f).onExternalButtonEvent(0x400, true);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if (event.getRepeatCount() == 0) ((GamepadFragment) f).onExternalButtonEvent(0x800, true);
                    return true;
                }
            }
        } else if (pager.getCurrentItem() == PagerAdapter.MOUSE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_SCROLL, 1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_SCROLL, -1);
                return true;
            }
        }

        // Optimized keyboard mapping logic
        byte key = 0;
        byte meta_key = 0;
        for (int i = 0; i < KeyboardUsage.KEYBOARD_USAGES.length; i++) {
            if (keyCode == KeyboardUsage.KEYBOARD_USAGES[i].key_code) {
                key = KeyboardUsage.KEYBOARD_USAGES[i].usage;
                meta_key = KeyboardUsage.KEYBOARD_USAGES[i].meta;
                break;
            }
        }

        if (key != 0) {
            if (event.isCtrlPressed()) meta_key |= KeyboardUsage.META_LEFT_CONTROL;
            if (event.isShiftPressed()) meta_key |= KeyboardUsage.META_LEFT_SHIFT;
            if (event.isAltPressed()) meta_key |= KeyboardUsage.META_LEFT_ALT;
            if (event.isMetaPressed()) meta_key |= KeyboardUsage.META_LEFT_META;
            
            short val = (short) (meta_key + (key << 8));
            sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, (int) val);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (pager.getCurrentItem() == PagerAdapter.ABS_MOUSE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                absMouseButtons &= ~0x01;
                sendAbsMouseReport(absMouseButtons, lastAbsX, lastAbsY);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                absMouseButtons &= ~0x02;
                sendAbsMouseReport(absMouseButtons, lastAbsX, lastAbsY);
                return true;
            }
        } else if (pager.getCurrentItem() == PagerAdapter.GAMEPAD) {
            Fragment f = pagerAdapter.getRegisteredFragment(PagerAdapter.GAMEPAD);
            if (f instanceof GamepadFragment) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    ((GamepadFragment) f).onExternalButtonEvent(0x400, false);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    ((GamepadFragment) f).onExternalButtonEvent(0x800, false);
                    return true;
                }
            }
        } else if (pager.getCurrentItem() == PagerAdapter.MOUSE) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return true;
            }
        }
        
        // Reset keyboard state on release
        for (int i = 0; i < KeyboardUsage.KEYBOARD_USAGES.length; i++) {
            if (keyCode == KeyboardUsage.KEYBOARD_USAGES[i].key_code) {
                sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
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
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(p);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 2);
        } else {
            checkBluetoothEnabled();
        }
    }

    private void checkBluetoothEnabled() {
        android.bluetooth.BluetoothManager bm = (android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        android.bluetooth.BluetoothAdapter ba = bm != null ? bm.getAdapter() : null;
        if (ba != null && !ba.isEnabled()) {
            Intent intent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                startActivityForResult(intent, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            restartGattDatabase();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            boolean allGranted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) {
                checkBluetoothEnabled();
                restartGattDatabase();
            }
        }
    }

    @Override
    protected void onDestroy() {
        threadHandler.removeCallbacks(deviceLayoutUpdate);
        threadHandler.removeCallbacks(deviceFieldUpdate);
        if (mService != null) unbindService(mConnection);
        super.onDestroy();
    }

    class PagerAdapter extends FragmentPagerAdapter {
        static final int BLOCKLY = 0;
        static final int CONFIGURATION = 1;
        static final int CONSUMER = 2;
        static final int MOUSE = 3;
        static final int KEYBOARD = 4;
        static final int GAMEPAD = 5;
        static final int ABS_MOUSE = 6;
        
        private final SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public PagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case BLOCKLY: return BlocklyFragment.newInstance();
                case CONFIGURATION: return ConfigurationFragment.newInstance();
                case CONSUMER: return ConsumerFragment.newInstance();
                case MOUSE: return MouseFragment.newInstance();
                case KEYBOARD: return KeyboardFragment.newInstance();
                case GAMEPAD: return GamepadFragment.newInstance();
                case ABS_MOUSE: return AbsoluteMouseFragment.newInstance();
                default: return BlocklyFragment.newInstance();
            }
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
        public int getCount() {
            return 7;
        }
    }
}
