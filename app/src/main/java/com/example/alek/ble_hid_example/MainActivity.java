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
import android.os.Looper;
import android.os.IBinder;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alek.ble_hid_example.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements KeyEvent.Callback {
    private ActivityMainBinding binding;
    private final Handler threadHandler = new Handler(Looper.getMainLooper());
    public ViewPager2 pager = null;
    public PagerAdapter pagerAdapter = null;
    private volatile List<BluetoothDevice> devices = new ArrayList<>();

    public void swipePossible(boolean enable) {
        if (pager != null) {
            pager.setUserInputEnabled(enable);
        }
    }
    
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
            final long refreshLayoutPeriodMs = HidConstants.DEVICE_LAYOUT_REFRESH_MS;
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
            final long refreshTextViewPeriodMs = HidConstants.DEVICE_FIELD_REFRESH_MS;
            final List<BluetoothDevice> currentDevices = getEnabledDevices();
            
            String text;
            int text_color;

            if (!currentDevices.isEmpty()) {
                if (i >= currentDevices.size()) i = 0;
                BluetoothDevice d = currentDevices.get(i);
                String name = "Unknown Device";
                try {
                    name = d.getName() != null ? d.getName() : "Unknown Device";
                } catch (SecurityException ignored) {}
                text = name + "  (" + d.getAddress() + ")";
                text_color = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);

                if (currentDevices.size() > 1) {
                    text += " [" + (i + 1) + " / " + currentDevices.size() + "]";
                }
                i = (i + 1) % currentDevices.size();
            } else {
                text = getString(R.string.device_not_connected) + " ";
                text_color = ContextCompat.getColor(getApplicationContext(), R.color.colorGrey);
                i = 0;
            }

            updateDeviceInfoField(text, text_color);
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
        Window window = getWindow();
        window.setDecorFitsSystemWindows(false);
        WindowInsetsController controller = window.getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private DeviceStatusBarHelper statusBarHelper;

    private void updateDeviceInfoField(final String text, final int text_color) {
        if (statusBarHelper == null) {
            statusBarHelper = new DeviceStatusBarHelper(this);
        }
        statusBarHelper.updateStatusText(text, text_color);
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
                    for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
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
                    swipePossible(true);
                    pager.setCurrentItem(PagerAdapter.MOUSE, false);
                }
            }
        });
    }

    public void sendKeyboardNotification(ReportField rf, int value) {
        if (mService != null) mService.sendKeyboardNotification(rf, value);
    }

    public void sendMouseNotification(ReportField rf, int value) {
        if (mService != null) mService.sendMouseNotification(rf, value);
    }

    public void sendConsumerNotification(ReportField rf, int value) {
        if (mService != null) mService.sendConsumerNotification(rf, value);
    }

    public void sendGamepadNotification(int buttons, int lx, int ly, int rx, int ry) {
        if (mService != null) mService.sendGamepadNotification(buttons, lx, ly, rx, ry);
    }

    public void sendKeyboardText(String s) {
        if (mService != null) {
            mService.sendKeyboardText(s);
        }
    }

    public void sendKeyboardText(String s, Runnable onComplete) {
        if (mService != null) {
            mService.sendKeyboardText(s, onComplete);
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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hideSystemUI();

        checkPermissions();
        
        pagerAdapter = new PagerAdapter(this);
        pager = binding.viewPager;
        pager.setAdapter(pagerAdapter);
        pager.setOffscreenPageLimit(6);
        
        AppPreferencesManager prefsManager = AppPreferencesManager.getInstance(this);
        int lastTab = prefsManager.getLastTab();
        pager.setCurrentItem(lastTab, false);
        
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                prefsManager.setLastTab(position);

                if (position == PagerAdapter.ABS_MOUSE) {
                    swipePossible(false);
                } else if (position == PagerAdapter.GAMEPAD) {
                    swipePossible(!prefsManager.isGamepadLockEnabled());
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    swipePossible(true);
                }
            }
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
        KeyboardUsage usage = KeyboardUsage.findByKeyCode(keyCode);
        if (usage != null) {
            byte key = usage.usage;
            byte meta_key = usage.meta;

            if (event.isCtrlPressed()) meta_key |= KeyboardUsage.META_LEFT_CONTROL;
            if (event.isShiftPressed()) meta_key |= KeyboardUsage.META_LEFT_SHIFT;
            if (event.isAltPressed()) meta_key |= KeyboardUsage.META_LEFT_ALT;
            if (event.isMetaPressed()) meta_key |= KeyboardUsage.META_LEFT_META;
            
            short val = (short) (meta_key + (key << 8));
            sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, val);
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
        if (KeyboardUsage.findByKeyCode(keyCode) != null) {
            sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    restartGattDatabase();
                }
            });

    private PermissionManager permissionManager;

    private void checkPermissions() {
        if (permissionManager == null) {
            permissionManager = new PermissionManager(this);
        }
        permissionManager.checkAndRequestPermissions(() -> permissionManager.checkBluetoothEnabled(enableBluetoothLauncher));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted && permissionManager != null) {
                permissionManager.checkBluetoothEnabled(enableBluetoothLauncher);
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

    class PagerAdapter extends FragmentStateAdapter {
        static final int BLOCKLY = 0;
        static final int CONFIGURATION = 1;
        static final int CONSUMER = 2;
        static final int MOUSE = 3;
        static final int KEYBOARD = 4;
        static final int GAMEPAD = 5;
        static final int ABS_MOUSE = 6;

        private final SparseArray<Fragment> fragments = new SparseArray<>();

        public PagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment f;
            switch (position) {
                case BLOCKLY: f = BlocklyFragment.newInstance(); break;
                case CONFIGURATION: f = ConfigurationFragment.newInstance(); break;
                case CONSUMER: f = ConsumerFragment.newInstance(); break;
                case MOUSE: f = MouseFragment.newInstance(); break;
                case KEYBOARD: f = KeyboardFragment.newInstance(); break;
                case GAMEPAD: f = GamepadFragment.newInstance(); break;
                case ABS_MOUSE: f = AbsoluteMouseFragment.newInstance(); break;
                default: f = BlocklyFragment.newInstance(); break;
            }
            fragments.put(position, f);
            return f;
        }

        public Fragment getRegisteredFragment(int position) {
            Fragment f = fragments.get(position);
            if (f != null && f.isAdded()) return f;
            return getSupportFragmentManager().findFragmentByTag("f" + position);
        }

        @Override
        public int getItemCount() {
            return 7;
        }
    }
}
