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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

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
        View btnSend = view.findViewById(R.id.keyboard_button_send);
        if (btnSend != null) btnSend.setOnClickListener(this);

        // Keyboard character spinner configuration
        List<String> kbList = new ArrayList<>();
        kbList.add("SELECT VALUE");
        kbList.addAll(KeyboardUsage.getUsageNames());

        Spinner spinner_kb = view.findViewById(R.id.keyboard_spinner_character);
        if (spinner_kb != null) {
            spinner_kb.setOnItemSelectedListener(this);
            ArrayAdapter<String> dataAdapter_kb = new ArrayAdapter<>(getContext(),
                    R.layout.spinner_item, kbList);
            dataAdapter_kb.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinner_kb.setAdapter(dataAdapter_kb);
            spinner_kb.setSelection(0);
        }

        final TextInputEditText editText = view.findViewById(R.id.keyboard_edit_text);
        final MaterialButton toggleBtn = view.findViewById(R.id.keyboard_toggle_kb);

        // Dummy invisible input view for live typing so 'enter text' is NOT focused/highlighted
        final android.widget.EditText dummyEditText = new android.widget.EditText(getContext());
        dummyEditText.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        dummyEditText.setAlpha(0f);
        dummyEditText.setFocusable(true);
        dummyEditText.setFocusableInTouchMode(true);
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).addView(dummyEditText);
        }

        final long[] keyboardOpenTime = new long[]{0};

        toggleBtn.addOnCheckedChangeListener(
                new MaterialButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(MaterialButton buttonView, boolean isChecked) {
                        buttonView.setText(isChecked ? R.string.keyboard_on : R.string.keyboard_off);
                        InputMethodManager imm = getActivity() != null ? (InputMethodManager) getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE) : null;
                        
                        if (isChecked) {
                            keyboardOpenTime[0] = System.currentTimeMillis();
                            dummyEditText.requestFocus();
                            if (imm != null) {
                                imm.showSoftInput(dummyEditText, InputMethodManager.SHOW_IMPLICIT);
                            }
                        } else {
                            dummyEditText.clearFocus();
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(dummyEditText.getWindowToken(), 0);
                            }
                        }
                    }
                });

        final long[] editTextOpenTime = new long[]{0};

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                editTextOpenTime[0] = System.currentTimeMillis();
            }
            return false;
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            }
        });

        View.OnTouchListener touchOutsideListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (editText.hasFocus()) {
                    editText.clearFocus();
                    if (getActivity() != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        }
                    }
                }
            }
            return false;
        };

        view.setOnTouchListener(touchOutsideListener);
        View subMainLayout = view.findViewById(R.id.keyboard_sub_main_layout);
        if (subMainLayout != null) {
            subMainLayout.setOnTouchListener(touchOutsideListener);
        }

        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (getActivity() == null) return;
            long now = System.currentTimeMillis();
            // Grace period of 500ms after clicking Keyboard On or focusing editText to avoid closing while animating open
            if (now - keyboardOpenTime[0] < 500 || now - editTextOpenTime[0] < 500) {
                return;
            }
            android.graphics.Rect r = new android.graphics.Rect();
            view.getWindowVisibleDisplayFrame(r);
            int screenHeight = view.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;
            if (!isKeyboardVisible) {
                if (toggleBtn.isChecked()) {
                    toggleBtn.setChecked(false);
                    dummyEditText.clearFocus();
                }
                if (editText.hasFocus()) {
                    editText.clearFocus();
                }
            }
        });

        // Live typing logic on dummyEditText
        dummyEditText.addTextChangedListener(new TextWatcher() {
            private boolean isInternalChange = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInternalChange) return;
                if (toggleBtn.isChecked() && count > before) {
                    String added = s.subSequence(start, start + count).toString();
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.sendKeyboardText(added);
                        isInternalChange = true;
                        dummyEditText.setText(""); // Clear for next character
                        isInternalChange = false;
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle backspace in live mode
        dummyEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (toggleBtn.isChecked() && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DEL) {
                        MainActivity activity = (MainActivity) getActivity();
                        if (activity != null) {
                            activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0x2a << 8);
                            activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        numLockBtn = (MaterialButton) view.findViewById(R.id.keyboard_lock_num);
        capsLockBtn = (MaterialButton) view.findViewById(R.id.keyboard_lock_caps);
        scrollLockBtn = (MaterialButton) view.findViewById(R.id.keyboard_lock_scroll);

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
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, usage << 8);
                    // Send key release
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
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
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, val);
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                }
            }
        };

        int[] shortcutIds = {
                R.id.kb_short_undo, R.id.kb_short_redo, R.id.kb_short_save,
                R.id.kb_short_all, R.id.kb_short_cut, R.id.kb_short_copy,
                R.id.kb_short_paste, R.id.kb_short_find, R.id.kb_short_replace
        };
        for (int sId : shortcutIds) {
            View sBtn = view.findViewById(sId);
            if (sBtn != null) sBtn.setOnClickListener(shortcutClickListener);
        }

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
                    ContextCompat.getColor(getContext(), R.color.colorPrimary) : android.graphics.Color.TRANSPARENT));
            numLockBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));
        }
        if (capsLockBtn != null) {
            boolean isChecked = (status & 0x02) != 0;
            capsLockBtn.setChecked(isChecked);
            capsLockBtn.setBackgroundTintList(ColorStateList.valueOf(isChecked ? 
                    ContextCompat.getColor(getContext(), R.color.colorPrimary) : android.graphics.Color.TRANSPARENT));
            capsLockBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));
        }
        if (scrollLockBtn != null) {
            boolean isChecked = (status & 0x04) != 0;
            scrollLockBtn.setChecked(isChecked);
            scrollLockBtn.setBackgroundTintList(ColorStateList.valueOf(isChecked ? 
                    ContextCompat.getColor(getContext(), R.color.colorPrimary) : android.graphics.Color.TRANSPARENT));
            scrollLockBtn.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getView() != null) {
            MaterialButton toggleKb = getView().findViewById(R.id.keyboard_toggle_kb);
            if (toggleKb != null) toggleKb.setChecked(false);
            Spinner spinner_kb = getView().findViewById(R.id.keyboard_spinner_character);
            if (spinner_kb != null) {
                spinner_kb.setSelection(0);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        short toSend = 0;
        int value = ConsumerControlUsage.getUsage(v.getId());
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return false;

        if (value == 0) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            toSend |= value;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            toSend &= ~value;
            toSend &= 0xffff;
        } else {
            return false;
        }

        activity.sendConsumerNotification(ReportField.REPORT_FIELD_CONSUMER_CONTROL, toSend);

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        MainActivity activity = (MainActivity) getActivity();
        short value = 0;

        if (parent.getChildAt(0) != null) {
            ((TextView) parent.getChildAt(0)).setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));

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
                activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, value);
                activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);

                // Reset spinner selection to allow sending the same character again
                parent.setSelection(0);
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
        if (activity == null) return;

        if (v.getId() == R.id.keyboard_button_send && getView() != null) {
            TextInputEditText editText = getView().findViewById(R.id.keyboard_edit_text);
            if (editText != null && editText.getText() != null) {
                activity.sendKeyboardText(editText.getText().toString());
                editText.setText(""); // Clear field after sending
            }
        }
    }
}
