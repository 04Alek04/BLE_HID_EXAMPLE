package com.example.alek.ble_hid_example;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class BlocklyInterfaceTest {

    private BlocklyInterface blocklyInterface;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        MainActivity activity = new MainActivity();
        blocklyInterface = new BlocklyInterface(activity);
    }

    @Test
    public void testAllJavaScriptBridgeMethods_executeWithoutExceptions() {
        assertNotNull(blocklyInterface);

        // 1. Keyboard
        blocklyInterface.sendKeyboardKey(0x04, 0x02); // Usage 'a', Shift
        blocklyInterface.typeText("Hello BLE");
        assertFalse(blocklyInterface.getCapsLockState());
        assertFalse(blocklyInterface.getNumLockState());
        assertFalse(blocklyInterface.getScrollLockState());

        // 2. Relative Mouse
        blocklyInterface.moveMouse(10, -5);
        blocklyInterface.scrollMouse(2);
        blocklyInterface.panMouse(-1);
        blocklyInterface.setMouseButtons(1);
        blocklyInterface.mouseClick(1);

        // 3. Absolute Mouse
        blocklyInterface.sendAbsoluteMouse(true, 16384, 16384);

        // 4. Consumer / Media
        blocklyInterface.sendConsumerKey(0xCD); // Play/Pause
        blocklyInterface.sendConsumerKey(0x0182); // App Launch
        blocklyInterface.sendConsumerKey(0x0201); // App Control

        // 5. Gamepad
        blocklyInterface.sendGamepadButton(1, true);
        blocklyInterface.sendGamepadStick(1, 50, -50);
        blocklyInterface.sendGamepad(1, 0, 0, 0, 0);

        // 6. Logging & Safety Release
        blocklyInterface.log("Test log bridge");
        blocklyInterface.releaseAll();
    }
}
