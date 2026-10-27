# =====================================================================
# Windows BLE HID Host Reconnect Script (Fast Attempt + Adapter Recovery)
# =====================================================================

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

if (-not $target) {
    Write-Host "No paired (HID) BLE device found." -ForegroundColor Red
    exit 1
}

Write-Host "Found device: $($target.Name)"

# =====================================================================
# ATTEMPT 1: Standard Reconnect (Original Method)
# =====================================================================
$ble = Await-WinRT ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromIdAsync($target.Id)) ([Windows.Devices.Bluetooth.BluetoothLEDevice])

if ($ble) {
    $res = Await-WinRT ($ble.GetGattServicesAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult])

    if ($res -and $res.Status -eq 'Success') {
        $count = @($res.Services).Count
        Write-Host "Connection Status: $($res.Status) (Found $count GATT Services)" -ForegroundColor Green
        exit 0
    } else {
        Write-Host "Initial connection attempt status: $($res.Status). Starting recovery mode..." -ForegroundColor Yellow
    }
}

# =====================================================================
# ATTEMPT 2: Recovery Mode (Windows Bluetooth Radio Adapter Reset)
# =====================================================================
Write-Host "=== Recovery Mode Activated ===" -ForegroundColor Cyan
Write-Host "Resetting Windows Bluetooth Radio Adapter & bthserv..." -ForegroundColor Yellow

# Restart bthserv Bluetooth Service
Restart-Service bthserv -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 600

# Reset Main Bluetooth Adapter PnP Device if accessible
$btAdapter = Get-PnpDevice -Class "Bluetooth" | Where-Object {
    $_.FriendlyName -like "*Bluetooth*Adapter*" -or
    $_.FriendlyName -like "*Intel*Bluetooth*" -or
    $_.FriendlyName -like "*Realtek*Bluetooth*" -or
    $_.FriendlyName -like "*Qualcomm*Bluetooth*" -or
    $_.FriendlyName -like "*Generic Bluetooth*"
} | Select-Object -First 1

if ($btAdapter) {
    Write-Host "Resetting Bluetooth Radio Adapter: $($btAdapter.FriendlyName)" -ForegroundColor Yellow
    $btAdapter | Disable-PnpDevice -Confirm:$false -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 600
    $btAdapter | Enable-PnpDevice -Confirm:$false -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 800
}

# Final Reconnect Attempt after Adapter Reset
$ble = Await-WinRT ([Windows.Devices.Bluetooth.BluetoothLEDevice]::FromIdAsync($target.Id)) ([Windows.Devices.Bluetooth.BluetoothLEDevice])
if ($ble) {
    $res = Await-WinRT ($ble.GetGattServicesAsync([Windows.Devices.Bluetooth.BluetoothCacheMode]::Uncached)) ([Windows.Devices.Bluetooth.GenericAttributeProfile.GattDeviceServicesResult])
    if ($res -and $res.Status -eq 'Success') {
        $count = @($res.Services).Count
        Write-Host "Connection Status: $($res.Status) (Found $count GATT Services - Recovered via Radio Adapter Reset)" -ForegroundColor Green
        exit 0
    }
}

Write-Host "Connection Status: Failed" -ForegroundColor Red
