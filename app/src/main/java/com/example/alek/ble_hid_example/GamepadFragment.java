package com.example.alek.ble_hid_example;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class GamepadFragment extends Fragment {

    private int buttonsState = 0;
    private int analogLX = 0;
    private int analogLY = 0;
    private int analogRX = 0;
    private int analogRY = 0;
    
    private View lStickView;
    private View rStickView;
    private View touchpadView;
    private ColorStateList defaultLStickTint;
    private ColorStateList defaultRStickTint;

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
        
        final View lStickToggle = view.findViewById(R.id.gamepad_l_stick_toggle);
        final View touchToggle = view.findViewById(R.id.gamepad_touch_toggle);
        final View lockToggle = view.findViewById(R.id.gamepad_lock_toggle);
        final View rStickToggle = view.findViewById(R.id.gamepad_r_stick_toggle);

        final View lAnalogBase = view.findViewById(R.id.gamepad_left_analog_base);
        final View dpadContainer = view.findViewById(R.id.gamepad_dpad_container);
        final View rAnalogBase = view.findViewById(R.id.gamepad_right_analog_base);
        final View buttonsContainer = view.findViewById(R.id.gamepad_buttons_container);

        lStickView = view.findViewById(R.id.gamepad_left_analog_stick);
        rStickView = view.findViewById(R.id.gamepad_right_analog_stick);
        touchpadView = view.findViewById(R.id.gamepad_touchpad);

        // Restore States
        if (getContext() != null) {
            boolean lStickChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_l_stick", true);
            boolean touchChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_touch", true);
            boolean lockChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_lock", false);
            boolean rStickChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_r_stick", true);
            
            if (lStickToggle != null) {
                lStickToggle.setSelected(lStickChecked);
                if (lAnalogBase != null) lAnalogBase.setBackgroundResource(lStickChecked ? R.drawable.gamepad_analog_octagon : 0);
                if (lStickView != null) lStickView.setVisibility(lStickChecked ? View.VISIBLE : View.GONE);
                if (dpadContainer != null) dpadContainer.setVisibility(lStickChecked ? View.GONE : View.VISIBLE);
            }
            if (touchToggle != null) {
                touchToggle.setSelected(touchChecked);
                if (touchpadView != null) touchpadView.setVisibility(touchChecked ? View.VISIBLE : View.GONE);
            }
            if (lockToggle != null) {
                lockToggle.setSelected(lockChecked);
                if (activity != null) activity.swipePossible(!lockChecked);
            }
            if (rStickToggle != null) {
                rStickToggle.setSelected(rStickChecked);
                if (rAnalogBase != null) rAnalogBase.setBackgroundResource(rStickChecked ? R.drawable.gamepad_analog_octagon : 0);
                if (rStickView != null) rStickView.setVisibility(rStickChecked ? View.VISIBLE : View.GONE);
                if (buttonsContainer != null) buttonsContainer.setVisibility(rStickChecked ? View.GONE : View.VISIBLE);
            }
        }

        // L-STK Toggle Logic
        if (lStickToggle != null) {
            lStickToggle.setOnClickListener(v -> {
                boolean newState = !v.isSelected();
                v.setSelected(newState);
                if (getContext() != null) {
                    getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("gamepad_l_stick", newState).apply();
                }
                if (lAnalogBase != null) lAnalogBase.setBackgroundResource(newState ? R.drawable.gamepad_analog_octagon : 0);
                if (lStickView != null) lStickView.setVisibility(newState ? View.VISIBLE : View.GONE);
                if (dpadContainer != null) dpadContainer.setVisibility(newState ? View.GONE : View.VISIBLE);
                analogLX = 0; analogLY = 0;
                if (activity != null) activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
            });
        }

        // TOUCH Toggle Logic
        if (touchToggle != null) {
            touchToggle.setOnClickListener(v -> {
                boolean newState = !v.isSelected();
                v.setSelected(newState);
                if (getContext() != null) {
                    getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("gamepad_touch", newState).apply();
                }
                if (touchpadView != null) touchpadView.setVisibility(newState ? View.VISIBLE : View.GONE);
            });
        }

        // LOCK Toggle Logic
        if (lockToggle != null) {
            lockToggle.setOnClickListener(v -> {
                boolean newState = !v.isSelected();
                v.setSelected(newState);
                if (getContext() != null) {
                    getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("gamepad_lock", newState).apply();
                }
                if (activity != null) activity.swipePossible(!newState);
            });
        }

        // R-STK Toggle Logic
        if (rStickToggle != null) {
            rStickToggle.setOnClickListener(v -> {
                boolean newState = !v.isSelected();
                v.setSelected(newState);
                if (getContext() != null) {
                    getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putBoolean("gamepad_r_stick", newState).apply();
                }
                if (rAnalogBase != null) rAnalogBase.setBackgroundResource(newState ? R.drawable.gamepad_analog_octagon : 0);
                if (rStickView != null) rStickView.setVisibility(newState ? View.VISIBLE : View.GONE);
                if (buttonsContainer != null) buttonsContainer.setVisibility(newState ? View.GONE : View.VISIBLE);
                analogRX = 0; analogRY = 0;
                if (activity != null) activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
            });
        }

        // MENU and START buttons
        setupButton(view, R.id.gamepad_btn_menu, 0x100, activity);
        setupButton(view, R.id.gamepad_btn_start, 0x200, activity);

        final float touchSlop = getContext() != null ? ViewConfiguration.get(getContext()).getScaledTouchSlop() : 16f;

        // --- LEFT STICK LOGIC ---
        if (lStickView != null) defaultLStickTint = lStickView.getBackgroundTintList();
        
        final GestureDetector lStickDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                buttonsState |= 0x400; // L3
                updateSticksVisuals();
                if (activity != null) activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                return true;
            }
        });

        if (lAnalogBase != null) {
            lAnalogBase.setOnTouchListener(new View.OnTouchListener() {
                private float touchDownX, touchDownY;
                private boolean isDragging = false;
                private boolean isTouchValid = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (activity == null || (lStickToggle != null && !lStickToggle.isSelected())) return false;

                    int action = event.getActionMasked();

                    float centerX = v.getWidth() / 2f;
                    float centerY = v.getHeight() / 2f;
                    float radius = v.getWidth() / 2f;

                    if (action == MotionEvent.ACTION_DOWN) {
                        float stickCenterX = centerX + (lStickView != null ? lStickView.getTranslationX() : 0);
                        float stickCenterY = centerY + (lStickView != null ? lStickView.getTranslationY() : 0);
                        float stickRadius = (lStickView != null && lStickView.getWidth() > 0) ? (lStickView.getWidth() / 2f * 1.2f) : (v.getWidth() * 0.2f);

                        float distFromStick = (float) Math.hypot(event.getX() - stickCenterX, event.getY() - stickCenterY);

                        if (distFromStick <= stickRadius) {
                            isTouchValid = true;
                            touchDownX = event.getX();
                            touchDownY = event.getY();
                            isDragging = false;
                        } else {
                            isTouchValid = false;
                            return false;
                        }
                    }

                    if (!isTouchValid) return false;

                    lStickDetector.onTouchEvent(event);

                    if (lockToggle != null && lockToggle.isSelected()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        activity.swipePossible(false);
                    }

                    if (action == MotionEvent.ACTION_MOVE) {
                        if (!isDragging) {
                            float dragDist = (float) Math.hypot(event.getX() - touchDownX, event.getY() - touchDownY);
                            if (dragDist > touchSlop) {
                                isDragging = true;
                            }
                        }

                        if (isDragging) {
                            float dx = event.getX() - centerX;
                            float dy = event.getY() - centerY;
                            float distance = (float) Math.hypot(dx, dy);

                            if (distance > radius) {
                                dx = (dx / distance) * radius;
                                dy = (dy / distance) * radius;
                            }

                            if (lStickView != null) {
                                lStickView.setTranslationX(dx);
                                lStickView.setTranslationY(dy);
                            }

                            analogLX = Math.round((dy / radius) * 127);
                            analogLY = Math.round((1 - (dx / radius)) * 127) - 127;
                            analogLX = Math.max(-127, Math.min(127, analogLX));
                            analogLY = Math.max(-127, Math.min(127, analogLY));

                            activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                        }
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (lockToggle == null || !lockToggle.isSelected()) {
                            activity.swipePossible(true);
                        }
                        isDragging = false;
                        isTouchValid = false;
                        buttonsState &= ~0x400; // Release L3 on finger lift
                        updateSticksVisuals();
                        if (lStickView != null) {
                            lStickView.setTranslationX(0);
                            lStickView.setTranslationY(0);
                        }
                        analogLX = 0;
                        analogLY = 0;
                        activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                    }
                    return true;
                }
            });
        }

        // --- RIGHT STICK LOGIC ---
        if (rStickView != null) defaultRStickTint = rStickView.getBackgroundTintList();

        final GestureDetector rStickDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                buttonsState |= 0x800; // R3
                updateSticksVisuals();
                if (activity != null) activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                return true;
            }
        });

        if (rAnalogBase != null) {
            rAnalogBase.setOnTouchListener(new View.OnTouchListener() {
                private float touchDownX, touchDownY;
                private boolean isDragging = false;
                private boolean isTouchValid = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (activity == null || (rStickToggle != null && !rStickToggle.isSelected())) return false;

                    int action = event.getActionMasked();

                    float centerX = v.getWidth() / 2f;
                    float centerY = v.getHeight() / 2f;
                    float radius = v.getWidth() / 2f;

                    if (action == MotionEvent.ACTION_DOWN) {
                        float stickCenterX = centerX + (rStickView != null ? rStickView.getTranslationX() : 0);
                        float stickCenterY = centerY + (rStickView != null ? rStickView.getTranslationY() : 0);
                        float stickRadius = (rStickView != null && rStickView.getWidth() > 0) ? (rStickView.getWidth() / 2f * 1.2f) : (v.getWidth() * 0.2f);

                        float distFromStick = (float) Math.hypot(event.getX() - stickCenterX, event.getY() - stickCenterY);

                        if (distFromStick <= stickRadius) {
                            isTouchValid = true;
                            touchDownX = event.getX();
                            touchDownY = event.getY();
                            isDragging = false;
                        } else {
                            isTouchValid = false;
                            return false;
                        }
                    }

                    if (!isTouchValid) return false;

                    rStickDetector.onTouchEvent(event);

                    if (lockToggle != null && lockToggle.isSelected()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        activity.swipePossible(false);
                    }

                    if (action == MotionEvent.ACTION_MOVE) {
                        if (!isDragging) {
                            float dragDist = (float) Math.hypot(event.getX() - touchDownX, event.getY() - touchDownY);
                            if (dragDist > touchSlop) {
                                isDragging = true;
                            }
                        }

                        if (isDragging) {
                            float dx = event.getX() - centerX;
                            float dy = event.getY() - centerY;
                            float distance = (float) Math.hypot(dx, dy);

                            if (distance > radius) {
                                dx = (dx / distance) * radius;
                                dy = (dy / distance) * radius;
                            }

                            if (rStickView != null) {
                                rStickView.setTranslationX(dx);
                                rStickView.setTranslationY(dy);
                            }

                            analogRX = Math.round((dy / radius) * 127);
                            analogRY = Math.round((1 - (dx / radius)) * 127) - 127;
                            analogRX = Math.max(-127, Math.min(127, analogRX));
                            analogRY = Math.max(-127, Math.min(127, analogRY));

                            activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                        }
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (lockToggle == null || !lockToggle.isSelected()) {
                            activity.swipePossible(true);
                        }
                        isDragging = false;
                        isTouchValid = false;
                        buttonsState &= ~0x800; // Release R3 on finger lift
                        updateSticksVisuals();
                        if (rStickView != null) {
                            rStickView.setTranslationX(0);
                            rStickView.setTranslationY(0);
                        }
                        analogRX = 0;
                        analogRY = 0;
                        activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                    }
                    return true;
                }
            });
        }

        // Digital D-pad Buttons (Left Side Alternative - Left Stick Axes X & Y)
        setupDpadButton(view, R.id.gamepad_d_up, 127, 0, false, activity);
        setupDpadButton(view, R.id.gamepad_d_down, -127, 0, false, activity);
        setupDpadButton(view, R.id.gamepad_d_left, 0, -127, false, activity);
        setupDpadButton(view, R.id.gamepad_d_right, 0, 127, false, activity);
        setupButton(view, R.id.gamepad_d_center, 0x400, activity); // L3 (0x0B)

        // Right Side Alternative Buttons (Right Stick Axes Z & Rz)
        setupDpadButton(view, R.id.gamepad_btn_an_r_up, 127, 0, true, activity);
        setupDpadButton(view, R.id.gamepad_btn_an_r_down, -127, 0, true, activity);
        setupDpadButton(view, R.id.gamepad_btn_an_r_left, 0, -127, true, activity);
        setupDpadButton(view, R.id.gamepad_btn_an_r_right, 0, 127, true, activity);
        setupButton(view, R.id.gamepad_btn_r3, 0x800, activity); // R3 (0x0C)

        // Shoulder Buttons / Triggers
        setupButton(view, R.id.gamepad_btn_lb, 0x10, activity); // LB (0x05)
        setupButton(view, R.id.gamepad_btn_rb, 0x20, activity); // RB (0x06)
        setupButton(view, R.id.gamepad_btn_lt, 0x40, activity); // LT (0x07)
        setupButton(view, R.id.gamepad_btn_rt, 0x80, activity); // RT (0x08)

        // Standalone BT Buttons (A, B, X, Y Buttons)
        setupButton(view, R.id.gamepad_btn_bt0, 0x01, activity); // Button A
        setupButton(view, R.id.gamepad_btn_bt1, 0x02, activity); // Button B
        setupButton(view, R.id.gamepad_btn_bt2, 0x04, activity); // Button X
        setupButton(view, R.id.gamepad_btn_bt3, 0x08, activity); // Button Y

        // --- TOUCHPAD LOGIC ---
        touchpadView = view.findViewById(R.id.gamepad_touchpad);
        if (touchpadView != null) {
            touchpadView.setOnTouchListener(new View.OnTouchListener() {
                private float touchDownX, touchDownY;
                private float lastX, lastY;
                private float remainderX, remainderY;
                private boolean isDragging = false;
                private boolean isLeftClickPressed = false;

                private final GestureDetector touchpadDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        isLeftClickPressed = true;
                        if (activity != null) {
                            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x01);
                        }
                        return true;
                    }
                });

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (activity == null) return false;

                    touchpadDetector.onTouchEvent(event);

                    int action = event.getActionMasked();
                    final View lockToggle = getView() != null ? getView().findViewById(R.id.gamepad_lock_toggle) : null;

                    if (action == MotionEvent.ACTION_DOWN) {
                        touchDownX = event.getX();
                        touchDownY = event.getY();
                        lastX = touchDownX;
                        lastY = touchDownY;
                        remainderX = 0;
                        remainderY = 0;
                        isDragging = false;

                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        if (lockToggle != null && lockToggle.isSelected()) {
                            activity.swipePossible(false);
                        }
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        float x = event.getX();
                        float y = event.getY();

                        boolean isOutOfBounds = (x < 0 || x >= v.getWidth() || y < 0 || y >= v.getHeight());

                        if (isOutOfBounds) {
                            if (isLeftClickPressed) {
                                isLeftClickPressed = false;
                                activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x00);
                            }
                        } else {
                            if (!isDragging) {
                                float dragDist = (float) Math.hypot(x - touchDownX, y - touchDownY);
                                if (dragDist > touchSlop) {
                                    isDragging = true;
                                    lastX = x;
                                    lastY = y;
                                }
                            }

                            if (isDragging) {
                                float rawDx = x - lastX;
                                float rawDy = y - lastY;
                                lastX = x;
                                lastY = y;

                                float sensitivity = 1.5f;
                                // Swapped touchpad axes (screen dy -> mouse X, screen dx -> mouse Y inverted)
                                float moveX = rawDy * sensitivity + remainderX;
                                float moveY = -rawDx * sensitivity + remainderY;

                                int intX = Math.round(moveX);
                                int intY = Math.round(moveY);

                                if (intX != 0 || intY != 0) {
                                    remainderX = moveX - intX;
                                    remainderY = moveY - intY;

                                    byte _x = (byte) Math.max(-128, Math.min(127, intX));
                                    byte _y = (byte) Math.max(-128, Math.min(127, intY));
                                    int btn = isLeftClickPressed ? 0x01 : 0x00;
                                    int toSend = (btn & 0xFF) | ((_x & 0xFF) << 8) | ((_y & 0xFF) << 16);

                                    activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS_XY, toSend);
                                } else {
                                    remainderX = moveX;
                                    remainderY = moveY;
                                }
                            }
                        }
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (lockToggle == null || !lockToggle.isSelected()) {
                            activity.swipePossible(true);
                        }
                        isDragging = false;

                        if (isLeftClickPressed) {
                            isLeftClickPressed = false;
                            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x00);
                        }
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncLockWithPager();
    }

    private void syncLockWithPager() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && getContext() != null) {
            boolean lockChecked = getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean("gamepad_lock", false);
            activity.swipePossible(!lockChecked);
        }
    }

    public void onExternalButtonEvent(int bit, boolean pressed) {
        if (pressed) buttonsState |= bit; else buttonsState &= ~bit;
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
    }

    private void setupButton(View root, int id, final int bit, final MainActivity activity) {
        final View btn = root.findViewById(id);
        if (btn == null) return;
        final View lockToggle = root.findViewById(R.id.gamepad_lock_toggle);
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (activity == null) return false;
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    if (lockToggle != null && lockToggle.isSelected()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        activity.swipePossible(false);
                    }
                    buttonsState |= bit;
                    btn.setPressed(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                    buttonsState &= ~bit;
                    btn.setPressed(false);
                    if (lockToggle == null || !lockToggle.isSelected()) {
                        activity.swipePossible(true);
                    }
                }
                activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                return true;
            }
        });
    }

    private void updateSticksVisuals() {
        if (lStickView != null) {
            boolean isPressed = (buttonsState & 0x400) != 0;
            lStickView.setBackgroundTintList(isPressed ? ColorStateList.valueOf(Color.WHITE) : defaultLStickTint);
        }
        if (rStickView != null) {
            boolean isPressed = (buttonsState & 0x800) != 0;
            rStickView.setBackgroundTintList(isPressed ? ColorStateList.valueOf(Color.WHITE) : defaultRStickTint);
        }
    }

    private void setupDpadButton(View root, int id, final int xVal, final int yVal, final boolean isRightStick, final MainActivity activity) {
        final View btn = root.findViewById(id);
        if (btn == null) return;
        final View lockToggle = root.findViewById(R.id.gamepad_lock_toggle);
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (activity == null) return false;
                if (lockToggle != null && lockToggle.isSelected()) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    activity.swipePossible(false);
                }
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    if (isRightStick) {
                        if (xVal != 0) analogRX = xVal;
                        if (yVal != 0) analogRY = yVal;
                    } else {
                        if (xVal != 0) analogLX = xVal;
                        if (yVal != 0) analogLY = yVal;
                    }
                    btn.setPressed(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (lockToggle == null || !lockToggle.isSelected()) {
                        activity.swipePossible(true);
                    }
                    if (isRightStick) {
                        if (xVal != 0) analogRX = 0;
                        if (yVal != 0) analogRY = 0;
                    } else {
                        if (xVal != 0) analogLX = 0;
                        if (yVal != 0) analogLY = 0;
                    }
                    btn.setPressed(false);
                }
                activity.sendGamepadNotification(buttonsState, analogLX, analogLY, analogRX, analogRY);
                return true;
            }
        });
    }
}
