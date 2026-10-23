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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;

public class ConfigurationFragment extends Fragment implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private static final String SCRIPT_TEXT =
            "Add-Type -AssemblyName System.Runtime.WindowsRuntime\n" +
            "[void][Windows.Devices.Bluetooth.BluetoothLEDevice, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]\n" +
            "[void][Windows.Devices.Enumeration.DeviceInformation, Windows.Devices.Enumeration, ContentType = WindowsRuntime]\n" +
            "[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]\n\n" +
            "$m = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n\n" +
            "function Await-WinRT ($op, [type]$t) {\n" +
            "    try {\n" +
            "        $task = $m.MakeGenericMethod($t).Invoke($null, @($op))\n" +
            "        $task.Wait()\n" +
            "        return $task.Result\n" +
            "    } catch {\n" +
            "        return $null\n" +
            "    }\n" +
            "}\n\n" +
            "Write-Host \"Searching for paired BLE HID devices...\"\n" +
            "$sel = [Windows.Devices.Bluetooth.BluetoothLEDevice]::GetDeviceSelectorFromPairingState($true)\n" +
            "$devices = Await-WinRT ([Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($sel)) ([Windows.Devices.Enumeration.DeviceInformationCollection])\n\n" +
            "$target = $devices | Where-Object { $_.Name -like \"*(HID)*\" } | Select-Object -First 1\n\n" +
            "if ($target) {\n" +
            "    Write-Host \"Found device: $($target.Name)\"\n" +
            "    $ble = Await-WinRT ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromIdAsync($target.Id)) ([Windows.Devices.Bluetooth.BluetoothLEDevice])\n" +
            "    if ($ble) {\n" +
            "        $res = Await-WinRT ($ble.GetGattServicesAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult])\n" +
            "        Write-Host \"Connection Status: $($res.Status)\"\n" +
            "    }\n" +
            "} else {\n" +
            "    Write-Host \"No paired (HID) BLE device found.\"\n" +
            "}\n";

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
        view.findViewById(R.id.conf_type_script_button).setOnClickListener(this);

        SwitchCompat restoreNameSwitch = view.findViewById(R.id.conf_restore_name_switch);
        if (restoreNameSwitch != null && getActivity() != null) {
            AppPreferencesManager prefsManager = AppPreferencesManager.getInstance(getActivity());
            restoreNameSwitch.setChecked(prefsManager.isRestoreNameEnabled());
            restoreNameSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefsManager.setRestoreNameEnabled(isChecked)
            );
        }
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
        } else if (v.getId() == R.id.conf_type_script_button) {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).sendKeyboardText(SCRIPT_TEXT, () -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Script sent successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
