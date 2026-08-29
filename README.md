# BLE_HID_EXAMPLE
Example of HID over GATT (Bluetooth Low Energy) Android Application. The application emulates a peripheral BLE device (mouse, keyboard, gamepad, digitizer, and consumer remote) that can be connected to a PC, tablet, or smartphone.

The application consists of 7 full-screen fragments. When more than one device is connected, notifications can be routed to all connected devices simultaneously or toggled individually per host.

## Blockly fragment (Automation & Macros)
<img src="screenshots/blockly.jpg" width="300" />

- **Visual Programming Workspace** - drag-and-drop Google Blockly interface to build custom HID macros and automated scripts.
- **Rich Block Library** - includes blocks for keyboard text/keys, mouse movement, clicks, scrolling, absolute positioning, gamepad sticks/buttons, consumer media controls, delays, loops, and conditional logic.
- **Run / Stop Execution** - execute programmed scripts in real-time or stop execution with a single tap.
- **Save & Load** - save macros to internal storage and reload them whenever needed.
- **Stash Bar** - quick access bar for frequently used blocks.

## Configuration fragment
<img src="screenshots/configuration.jpg" width="300" />

- **Reboot application button** - restarts the application and the Bluetooth stack. Useful for refreshing the connection state if any issues occur.
- **Clear configs button** - deletes saved device pairings and configurations if connection issues occur.
- **List of connected devices with toggles** - displays connected hosts with individual switches to enable or disable HID notification routing per device.

## Consumer control fragment
<img src="screenshots/consumer.jpg" width="300" />

- **Music/player control** - buttons for previous track, stop, play/pause, and next track.
- **Volume & Brightness** - quick adjustments for sound and screen brightness.
- **System control** - perform system actions like sleep, hibernation, shut down, or restart.
- **Application launch** - start selected applications such as a calculator or web browser. Supported by specific operating systems.
- **Application control** - perform actions like "help" or "search" in the currently focused app. Note: these actions are highly application-specific.

## Mouse fragment
<img src="screenshots/mouse.jpg" width="300" />

- **Buttons (1, 2, 3, 4, 5)** - emulates standard mouse button clicks (Left, Right, Middle, Button 4, Button 5).
- **Touchpad area** - move the cursor; double-tap emulates a left-click.
- **Horizontal Scroll bar** - smooth horizontal panning/scrolling (h-scroll / tilt wheel).
- **Volume buttons** - the phone's physical volume up/down buttons emulate the vertical mouse scroll wheel.

## Keyboard fragment
<img src="screenshots/keyboard.jpg" width="300" />

- **Send text** - type a phrase and send it to the computer all at once.
- **Send character** - a dropdown list to select and send a single special character or function key.
- **Keyboard shortcuts** - dedicated buttons for common actions like Ctrl+C, Ctrl+V, Ctrl+Z, Ctrl+A, and more.
- **LED indicators** - shows the real-time status of Caps Lock, Num Lock, and Scroll Lock received from the host.
- **Keyboard On/Off** - enables or disables the software keyboard for direct live typing with smart Caps Lock synchronization.

## Gamepad fragment
<img src="screenshots/gamepad.jpg" width="300" />

- **Dual Analog Sticks / D-Pad** - virtual L-Stick and R-Stick with octagonal bases, double-tap for L3/R3 clicks, and toggles to switch between analog stick or digital D-Pad / button modes.
- **Action Buttons (BT0 - BT3)** - standard ABXY style action button diamond.
- **Shoulder Buttons & Triggers** - dedicated LB, RB, LT, and RT buttons.
- **Touchpad & System Keys** - integrated central touchpad area and MENU / START system buttons.
- **Control Toggles** - independent toggles for L-Stick, Touchpad, Tab Swiping Lock, and R-Stick.

## Absolute Mouse fragment
<img src="screenshots/absolute_mouse.jpg" width="300" />

- **Full-Screen Touch Surface** - absolute 0–32767 coordinate mapping for touchscreen and digitizer emulation.
- **Triple-Tap Exit** - triple-tap anywhere inside the touch frame to exit back to standard mouse mode.
- **Physical Volume Buttons** - physical volume up/down buttons map to primary and secondary touch/click states.
- **Real-Time Coordinate Feedback** - displays live X/Y absolute coordinates.

---

## What's new in version 2.5
- **Blockly Visual Automation Engine:** Integrated Google Blockly for creating, editing, saving, and executing automated HID macros and scripts (keyboard, mouse, gamepad, consumer controls, delays, loops, and conditional logic) over BLE.
- **Full Virtual Gamepad Mode:** Added a full-featured gamepad controller with dual analog sticks (L-Stick, R-Stick), D-Pad, L3/R3, ABXY buttons, LB/RB/LT/RT triggers, MENU/START, and an integrated touchpad.
- **Absolute Mouse / Digitizer:** Introduced a dedicated full-screen absolute positioning mode (0–32767 range) for touchscreen/digitizer emulation with a triple-tap exit gesture.
- **Horizontal Mouse Scroll:** Added horizontal scrolling/panning (h-scroll) support to the Mouse fragment.
- **Per-Device Management:** Ability to toggle HID notifications on or off for individual connected Bluetooth hosts directly from the configuration screen.
- **HID Battery Reporting:** Added Battery System HID report (Report ID 4) and live Battery Level service notifications.
- **Smart Keyboard Typing:** Smart Caps Lock synchronization with automatic Shift XOR logic and seamless background text input.
- **BLE Performance & Modern UI:** Implemented low-latency BLE connection requests (`HIGH_PRIORITY`) and redesigned the interface with a modern dark theme.

---

## What's new in version 2.0
- **AI-Driven Modernization:** The entire project was refactored and updated in collaboration with **Gemini in Android Studio**.
- **Android 14 Support:** Fully updated for the latest security and permission requirements.
- **Enhanced Keyboard:** Added LED status lights and productivity shortcuts (Ctrl+C, Ctrl+V, etc.).
- **Smoother Mouse:** Implemented motion merging to prevent cursor "jumping".
- **Better Stability:** New notification queue system to handle multiple connections reliably.
- **Simpler Experience:** Removed outdated "basic mode" and refactored the HID descriptor to use smaller, separate reports for better host compatibility.
