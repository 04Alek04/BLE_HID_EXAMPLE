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

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class ConfigurationFragment extends Fragment implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private static final String BASIC_STRING_ENABLED = "Consumer";
    private static final String BASIC_STRING_DISABLED = BASIC_STRING_ENABLED +
            " + Mouse\nKeyboard + D-Pad";

    public static ConfigurationFragment newInstance() {
        return new ConfigurationFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.configuration_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int[][] ids = {
                {R.id.conf_pair_switch, ApplicationConfiguration.FORCE_BOND_FEAT},
                {R.id.conf_basic_switch, ApplicationConfiguration.BASIC_FEAT},
        };

        for (int[] pair : ids) {
            Switch sw = (Switch) getActivity().findViewById(pair[0]);

            if (sw == null) {
                continue;
            }

            sw.setChecked(ApplicationConfiguration.getConfigurationField(getContext(), pair[1]));
            sw.setOnCheckedChangeListener(this);
        }

        ((TextView) getActivity().findViewById(R.id.conf_feat_text)).setText(((Switch)
                getActivity().findViewById(R.id.conf_basic_switch)).isChecked() ?
                BASIC_STRING_ENABLED : BASIC_STRING_DISABLED);

        getActivity().findViewById(R.id.conf_reboot_button).setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        MainActivity mainActivity = (MainActivity) getActivity();

        int id = buttonView.getId();
        if (id == R.id.conf_pair_switch) {
            ApplicationConfiguration.setConfigurationField(getContext(),
                    ApplicationConfiguration.FORCE_BOND_FEAT, isChecked);
        } else if (id == R.id.conf_basic_switch) {
            ApplicationConfiguration.setConfigurationField(getContext(),
                    ApplicationConfiguration.BASIC_FEAT, isChecked);

            ApplicationConfiguration.setConfigurationField(getContext(),
                    ApplicationConfiguration.CONSUMER_FEAT, true);

            ApplicationConfiguration.setConfigurationField(getContext(),
                    ApplicationConfiguration.MOUSE_FEAT, !isChecked);

            ApplicationConfiguration.setConfigurationField(getContext(),
                    ApplicationConfiguration.KEYBOARD_FEAT, !isChecked);

            ((TextView) getActivity().findViewById(R.id.conf_feat_text)).setText(isChecked ?
                    BASIC_STRING_ENABLED : BASIC_STRING_DISABLED);
        } else {
            return;
        }

        mainActivity.pager.getAdapter().notifyDataSetChanged();
        mainActivity.pager.setAdapter(mainActivity.pagerAdapter);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.conf_reboot_button) {
            Intent i = getActivity().getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((MainActivity) getActivity()).restartGattDatabase();
            startActivity(i);
        }
    }
}
