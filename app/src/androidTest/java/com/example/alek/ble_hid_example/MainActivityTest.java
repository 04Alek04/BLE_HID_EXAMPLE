package com.example.alek.ble_hid_example;

import android.view.KeyEvent;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainActivityTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.POST_NOTIFICATIONS
    );

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void test01_ViewPagerAndTabsLaunch() {
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
    }

    @Test
    public void test02_MouseFragment_GesturesTouchpadAndScroll() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.MOUSE, false));

        // Click buttons MB1..MB5
        onView(withId(R.id.mouse_button_mouse_1)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.mouse_button_mouse_2)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.mouse_button_mouse_3)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.mouse_button_mouse_4)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.mouse_button_mouse_5)).check(matches(isDisplayed())).perform(click());

        // Perform real gestures on Touchpad surface & Vertical Scrollbar
        onView(withId(R.id.mouse_mouse_cursor)).check(matches(isDisplayed())).perform(swipeRight()).perform(swipeDown());
        onView(withId(R.id.mouse_h_scroll)).check(matches(isDisplayed())).perform(swipeUp()).perform(swipeDown());
        onView(withId(R.id.mouse_text_device_info)).check(matches(isDisplayed()));

        // Test Physical Volume Buttons in Mouse mode (Scroll UP/DOWN)
        onView(withId(R.id.viewPager)).perform(pressKey(KeyEvent.KEYCODE_VOLUME_UP));
        onView(withId(R.id.viewPager)).perform(pressKey(KeyEvent.KEYCODE_VOLUME_DOWN));
    }

    @Test
    public void test03_KeyboardFragment_SpinnerAndShortcuts() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.KEYBOARD, false));

        onView(withId(R.id.keyboard_sub_main_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.keyboard_edit_text)).check(matches(isDisplayed())).perform(typeText("Test"));
        onView(withId(R.id.keyboard_button_send)).check(matches(isDisplayed())).perform(click());

        // Click Character Spinner and select item
        onView(withId(R.id.keyboard_spinner_character)).check(matches(isDisplayed())).perform(click());
        onData(anything()).atPosition(0).perform(click());

        onView(withId(R.id.keyboard_toggle_kb)).check(matches(isDisplayed()));

        // Locks
        onView(withId(R.id.keyboard_lock_num)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.keyboard_lock_caps)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.keyboard_lock_scroll)).check(matches(isDisplayed())).perform(click());

        // Shortcuts
        onView(withId(R.id.kb_short_undo)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_redo)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_save)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_paste)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_cut)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_copy)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_all)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_find)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.kb_short_replace)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.keyboard_text_device_info)).check(matches(isDisplayed()));
    }

    @Test
    public void test04_ConsumerFragment_SpinnersAndMedia() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.CONSUMER, false));

        onView(withId(R.id.cons_sub_main_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.cons_system_text)).check(matches(isDisplayed()));
        onView(withId(R.id.cons_system_button_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.cons_launch_text)).check(matches(isDisplayed()));
        onView(withId(R.id.cons_control_text)).check(matches(isDisplayed()));

        // Media Controls
        onView(withId(R.id.cons_button_previous)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_stop)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_start_stop)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_next)).check(matches(isDisplayed())).perform(click());

        // Volume & Brightness
        onView(withId(R.id.cons_button_vol_plus)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_vol_minus)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_mute)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_bright_plus)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_bright_minus)).check(matches(isDisplayed())).perform(click());

        // System Controls
        onView(withId(R.id.cons_button_system_sleep)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_system_hibernation)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_system_power_down)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_system_restart)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_eject)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.cons_button_snapshot)).check(matches(isDisplayed())).perform(click());

        // Interacting with App Launch & App Control Spinners
        onView(withId(R.id.cons_spinner_al)).check(matches(isDisplayed())).perform(click());
        onData(anything()).atPosition(0).perform(click());

        onView(withId(R.id.cons_spinner_ac)).check(matches(isDisplayed())).perform(click());
        onData(anything()).atPosition(0).perform(click());

        onView(withId(R.id.cons_text_device_info)).check(matches(isDisplayed()));
    }

    @Test
    public void test05_GamepadFragment_SticksButtonsDpadAndToggles() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.GAMEPAD, false));

        // 1. Verify Triggers, Bumpers, Menu, Start
        onView(withId(R.id.gamepad_btn_lb)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_lt)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_rb)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_rt)).check(matches(notNullValue()));

        // 2. Toggle to D-Pad mode and test D-Pad buttons
        onView(withId(R.id.gamepad_l_stick_toggle)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_d_up)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_d_left)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_d_right)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_d_down)).check(matches(notNullValue()));

        // 3. Toggle to Face Buttons mode and test A, B, X, Y, R3
        onView(withId(R.id.gamepad_r_stick_toggle)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_an_r_up)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_an_r_left)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_an_r_right)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_an_r_down)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_btn_r3)).check(matches(notNullValue()));

        // 4. Test Touchpad Toggle & Touchpad Gestures
        onView(withId(R.id.gamepad_touch_toggle)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_touchpad)).check(matches(notNullValue()));

        // 5. Test Lock Toggle & Info
        onView(withId(R.id.gamepad_lock_toggle)).check(matches(notNullValue()));
        onView(withId(R.id.gamepad_text_device_info)).check(matches(notNullValue()));

        // 6. Test Physical Volume Buttons in Gamepad mode
        onView(withId(R.id.viewPager)).perform(pressKey(KeyEvent.KEYCODE_VOLUME_UP));
        onView(withId(R.id.viewPager)).perform(pressKey(KeyEvent.KEYCODE_VOLUME_DOWN));
    }

    @Test
    public void test06_AbsoluteMouseFragment_SurfaceGesturesAndVolume() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.ABS_MOUSE, false));

        onView(withId(R.id.abs_mouse_root)).check(matches(isDisplayed()));
        onView(withId(R.id.abs_mouse_touch_surface)).check(matches(isDisplayed())).perform(click()).perform(swipeRight()).perform(swipeDown());
        onView(withId(R.id.abs_mouse_debug)).check(matches(notNullValue()));
        onView(withId(R.id.abs_mouse_text_device_info)).check(matches(notNullValue()));

        // Test Physical Volume Buttons in Absolute Mouse mode
        onView(withId(R.id.viewPager)).perform(pressKey(KeyEvent.KEYCODE_VOLUME_UP));
        onView(withId(R.id.viewPager)).perform(pressKey(KeyEvent.KEYCODE_VOLUME_DOWN));
    }

    @Test
    public void test07_BlocklyFragment_ScriptControls() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.BLOCKLY, false));

        onView(withId(R.id.btn_run_script)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_clear_blocks)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_save_blocks)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_load_blocks)).check(matches(isDisplayed()));
        onView(withId(R.id.blockly_webview)).check(matches(isDisplayed()));
    }

    @Test
    public void test08_ConfigurationFragment_Settings() {
        activityRule.getScenario().onActivity(activity -> activity.pager.setCurrentItem(MainActivity.PagerAdapter.CONFIGURATION, false));

        onView(withId(R.id.conf_reboot_button)).check(matches(isDisplayed()));
        onView(withId(R.id.conf_start_adv_button)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.conf_clear_configs_button)).check(matches(isDisplayed()));
        onView(withId(R.id.conf_restore_name_switch)).check(matches(isDisplayed())).perform(click()).perform(click());
        onView(withId(R.id.conf_linear_layout_devices)).check(matches(notNullValue()));
        onView(withId(R.id.conf_text_device_info)).check(matches(isDisplayed()));
    }
}
