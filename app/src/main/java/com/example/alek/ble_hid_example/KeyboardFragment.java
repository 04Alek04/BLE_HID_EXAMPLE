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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyboardFragment extends Fragment implements Button.OnTouchListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private ToggleButton numLockBtn, capsLockBtn, scrollLockBtn;

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
        List<String> kbList = new ArrayList<>();
        kbList.add("SELECT VALUE");
        kbList.addAll(KeyboardUsage.getUsageNames());

        Spinner spinner_kb = (Spinner) getActivity().findViewById(R.id.keyboard_spinner_character);
        spinner_kb.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter_kb = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item, kbList);
        dataAdapter_kb.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner_kb.setAdapter(dataAdapter_kb);
        spinner_kb.setSelection(0);

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

        numLockBtn = (ToggleButton) getActivity().findViewById(R.id.keyboard_lock_num);
        capsLockBtn = (ToggleButton) getActivity().findViewById(R.id.keyboard_lock_caps);
        scrollLockBtn = (ToggleButton) getActivity().findViewById(R.id.keyboard_lock_scroll);

        View.OnClickListener lockClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte usage = 0;
                int id = v.getId();
                if (id == R.id.keyboard_lock_num) {
                    usage = (byte) 0x53;
                } else if (id == R.id.keyboard_lock_caps) {
                    usage = (byte) 0x39;
                } else if (id == R.id.keyboard_lock_scroll) {
                    usage = (byte) 0x47;
                }

                if (usage != 0) {
                    MainActivity activity = (MainActivity) getActivity();
                    // Send key press
                    activity.sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, usage << 8);
                    // Send key release
                    activity.sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                }
            }
        };

        numLockBtn.setOnClickListener(lockClickListener);
        capsLockBtn.setOnClickListener(lockClickListener);
        scrollLockBtn.setOnClickListener(lockClickListener);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            updateLedStatus(activity.getLastKeyboardLedStatus());
        }
    }

    public void updateLedStatus(byte status) {
        if (numLockBtn != null) {
            numLockBtn.setChecked((status & 0x01) != 0);
        }
        if (capsLockBtn != null) {
            capsLockBtn.setChecked((status & 0x02) != 0);
        }
        if (scrollLockBtn != null) {
            scrollLockBtn.setChecked((status & 0x04) != 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        ((ToggleButton) getActivity().findViewById(R.id.keyboard_toggle_kb)).setChecked(false);
        Spinner spinner_kb = (Spinner) getActivity().findViewById(R.id.keyboard_spinner_character);
        if (spinner_kb != null) {
            spinner_kb.setSelection(0);
        }
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
                if (position == 0) {
                    /* Dummy item */
                    return;
                }
                String selectedDescription = parent.getItemAtPosition(position).toString();
                for (int i = 0; i < KeyboardUsage.KEYBOARD_USAGES.length; i++) {
                    if (Objects.equals(selectedDescription,
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

        if (v.getId() == R.id.keyboard_button_send) {
            activity.sendNotification(((EditText) getActivity().findViewById(
                    R.id.keyboard_edit_text)).getText().toString());
        }
    }
}
