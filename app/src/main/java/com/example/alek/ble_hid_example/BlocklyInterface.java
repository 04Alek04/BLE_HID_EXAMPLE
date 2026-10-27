package com.example.alek.ble_hid_example;

import android.webkit.JavascriptInterface;
import android.util.Log;
import androidx.fragment.app.Fragment;

public class BlocklyInterface {
    private MainActivity activity;

    public BlocklyInterface(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void sendKeyboardKey(int usage, int modifiers) {
        int val = (modifiers & 0xFF) | ((usage & 0xFF) << 8);
        activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, val);
        Log.d("Blockly", "Key Sent: Usage=" + usage + ", Modifiers=" + modifiers);
    }

    @JavascriptInterface
    public void typeText(String text) {
        Log.d("Blockly", "Typing text: " + text);
        if (text != null) {
            activity.sendKeyboardText(text);
        }
    }

    @JavascriptInterface
    public void moveMouse(int x, int y) {
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_XY, (x & 0xFF) | ((y & 0xFF) << 8));
    }

    @JavascriptInterface
    public void scrollMouse(int steps) {
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_SCROLL, steps);
    }

    @JavascriptInterface
    public void panMouse(int steps) {
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_HSCROLL, steps);
    }

    @JavascriptInterface
    public void setMouseButtons(int buttons) {
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, buttons);
    }

    @JavascriptInterface
    public void mouseClick(int buttons) {
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, buttons);
        // Auto-release after click for simplicity in blocks
        try { Thread.sleep(HidConstants.BLOCKLY_CLICK_HOLD_MS); } catch (InterruptedException e) {}
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0);
    }

    @JavascriptInterface
    public void sendConsumerKey(int usage) {
        ReportField rf = ReportField.REPORT_FIELD_CONSUMER_CONTROL;
        int value = 0;

        // Route to the correct field based on Usage ID
        if (usage >= 0x0201) {
            // App Control Array (bytes 4-5)
            rf = ReportField.REPORT_FIELD_CONTROL_BUTTON;
            value = usage;
        } else if (usage >= 0x0181) {
            // App Launch Array (bytes 2-3)
            rf = ReportField.REPORT_FIELD_LAUNCHER_BUTTON;
            value = usage;
        } else {
            // Bitmask field (bytes 0-1)
            value = getConsumerBitmask(usage);
        }

        Log.d("Blockly", "Consumer Notification: Usage=" + usage + " -> Field=" + rf + " Value=" + value);
        activity.sendConsumerNotification(rf, value);
        try { Thread.sleep(HidConstants.BLOCKLY_CLICK_HOLD_MS); } catch (InterruptedException e) {}
        activity.sendConsumerNotification(rf, 0);
    }

    private int getConsumerBitmask(int usage) {
        switch (usage) {
            case 0x6F: return 0x0001; // Bright Up
            case 0x70: return 0x0002; // Bright Down
            case 0xB5: return 0x0004; // Scan Next Track
            case 0xB6: return 0x0008; // Scan Previous Track
            case 0xB7: return 0x0010; // Stop
            case 0xCD: return 0x0020; // Play/Pause
            case 0xE2: return 0x0040; // Mute
            case 0xE9: return 0x0080; // Volume Up
            case 0xEA: return 0x0100; // Volume Down
            case 0xB8: return 0x0200; // Eject
            case 0x65: return 0x0400; // Snapshot
            case 0x82: return 0x0800; // System Sleep
            case 0xA8: return 0x1000; // System Hibernate
            case 0x81: return 0x2000; // System Power Down
            case 0x8E: return 0x4000; // System Cold Restart
            case 0x8F: return 0x8000; // System Warm Restart
            default: return 0;
        }
    }

    @JavascriptInterface
    public boolean getCapsLockState() {
        return (activity.getLastKeyboardLedStatus() & HidKeyCodes.LED_CAPS_LOCK) != 0;
    }

    @JavascriptInterface
    public boolean getNumLockState() {
        return (activity.getLastKeyboardLedStatus() & HidKeyCodes.LED_NUM_LOCK) != 0;
    }

    @JavascriptInterface
    public boolean getScrollLockState() {
        return (activity.getLastKeyboardLedStatus() & HidKeyCodes.LED_SCROLL_LOCK) != 0;
    }

    private int currentGamepadButtons = 0;
    private int currentGamepadLX = 0;
    private int currentGamepadLY = 0;
    private int currentGamepadRX = 0;
    private int currentGamepadRY = 0;

    @JavascriptInterface
    public void sendGamepadButton(int buttonIndex, boolean pressed) {
        if (pressed) {
            currentGamepadButtons |= (1 << (buttonIndex - 1));
        } else {
            currentGamepadButtons &= ~(1 << (buttonIndex - 1));
        }
        activity.sendGamepadNotification(currentGamepadButtons, currentGamepadLX, currentGamepadLY, currentGamepadRX, currentGamepadRY);
    }

    @JavascriptInterface
    public void sendGamepadStick(int stickIndex, int x, int y) {
        if (stickIndex == 1) { // Left Stick
            currentGamepadLX = x;
            currentGamepadLY = y;
        } else { // Right Stick
            currentGamepadRX = x;
            currentGamepadRY = y;
        }
        activity.sendGamepadNotification(currentGamepadButtons, currentGamepadLX, currentGamepadLY, currentGamepadRX, currentGamepadRY);
    }

    @JavascriptInterface
    public void sendGamepad(int buttons, int lx, int ly, int rx, int ry) {
        this.currentGamepadButtons = buttons;
        this.currentGamepadLX = lx;
        this.currentGamepadLY = ly;
        this.currentGamepadRX = rx;
        this.currentGamepadRY = ry;
        activity.sendGamepadNotification(buttons, lx, ly, rx, ry);
    }

    @JavascriptInterface
    public void sendAbsoluteMouse(boolean touching, int x, int y) {
        activity.sendAbsMouseReport(touching ? 0x01 : 0x00, x, y);
    }

    @JavascriptInterface
    public void saveBlocks(String encodedXml) {
        Log.i("Blockly", "Bridge: Received encoded XML for saving. Length: " + (encodedXml != null ? encodedXml.length() : 0));
        activity.saveBlocklyToFile(encodedXml);
    }

    @JavascriptInterface
    public void onScriptFinished() {
        releaseAll();
        activity.runOnUiThread(() -> {
            Fragment f = activity.pagerAdapter.getRegisteredFragment(activity.pager.getCurrentItem());
            if (f instanceof BlocklyFragment) {
                ((BlocklyFragment) f).onScriptFinished();
            }
        });
    }

    @JavascriptInterface
    public void releaseAll() {
        Log.d("Blockly", "Safety Release: Clearing all HID states and pending queue");
        activity.clearPendingNotifications();
        // Release Keyboard
        activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
        // Release Mouse Buttons
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0);
        // Release Absolute Mouse button ONLY if a button is currently pressed
        if (activity.getAbsMouseButtons() != 0) {
            activity.sendAbsMouseReport(0, activity.getLastAbsX(), activity.getLastAbsY());
        }
        // Release Gamepad Buttons and reset sticks
        activity.sendGamepadNotification(0, 0, 0, 0, 0);
        currentGamepadButtons = 0;
        currentGamepadLX = 0;
        currentGamepadLY = 0;
        currentGamepadRX = 0;
        currentGamepadRY = 0;
        // Release Consumer keys (all fields)
        activity.sendConsumerNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, 0);
        activity.sendConsumerNotification(ReportField.REPORT_FIELD_LAUNCHER_BUTTON, 0);
        activity.sendConsumerNotification(ReportField.REPORT_FIELD_CONTROL_BUTTON, 0);
    }

    @JavascriptInterface
    public void log(String msg) {
        Log.i("BlocklyJS", msg);
    }
}
