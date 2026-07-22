package com.example.alek.ble_hid_example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class GamepadFragment extends Fragment {

    private int buttonsState = 0;
    private int analogX = 0;
    private int analogY = 0;

    public static GamepadFragment newInstance() {
        return new GamepadFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gamepad_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();
        final SwitchMaterial lockSwitch = view.findViewById(R.id.gamepad_lock_switch);

        // Global touch listener to block ViewPager swipe on background areas
        View.OnTouchListener swipeBlocker = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (activity != null && activity.pager != null && lockSwitch != null && lockSwitch.isChecked()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                        activity.pager.swipePossible(false);
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (event.getPointerCount() <= 1) {
                            activity.pager.swipePossible(true);
                        }
                    }
                    return true;
                }
                return false; 
            }
        };

        view.setOnTouchListener(swipeBlocker);
        
        View touchSurface = view.findViewById(R.id.gamepad_touch_surface);
        if (touchSurface != null) touchSurface.setOnTouchListener(swipeBlocker);
        
        View buttonsContainer = view.findViewById(R.id.gamepad_buttons_container);
        if (buttonsContainer != null) buttonsContainer.setOnTouchListener(swipeBlocker);

        // Buttons Mapping (Standard HID Mapping - Adjusted for 90deg rotation)
        setupButton(view, R.id.gamepad_btn_4, 0x01, activity); // Visual Down (Portrait Left) -> A
        setupButton(view, R.id.gamepad_btn_3, 0x02, activity); // Visual Right (Portrait Bottom) -> B
        setupButton(view, R.id.gamepad_btn_1, 0x04, activity); // Visual Left (Portrait Top) -> X
        setupButton(view, R.id.gamepad_btn_2, 0x08, activity); // Visual Up (Portrait Right) -> Y
        
        setupButton(view, R.id.gamepad_btn_lb, 0x10, activity); // L1 / LB
        setupButton(view, R.id.gamepad_btn_rb, 0x20, activity); // R1 / RB
        setupButton(view, R.id.gamepad_btn_lt, 0x40, activity); // L2 / LT
        setupButton(view, R.id.gamepad_btn_rt, 0x80, activity); // R2 / RT

        setupButton(view, R.id.gamepad_btn_menu, 0x100, activity);
        setupButton(view, R.id.gamepad_btn_start, 0x200, activity);

        // Analog Stick logic
        final View stick = view.findViewById(R.id.gamepad_left_analog_stick);
        final View base = view.findViewById(R.id.gamepad_left_analog_base);

        if (base != null) {
            base.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (activity == null) return false;
                    
                    if (lockSwitch != null && lockSwitch.isChecked()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        if (activity.pager != null) activity.pager.swipePossible(false);
                    }

                    float centerX = v.getWidth() / 2f;
                    float centerY = v.getHeight() / 2f;
                    float radius = v.getWidth() / 2f;

                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                        float dx = event.getX() - centerX;
                        float dy = event.getY() - centerY;
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);

                        if (distance > radius) {
                            dx = (dx / distance) * radius;
                            dy = (dy / distance) * radius;
                        }

                        stick.setTranslationX(dx);
                        stick.setTranslationY(dy);

                        // Convert to HID values (-127 to 127)
                        analogX = Math.round((dy / radius) * 127);
                        analogY = Math.round((1 - (dx / radius)) * 127) - 127;

                        analogX = Math.max(-127, Math.min(127, analogX));
                        analogY = Math.max(-127, Math.min(127, analogY));

                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (activity.pager != null && lockSwitch != null && lockSwitch.isChecked()) activity.pager.swipePossible(true);
                        stick.setTranslationX(0);
                        stick.setTranslationY(0);
                        analogX = 0;
                        analogY = 0;
                    }

                    activity.sendGamepadNotification(buttonsState, analogX, analogY);
                    return true;
                }
            });
        }
    }

    private void setupButton(View root, int id, final int bit, final MainActivity activity) {
        final View btn = root.findViewById(id);
        if (btn == null) return;
        final SwitchMaterial lockSwitch = root.findViewById(R.id.gamepad_lock_switch);

        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (activity == null) return false;
                
                if (lockSwitch != null && lockSwitch.isChecked()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    if (activity.pager != null) activity.pager.swipePossible(false);
                }
                
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    buttonsState |= bit;
                    btn.setPressed(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (activity.pager != null && lockSwitch != null && lockSwitch.isChecked()) activity.pager.swipePossible(true);
                    buttonsState &= ~bit;
                    btn.setPressed(false);
                }
                
                activity.sendGamepadNotification(buttonsState, analogX, analogY);
                return true;
            }
        });
    }
}
