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

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.example.alek.ble_hid_example.databinding.MouseFragmentBinding;

public class MouseFragment extends Fragment implements View.OnTouchListener {

    private MouseFragmentBinding binding;

    public static MouseFragment newInstance() {
        return new MouseFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = MouseFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final View final_view = view;
        ViewTreeObserver observer = final_view.getViewTreeObserver();

        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (binding != null && binding.mouseMouseCursor != null) {
                    binding.mouseMouseCursor.setMinimumHeight(binding.mouseMouseCursor.getWidth());
                }

                final_view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        binding.mouseButtonMouse1.setOnTouchListener(this);
        binding.mouseButtonMouse2.setOnTouchListener(this);
        binding.mouseButtonMouse3.setOnTouchListener(this);
        binding.mouseButtonMouse4.setOnTouchListener(this);
        binding.mouseButtonMouse5.setOnTouchListener(this);

        binding.mouseHScroll.setOnTouchListener(new View.OnTouchListener() {
            private float lastX;
            private final TouchInputCalculator scrollCalculator = new TouchInputCalculator();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity == null) return false;
                int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        activity.swipePossible(false);
                        lastX = event.getX();
                        scrollCalculator.resetRemainder();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float dx = currentX - lastX;
                        lastX = currentX;

                        int stepX = scrollCalculator.calculateScrollStep(dx, 0.2f);
                        if (stepX != 0) {
                            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_HSCROLL, stepX);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        activity.swipePossible(true);
                        break;
                }
                return true;
            }
        });

        binding.mouseMouseCursor.setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector gestureDetector = new GestureDetector(getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            MainActivity activity = (MainActivity) getActivity();
                            if (activity != null) {
                                activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, HidMouseCodes.BUTTON_LEFT);
                                activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0);
                            }
                            return super.onDoubleTap(e);
                        }
                    });
            private float lastX, lastY;
            private final TouchInputCalculator cursorCalculator = new TouchInputCalculator();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity == null) return false;

                int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        activity.swipePossible(false);
                        lastX = event.getX();
                        lastY = event.getY();
                        cursorCalculator.resetRemainder();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        float dx = currentX - lastX;
                        float dy = currentY - lastY;
                        lastX = currentX;
                        lastY = currentY;

                        int toSend = cursorCalculator.calculateRelativeDelta(dx, dy, 1.5f);
                        if (toSend != 0) {
                            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_XY, toSend);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        activity.swipePossible(true);
                        break;
                }

                gestureDetector.onTouchEvent(event);

                return true;
            }
        });
    }

    private int buttonsState = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        MainActivity activity = (MainActivity) getActivity();
        int value;

        int id = v.getId();
        if (id == R.id.mouse_button_mouse_1) {
            value = HidMouseCodes.BUTTON_LEFT;
        } else if (id == R.id.mouse_button_mouse_2) {
            value = HidMouseCodes.BUTTON_RIGHT;
        } else if (id == R.id.mouse_button_mouse_3) {
            value = HidMouseCodes.BUTTON_MIDDLE;
        } else if (id == R.id.mouse_button_mouse_4) {
            value = HidMouseCodes.BUTTON_BACK;
        } else if (id == R.id.mouse_button_mouse_5) {
            value = HidMouseCodes.BUTTON_FORWARD;
        } else {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            v.setPressed(true);
            buttonsState |= value;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            v.setPressed(false);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                buttonsState &= ~value;
            } else {
                buttonsState &= ~value; // Cancel should also release
            }
        } else {
            return false;
        }

        if (activity != null) {
            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, buttonsState);
        }

        return true;
    }
}
