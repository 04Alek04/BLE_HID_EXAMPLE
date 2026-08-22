package com.example.alek.ble_hid_example;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
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
    private View stickView;
    private ColorStateList defaultStickTint;

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
        final SwitchMaterial stickSwitch = view.findViewById(R.id.gamepad_stick_switch);
        
        final View analogBase = view.findViewById(R.id.gamepad_left_analog_base);
        final View dpadContainer = view.findViewById(R.id.gamepad_dpad_container);

        // Restore Switch states
        if (getContext() != null) {
            boolean lockChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_lock", true);
            boolean stickChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_stick", true);
            if (lockSwitch != null) lockSwitch.setChecked(lockChecked);
            if (stickSwitch != null) {
                stickSwitch.setChecked(stickChecked);
                // Initial UI state for STICK
                if (analogBase != null) analogBase.setVisibility(stickChecked ? View.VISIBLE : View.GONE);
                if (dpadContainer != null) dpadContainer.setVisibility(stickChecked ? View.GONE : View.VISIBLE);
            }
        }

        // STICK Switch logic
        if (stickSwitch != null) {
            stickSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Save state
                if (getContext() != null) {
                    getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("gamepad_stick", isChecked).apply();
                }
                
                if (isChecked) {
                    if (analogBase != null) analogBase.setVisibility(View.VISIBLE);
                    if (dpadContainer != null) dpadContainer.setVisibility(View.GONE);
                } else {
                    if (analogBase != null) analogBase.setVisibility(View.GONE);
                    if (dpadContainer != null) dpadContainer.setVisibility(View.VISIBLE);
                }
                // Reset axes on switch
                analogX = 0;
                analogY = 0;
                if (activity != null) activity.sendGamepadNotification(buttonsState, analogX, analogY);
            });
        }

        // LOCK Switch logic
        if (lockSwitch != null) {
            lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (getContext() != null) {
                    getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("gamepad_lock", isChecked).apply();
                }
            });
        }

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

        View touchSurface = view.findViewById(R.id.gamepad_touch_surface);
        if (touchSurface != null) touchSurface.setOnTouchListener(swipeBlocker);
        
        View buttonsContainer = view.findViewById(R.id.gamepad_buttons_container);
        if (buttonsContainer != null) buttonsContainer.setOnTouchListener(swipeBlocker);

        if (dpadContainer != null) dpadContainer.setOnTouchListener(swipeBlocker);

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
        setupButton(view, R.id.gamepad_d_center, 0x400, activity); // L3 / Stick Click

        // Digital D-pad Buttons logic
        setupDpadButton(view, R.id.gamepad_d_up, 0, -127, activity);
        setupDpadButton(view, R.id.gamepad_d_down, 0, 127, activity);
        setupDpadButton(view, R.id.gamepad_d_left, -127, 0, activity);
        setupDpadButton(view, R.id.gamepad_d_right, 127, 0, activity);

        // Analog Stick logic
        stickView = view.findViewById(R.id.gamepad_left_analog_stick);
        if (stickView != null) {
            defaultStickTint = stickView.getBackgroundTintList();
        }
        final View stick = stickView;
        
        final GestureDetector stickGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                // Set Stick Click bit - it will stay active as long as the finger is down
                buttonsState |= 0x400;
                updateStickAlpha();
                if (activity != null) activity.sendGamepadNotification(buttonsState, analogX, analogY);
                return true;
            }
        });

        if (analogBase != null) {
            analogBase.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (activity == null) return false;
                    
                    stickGestureDetector.onTouchEvent(event);
                    
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
                        
                        // Clear Stick Click when analog is released
                        buttonsState &= ~0x400;
                        updateStickAlpha();

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

    public void onExternalButtonEvent(int bit, boolean pressed) {
        if (pressed) {
            buttonsState |= bit;
        } else {
            buttonsState &= ~bit;
        }

        if (bit == 0x400) {
            updateStickAlpha();
        }

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.sendGamepadNotification(buttonsState, analogX, analogY);
        }
    }

    private void updateStickAlpha() {
        if (stickView != null) {
            // L3 (0x400)
            boolean isPressed = (buttonsState & 0x400) != 0;
            
            if (isPressed) {
                // When pressed: Intense white color
                stickView.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            } else {
                // Resting state: Return to default color
                stickView.setBackgroundTintList(defaultStickTint);
            }
            
            // Size remains unchanged (1.0f)
            stickView.setAlpha(1.0f);
            stickView.setScaleX(1.0f);
            stickView.setScaleY(1.0f);
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
                
                int action = event.getActionMasked();
                
                // Block ViewPager only for touches on the button itself
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    if (lockSwitch != null && lockSwitch.isChecked()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        if (activity.pager != null) activity.pager.swipePossible(false);
                    }
                    buttonsState |= bit;
                    btn.setPressed(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                    buttonsState &= ~bit;
                    btn.setPressed(false);
                    if (activity.pager != null && lockSwitch != null && lockSwitch.isChecked()) {
                        activity.pager.swipePossible(true);
                    }
                }
                
                activity.sendGamepadNotification(buttonsState, analogX, analogY);
                return true; // Consume event so it doesn't leak to background
            }
        });
    }

    private void setupDpadButton(View root, int id, final int xVal, final int yVal, final MainActivity activity) {
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
                    if (xVal != 0) analogX = xVal;
                    if (yVal != 0) analogY = yVal;
                    btn.setPressed(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (activity.pager != null && lockSwitch != null && lockSwitch.isChecked()) activity.pager.swipePossible(true);
                    if (xVal != 0) analogX = 0;
                    if (yVal != 0) analogY = 0;
                    btn.setPressed(false);
                }
                
                activity.sendGamepadNotification(buttonsState, analogX, analogY);
                return true;
            }
        });
    }
}
