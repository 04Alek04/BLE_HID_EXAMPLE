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

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

public class ConfigurationFragment extends Fragment implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

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

        view.findViewById(R.id.conf_reboot_button).setOnClickListener(this);
        view.findViewById(R.id.conf_clear_configs_button).setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.conf_reboot_button) {
            Intent i = getActivity().getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((MainActivity) getActivity()).restartGattDatabase();
            startActivity(i);
        } else if (v.getId() == R.id.conf_clear_configs_button) {
            ((MainActivity) getActivity()).clearDeviceConfigs();
        }
    }
}
