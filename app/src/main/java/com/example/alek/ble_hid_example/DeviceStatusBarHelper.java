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

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public class DeviceStatusBarHelper {

    private static final int[] STATUS_BAR_TEXT_VIEW_IDS = {
            R.id.cons_text_device_info,
            R.id.mouse_text_device_info,
            R.id.keyboard_text_device_info,
            R.id.gamepad_text_device_info,
            R.id.abs_mouse_text_device_info,
            R.id.conf_text_device_info
    };

    private final Activity activity;

    public DeviceStatusBarHelper(Activity activity) {
        this.activity = activity;
    }

    public void updateStatusText(final String text, final int textColor) {
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            for (int id : STATUS_BAR_TEXT_VIEW_IDS) {
                TextView textView = activity.findViewById(id);
                if (textView != null) {
                    textView.setVisibility(View.VISIBLE);
                    textView.setTextColor(textColor);
                    textView.setText(text);
                }
            }
        });
    }
}
