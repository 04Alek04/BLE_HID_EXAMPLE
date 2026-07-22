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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class DPadFragment extends Fragment implements View.OnTouchListener {

    public static DPadFragment newInstance() {
        return new DPadFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.d_pad_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final View final_view = view;
        ViewTreeObserver observer = final_view.getViewTreeObserver();

        getActivity().findViewById(R.id.d_pad_up).setOnTouchListener(this);
        getActivity().findViewById(R.id.d_pad_left).setOnTouchListener(this);
        getActivity().findViewById(R.id.d_pad_right).setOnTouchListener(this);
        getActivity().findViewById(R.id.d_pad_down).setOnTouchListener(this);

        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int ids[] = {R.id.d_pad_up, R.id.d_pad_left, R.id.d_pad_right, R.id.d_pad_down};

                for (int id : ids) {
                    View v = getActivity().findViewById(id);
                    v.setMinimumHeight(v.getWidth());
                }
                final_view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        MainActivity activity = (MainActivity) getActivity();
        byte toSend = 0;
        byte value;

        int id = v.getId();
        if (id == R.id.d_pad_up) {
            value = KeyboardUsage.getUsage("Up");
        } else if (id == R.id.d_pad_left) {
            value = KeyboardUsage.getUsage("Left");
        } else if (id == R.id.d_pad_right) {
            value = KeyboardUsage.getUsage("Right");
        } else if (id == R.id.d_pad_down) {
            value = KeyboardUsage.getUsage("Down");
        } else {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            toSend |= value;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            toSend = 0;
        } else {
            return false;
        }

        activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_KEYS, toSend);

        return true;
    }
}
