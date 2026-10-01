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

public class MouseFragment extends Fragment implements View.OnTouchListener {

    public static MouseFragment newInstance() {
        return new MouseFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.mouse_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final View final_view = view;
        ViewTreeObserver observer = final_view.getViewTreeObserver();

        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View mc = getActivity().findViewById(R.id.mouse_mouse_cursor);

                if (mc != null) {
                    mc.setMinimumHeight(mc.getWidth());
                }

                final_view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        getActivity().findViewById(R.id.mouse_button_mouse_1).setOnTouchListener(this);
        getActivity().findViewById(R.id.mouse_button_mouse_2).setOnTouchListener(this);
        getActivity().findViewById(R.id.mouse_button_mouse_3).setOnTouchListener(this);
        getActivity().findViewById(R.id.mouse_button_mouse_4).setOnTouchListener(this);
        getActivity().findViewById(R.id.mouse_button_mouse_5).setOnTouchListener(this);

        getActivity().findViewById(R.id.mouse_h_scroll).setOnTouchListener(new View.OnTouchListener() {
            private float lastX;
            private float remainderX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity == null) return false;
                int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        activity.swipePossible(false);
                        lastX = event.getX();
                        remainderX = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float dx = currentX - lastX;
                        lastX = currentX;

                        float sensitivity = 0.2f; // Decreased from 0.5f for slower scrolling
                        float moveX = dx * sensitivity + remainderX;
                        int intX = Math.round(moveX);

                        if (intX != 0) {
                            remainderX = moveX - intX;
                            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_HSCROLL, intX);
                        } else {
                            remainderX = moveX;
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

        getActivity().findViewById(R.id.mouse_mouse_cursor).setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector gestureDetector = new GestureDetector(getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            MainActivity activity = (MainActivity) getActivity();
                            if (activity != null) {
                                activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x01);
                                activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x00);
                            }
                            return super.onDoubleTap(e);
                        }
                    });
            private float lastX, lastY;
            private float remainderX, remainderY;

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
                        remainderX = 0;
                        remainderY = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        float dx = currentX - lastX;
                        float dy = currentY - lastY;
                        lastX = currentX;
                        lastY = currentY;

                        float sensitivity = 1.5f; // Adjust sensitivity for better feel
                        float moveX = dx * sensitivity + remainderX;
                        float moveY = dy * sensitivity + remainderY;

                        int intX = Math.round(moveX);
                        int intY = Math.round(moveY);

                        if (intX != 0 || intY != 0) {
                            remainderX = moveX - intX;
                            remainderY = moveY - intY;

                            byte _x = (byte) Math.max(-128, Math.min(127, intX));
                            byte _y = (byte) Math.max(-128, Math.min(127, intY));
                            int toSend = (_x & 0xFF) | ((_y & 0xFF) << 8);

                            activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_XY, toSend);
                        } else {
                            remainderX = moveX;
                            remainderY = moveY;
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
            value = 0x01;
        } else if (id == R.id.mouse_button_mouse_2) {
            value = 0x02;
        } else if (id == R.id.mouse_button_mouse_3) {
            value = 0x04;
        } else if (id == R.id.mouse_button_mouse_4) {
            value = 0x08;
        } else if (id == R.id.mouse_button_mouse_5) {
            value = 0x10;
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

        activity.sendMouseNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, buttonsState);

        return true;
    }
}
