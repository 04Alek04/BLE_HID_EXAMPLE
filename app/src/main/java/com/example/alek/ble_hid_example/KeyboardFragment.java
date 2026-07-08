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

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyboardFragment extends Fragment implements Button.OnTouchListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private MaterialButton numLockBtn, capsLockBtn, scrollLockBtn;

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

        ((MaterialButton) getActivity().findViewById(R.id.keyboard_toggle_kb)).addOnCheckedChangeListener(
                new MaterialButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(MaterialButton buttonView, boolean isChecked) {
                        buttonView.setText(isChecked ? R.string.keyboard_on : R.string.keyboard_off);
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        View viewToFocus = getActivity().findViewById(R.id.keyboard_edit_text);
                        if (viewToFocus == null) return;

                        if (isChecked) {
                            viewToFocus.requestFocus();
                            imm.showSoftInput(viewToFocus, InputMethodManager.SHOW_IMPLICIT);
                        } else {
                            imm.hideSoftInputFromWindow(viewToFocus.getWindowToken(), 0);
                        }
                    }
                });

        numLockBtn = (MaterialButton) getActivity().findViewById(R.id.keyboard_lock_num);
        capsLockBtn = (MaterialButton) getActivity().findViewById(R.id.keyboard_lock_caps);
        scrollLockBtn = (MaterialButton) getActivity().findViewById(R.id.keyboard_lock_scroll);

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

        View.OnClickListener shortcutClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte usage = 0;
                int id = v.getId();
                if (id == R.id.kb_short_undo) usage = 0x1d; // Z
                else if (id == R.id.kb_short_redo) usage = 0x1c; // Y
                else if (id == R.id.kb_short_save) usage = 0x16; // S
                else if (id == R.id.kb_short_all) usage = 0x04; // A
                else if (id == R.id.kb_short_cut) usage = 0x1b; // X
                else if (id == R.id.kb_short_copy) usage = 0x06; // C
                else if (id == R.id.kb_short_paste) usage = 0x19; // V
                else if (id == R.id.kb_short_find) usage = 0x09; // F
                else if (id == R.id.kb_short_replace) usage = 0x0b; // H

                if (usage != 0) {
                    MainActivity activity = (MainActivity) getActivity();
                    short val = (short) (KeyboardUsage.META_LEFT_CONTROL | (usage << 8));
                    activity.sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, (int) val);
                    activity.sendNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                }
            }
        };

        getActivity().findViewById(R.id.kb_short_undo).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_redo).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_save).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_all).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_cut).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_copy).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_paste).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_find).setOnClickListener(shortcutClickListener);
        getActivity().findViewById(R.id.kb_short_replace).setOnClickListener(shortcutClickListener);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            updateLedStatus(activity.getLastKeyboardLedStatus());
        }
    }

    public void updateLedStatus(byte status) {
        if (numLockBtn != null) {
            boolean isChecked = (status & 0x01) != 0;
            numLockBtn.setChecked(isChecked);
            numLockBtn.setBackgroundTintList(ColorStateList.valueOf(isChecked ? 
                    ContextCompat.getColor(getContext(), R.color.colorPrimary) : Color.TRANSPARENT));
            numLockBtn.setTextColor(isChecked ? Color.WHITE : 
                    ContextCompat.getColor(getContext(), R.color.colorWhite));
        }
        if (capsLockBtn != null) {
            boolean isChecked = (status & 0x02) != 0;
            capsLockBtn.setChecked(isChecked);
            capsLockBtn.setBackgroundTintList(ColorStateList.valueOf(isChecked ? 
                    ContextCompat.getColor(getContext(), R.color.colorPrimary) : Color.TRANSPARENT));
            capsLockBtn.setTextColor(isChecked ? Color.WHITE : 
                    ContextCompat.getColor(getContext(), R.color.colorWhite));
        }
        if (scrollLockBtn != null) {
            boolean isChecked = (status & 0x04) != 0;
            scrollLockBtn.setChecked(isChecked);
            scrollLockBtn.setBackgroundTintList(ColorStateList.valueOf(isChecked ? 
                    ContextCompat.getColor(getContext(), R.color.colorPrimary) : Color.TRANSPARENT));
            scrollLockBtn.setTextColor(isChecked ? Color.WHITE : 
                    ContextCompat.getColor(getContext(), R.color.colorWhite));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        ((MaterialButton) getActivity().findViewById(R.id.keyboard_toggle_kb)).setChecked(false);
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
            activity.sendNotification(((TextInputEditText) getActivity().findViewById(
                    R.id.keyboard_edit_text)).getText().toString());
        }
    }
}
