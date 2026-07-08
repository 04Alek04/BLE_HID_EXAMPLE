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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Objects;

public class KeyboardFragment extends Fragment implements Button.OnTouchListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static KeyboardFragment newInstance() {
        return new KeyboardFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.keyboard_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        getActivity().findViewById(R.id.keyboard_button_send).setOnClickListener(this);

        // Keyboard character spinner configuration
        Spinner spinner_kb = (Spinner) getActivity().findViewById(R.id.keyboard_spinner_character);
        spinner_kb.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter_kb = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, KeyboardUsage.getUsageNames());
        dataAdapter_kb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_kb.setAdapter(dataAdapter_kb);

        ((ToggleButton) getActivity().findViewById(R.id.keyboard_toggle_kb)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        if (!isChecked) {
                            getActivity().findViewById(R.id.keyboard_sub_main_layout).requestFocus();
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        } else {
                            getActivity().findViewById(R.id.keyboard_sub_main_layout).requestFocus();
                            imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                        }
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();

        ((ToggleButton) getActivity().findViewById(R.id.keyboard_toggle_kb)).setChecked(false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        short toSend = 0;
        int value = ConsumerControlUsage.getUsage(v.getId());
        MainActivity activity = (MainActivity) getActivity();

        if (value == 0) {
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

        activity.sendNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, toSend);

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        MainActivity activity = (MainActivity) getActivity();
        short value = 0;

        if (parent.getChildAt(0) != null) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);

            if (parent.getId() == R.id.keyboard_spinner_character) {
                for (int i = 0; i < KeyboardUsage.KEYBOARD_USAGES.length; i++) {
                    if (Objects.equals(parent.getItemAtPosition(position).toString(),
                            KeyboardUsage.KEYBOARD_USAGES[i].description)) {
                        value = KeyboardUsage.KEYBOARD_USAGES[i].meta;
                        value += (short) (KeyboardUsage.KEYBOARD_USAGES[i].usage << 8);
                        break;
                    }
                }

                activity.sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, (int) value);
                activity.sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
    }

    @Override
    public void onClick(View v) {
        MainActivity activity = (MainActivity) getActivity();

        switch (v.getId()) {
            case R.id.keyboard_button_send:
                activity.sendNotification(((EditText) getActivity().findViewById(
                        R.id.keyboard_edit_text)).getText().toString());
        }
    }
}
