/*
 * Copyright 2018-2019 Aleksander Drewnicki
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
import androidx.core.view.VelocityTrackerCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class MouseFragment extends Fragment implements View.OnTouchListener {
    private int i = 0;
    private float x = 0.0f;
    private float y = 0.0f;

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

        getActivity().findViewById(R.id.mouse_mouse_cursor).setOnTouchListener(new View.OnTouchListener() {
            final MainActivity activity = (MainActivity) getActivity();
            private final GestureDetector gestureDetector = new GestureDetector(getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            activity.sendNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x01);
                            activity.sendNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, 0x00);
                            return super.onDoubleTap(e);
                        }
                    });
            private VelocityTracker mVelocityTracker = null;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                CustomViewPager pager = (CustomViewPager) getActivity().findViewById(R.id.viewPager);

                int index = event.getActionIndex();
                int action = event.getActionMasked();
                int pointerId = event.getPointerId(index);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        pager.swipePossible(false);

                        if (mVelocityTracker == null) {
                            mVelocityTracker = VelocityTracker.obtain();
                        } else {
                            mVelocityTracker.clear();
                        }

                        mVelocityTracker.addMovement(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(event);
                        mVelocityTracker.computeCurrentVelocity(10000);

                        if ((++i >= 3)) {
                            x /= 150 * 3;
                            y /= 150 * 3;

                            byte _x = (byte) (x > 127 ? 127 : (x < -127 ? -127 : x));
                            byte _y = (byte) (y > 127 ? 127 : (y < -127 ? -127 : y));
                            int toSend = _x | (((short) _y) << 8);

                            activity.sendNotification(ReportField.REPORT_FIELD_MOUSE_XY, toSend);
                            i = 0;
                            x = 0.0f;
                            y = 0.0f;
                        } else {
                            x += VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId);
                            y += VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId);
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        pager.swipePossible(true);
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                        break;
                }

                gestureDetector.onTouchEvent(event);

                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        MainActivity activity = (MainActivity) getActivity();
        short toSend = 0;
        int value;

        switch (v.getId()) {
            /* Mouse keys */
            case R.id.mouse_button_mouse_1:
                value = 0x01;
                break;
            case R.id.mouse_button_mouse_2:
                value = 0x02;
                break;
            case R.id.mouse_button_mouse_3:
                value = 0x04;
                break;
            case R.id.mouse_button_mouse_4:
                value = 0x08;
                break;
            case R.id.mouse_button_mouse_5:
                value = 0x10;
                break;
            default:
                return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            toSend |= value;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            toSend &= ~value;
            toSend &= 0xffff;
        } else {
            return false;
        }

        activity.sendNotification(ReportField.REPORT_FIELD_MOUSE_BUTTONS, toSend);

        return true;
    }
}
