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
import android.content.SharedPreferences;

class ApplicationConfiguration {
    final static public int FORCE_BOND_FEAT = 0x01;
    final static public int CONSUMER_FEAT = 0x02;
    final static public int MOUSE_FEAT = 0x03;
    final static public int KEYBOARD_FEAT = 0x04;
    final static public int BASIC_FEAT = 0x05;
    private final static int TRUE = 1;
    private final static int FALSE = 0;
    final static private int[][] FEAT_ID_ARRAY = {
            /* ID, string ID, default value */
            {FORCE_BOND_FEAT, R.string.saved_force_bond, FALSE},
            {CONSUMER_FEAT, R.string.saved_enable_consumer, TRUE},
            {MOUSE_FEAT, R.string.saved_enable_mouse, FALSE},
            {KEYBOARD_FEAT, R.string.saved_enable_keyboard, FALSE},
            {BASIC_FEAT, R.string.saved_basic_mode, TRUE},
    };

    static void initializeConfiguration(Context context) {
        // Get values if exist - set them otherwise, all are true by defaults
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.app_name_prefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        for (int[] elem : FEAT_ID_ARRAY) {
            int id = elem[1];
            int def_val = elem[2];

            if (!sharedPref.contains(context.getString(id))) {
                editor.putInt(context.getString(id), def_val);
            }
        }

        editor.apply();
    }

    static void setConfigurationField(Context context, int feature, boolean value) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.app_name_prefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        for (int[] pair : FEAT_ID_ARRAY) {
            int feat = pair[0];
            int id = pair[1];

            if (feat == feature) {
                editor.putInt(context.getString(id), value ? TRUE : FALSE);
                editor.apply();
                return;
            }
        }
    }

    static boolean getConfigurationField(Context context, int feature) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.app_name_prefs), Context.MODE_PRIVATE);

        for (int[] pair : FEAT_ID_ARRAY) {
            int feat = pair[0];
            int id = pair[1];

            if (feat == feature) {
                return (sharedPref.contains(context.getString(id))) &&
                        (sharedPref.getInt(context.getString(id), FALSE) == TRUE);
            }
        }

        return false;
    }
}
