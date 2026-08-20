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
        Log.d("Blockly", "Key Sent: " + usage);
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
    public void mouseClick(int buttons) {
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, buttons);
        // Auto-release after click for simplicity in blocks
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0);
    }

    @JavascriptInterface
    public void sendConsumerKey(int usage) {
        activity.sendConsumerNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, usage);
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        activity.sendConsumerNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, 0);
    }

    @JavascriptInterface
    public boolean getCapsLockState() {
        return (activity.getLastKeyboardLedStatus() & 0x02) != 0;
    }

    @JavascriptInterface
    public boolean getNumLockState() {
        return (activity.getLastKeyboardLedStatus() & 0x01) != 0;
    }

    @JavascriptInterface
    public boolean getScrollLockState() {
        return (activity.getLastKeyboardLedStatus() & 0x04) != 0;
    }

    private int currentGamepadButtons = 0;

    @JavascriptInterface
    public void sendGamepadButton(int buttonIndex, boolean pressed) {
        if (pressed) {
            currentGamepadButtons |= (1 << (buttonIndex - 1));
        } else {
            currentGamepadButtons &= ~(1 << (buttonIndex - 1));
        }
        activity.sendGamepadNotification(currentGamepadButtons, 0, 0);
    }

    @JavascriptInterface
    public void sendGamepad(int buttons, int x, int y) {
        this.currentGamepadButtons = buttons;
        activity.sendGamepadNotification(buttons, x, y);
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
        Log.d("Blockly", "Safety Release: Clearing all HID states");
        // Release Keyboard
        activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
        // Release Mouse Buttons
        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0);
        // Release Gamepad Buttons and reset sticks
        activity.sendGamepadNotification(0, 0, 0);
        currentGamepadButtons = 0;
        // Release Consumer keys
        activity.sendConsumerNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, 0);
    }

    @JavascriptInterface
    public void log(String msg) {
        Log.i("BlocklyJS", msg);
    }
}
