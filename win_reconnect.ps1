Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Devices.Bluetooth.BluetoothLEDevice, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]
[void][Windows.Devices.Enumeration.DeviceInformation, Windows.Devices.Enumeration, ContentType = WindowsRuntime]
[void][Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult, Windows.Devices.Bluetooth, ContentType = WindowsRuntime]

$m = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]

function Await-WinRT ($op, [type]$t) {
    try {
        $task = $m.MakeGenericMethod($t).Invoke($null, @($op))
        $task.Wait()
        return $task.Result
    } catch {
        return $null
    }
}

Write-Host "Searching for paired BLE HID devices..."
$sel = [Windows.Devices.Bluetooth.BluetoothLEDevice]::GetDeviceSelectorFromPairingState($true)
$devices = Await-WinRT ([Windows.Devices.Enumeration.DeviceInformation]::FindAllAsync($sel)) ([Windows.Devices.Enumeration.DeviceInformationCollection])

$target = $devices | Where-Object { $_.Name -like "*(HID)*" } | Select-Object -First 1

if ($target) {
    Write-Host "Found device: $($target.Name)"
    $ble = Await-WinRT ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromIdAsync($target.Id)) ([Windows.Devices.Bluetooth.BluetoothLEDevice])
    if ($ble) {
        $res = Await-WinRT ($ble.GetGattServicesAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult])
        Write-Host "Connection Status: $($res.Status)"
    }
} else {
    Write-Host "No paired (HID) BLE device found."
}
