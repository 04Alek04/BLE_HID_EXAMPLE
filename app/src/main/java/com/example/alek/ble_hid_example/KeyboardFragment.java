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
import androidx.annotation.NonNull;
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

import com.example.alek.ble_hid_example.databinding.KeyboardFragmentBinding;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFragment extends Fragment implements Button.OnTouchListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private KeyboardFragmentBinding binding;

    public static KeyboardFragment newInstance() {
        return new KeyboardFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = KeyboardFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (binding == null) return;

        binding.keyboardButtonSend.setOnClickListener(this);

        List<String> kbList = new ArrayList<>();
        kbList.add("SELECT VALUE");
        kbList.addAll(KeyboardUsage.getUsageNames());

        binding.keyboardSpinnerCharacter.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter_kb = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item, kbList);
        dataAdapter_kb.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.keyboardSpinnerCharacter.setAdapter(dataAdapter_kb);
        binding.keyboardSpinnerCharacter.setSelection(0);

        final android.widget.EditText dummyEditText = new android.widget.EditText(getContext());
        dummyEditText.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        dummyEditText.setAlpha(0f);
        dummyEditText.setFocusable(true);
        dummyEditText.setFocusableInTouchMode(true);
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).addView(dummyEditText);
        }

        final long[] keyboardOpenTime = new long[]{0};

        binding.keyboardToggleKb.addOnCheckedChangeListener((buttonView, isChecked) -> {
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
        });

        final long[] editTextOpenTime = new long[]{0};

        binding.keyboardEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                editTextOpenTime[0] = System.currentTimeMillis();
            }
            return false;
        });

        binding.keyboardEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(binding.keyboardEditText.getWindowToken(), 0);
                }
            }
        });

        View.OnTouchListener touchOutsideListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (binding != null && binding.keyboardEditText.hasFocus()) {
                    binding.keyboardEditText.clearFocus();
                    if (getActivity() != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(binding.keyboardEditText.getWindowToken(), 0);
                        }
                    }
                }
            }
            return false;
        };

        view.setOnTouchListener(touchOutsideListener);
        binding.keyboardSubMainLayout.setOnTouchListener(touchOutsideListener);

        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (getActivity() == null || binding == null) return;
            long now = System.currentTimeMillis();
            if (now - keyboardOpenTime[0] < 500 || now - editTextOpenTime[0] < 500) {
                return;
            }
            android.graphics.Rect r = new android.graphics.Rect();
            view.getWindowVisibleDisplayFrame(r);
            int screenHeight = view.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;
            if (!isKeyboardVisible) {
                if (binding.keyboardToggleKb.isChecked()) {
                    binding.keyboardToggleKb.setChecked(false);
                    dummyEditText.clearFocus();
                }
                if (binding.keyboardEditText.hasFocus()) {
                    binding.keyboardEditText.clearFocus();
                }
            }
        });

        dummyEditText.addTextChangedListener(new TextWatcher() {
            private boolean isInternalChange = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInternalChange || binding == null) return;
                if (binding.keyboardToggleKb.isChecked() && count > before) {
                    String added = s.subSequence(start, start + count).toString();
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.sendKeyboardText(added);
                        isInternalChange = true;
                        dummyEditText.setText("");
                        isInternalChange = false;
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        dummyEditText.setOnKeyListener((v, keyCode, event) -> {
            if (binding != null && binding.keyboardToggleKb.isChecked() && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, HidKeyCodes.USAGE_BACKSPACE << 8);
                        activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                        return true;
                    }
                }
            }
            return false;
        });

        View.OnClickListener lockClickListener = v -> {
            byte usage = 0;
            int id = v.getId();
            if (id == R.id.keyboard_lock_num) {
                usage = HidKeyCodes.USAGE_NUM_LOCK;
            } else if (id == R.id.keyboard_lock_caps) {
                usage = HidKeyCodes.USAGE_CAPS_LOCK;
            } else if (id == R.id.keyboard_lock_scroll) {
                usage = HidKeyCodes.USAGE_SCROLL_LOCK;
            }

            if (usage != 0) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, usage << 8);
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                }
            }
        };

        binding.keyboardLockNum.setOnClickListener(lockClickListener);
        binding.keyboardLockCaps.setOnClickListener(lockClickListener);
        binding.keyboardLockScroll.setOnClickListener(lockClickListener);

        View.OnClickListener shortcutClickListener = v -> {
            byte usage = 0;
            int id = v.getId();
            if (id == R.id.kb_short_undo) usage = HidKeyCodes.USAGE_UNDO;
            else if (id == R.id.kb_short_redo) usage = HidKeyCodes.USAGE_REDO;
            else if (id == R.id.kb_short_save) usage = HidKeyCodes.USAGE_SAVE;
            else if (id == R.id.kb_short_all) usage = HidKeyCodes.USAGE_SELECT_ALL;
            else if (id == R.id.kb_short_cut) usage = HidKeyCodes.USAGE_CUT;
            else if (id == R.id.kb_short_copy) usage = HidKeyCodes.USAGE_COPY;
            else if (id == R.id.kb_short_paste) usage = HidKeyCodes.USAGE_PASTE;
            else if (id == R.id.kb_short_find) usage = HidKeyCodes.USAGE_FIND;
            else if (id == R.id.kb_short_replace) usage = HidKeyCodes.USAGE_REPLACE;

            if (usage != 0) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
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
        if (binding == null || getContext() == null) return;

        boolean numChecked = (status & HidKeyCodes.LED_NUM_LOCK) != 0;
        binding.keyboardLockNum.setChecked(numChecked);
        binding.keyboardLockNum.setBackgroundTintList(ColorStateList.valueOf(numChecked ?
                ContextCompat.getColor(getContext(), R.color.colorPrimary) : Color.TRANSPARENT));
        binding.keyboardLockNum.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));

        boolean capsChecked = (status & HidKeyCodes.LED_CAPS_LOCK) != 0;
        binding.keyboardLockCaps.setChecked(capsChecked);
        binding.keyboardLockCaps.setBackgroundTintList(ColorStateList.valueOf(capsChecked ?
                ContextCompat.getColor(getContext(), R.color.colorPrimary) : Color.TRANSPARENT));
        binding.keyboardLockCaps.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));

        boolean scrollChecked = (status & HidKeyCodes.LED_SCROLL_LOCK) != 0;
        binding.keyboardLockScroll.setChecked(scrollChecked);
        binding.keyboardLockScroll.setBackgroundTintList(ColorStateList.valueOf(scrollChecked ?
                ContextCompat.getColor(getContext(), R.color.colorPrimary) : Color.TRANSPARENT));
        binding.keyboardLockScroll.setTextColor(ContextCompat.getColor(getContext(), R.color.colorWhite));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) {
            binding.keyboardToggleKb.setChecked(false);
            binding.keyboardSpinnerCharacter.setSelection(0);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        short toSend = 0;
        int value = ConsumerControlUsage.getUsage(v.getId());
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || value == 0) return false;

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

        if (parent.getChildAt(0) != null) {
            ((TextView) parent.getChildAt(0)).setTextColor(ContextCompat.getColor(requireContext(), R.color.colorWhite));

            if (parent.getId() == R.id.keyboard_spinner_character && position > 0) {
                String selectedDescription = parent.getItemAtPosition(position).toString();
                KeyboardUsage kbUsage = KeyboardUsage.findByDescription(selectedDescription);
                if (kbUsage != null && activity != null) {
                    short value = (short) (kbUsage.meta + (kbUsage.usage << 8));
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, value);
                    activity.sendKeyboardNotification(ReportField.REPORT_FIELD_KEYBOARD_ALL, 0);
                }
                parent.setSelection(0);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (parent.getChildAt(0) != null) {
            ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
        }
    }

    @Override
    public void onClick(View v) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || binding == null) return;

        if (v.getId() == R.id.keyboard_button_send && binding.keyboardEditText.getText() != null) {
            activity.sendKeyboardText(binding.keyboardEditText.getText().toString());
            binding.keyboardEditText.setText("");
        }
    }
}
