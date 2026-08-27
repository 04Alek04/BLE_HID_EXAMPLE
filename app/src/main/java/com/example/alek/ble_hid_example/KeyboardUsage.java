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

import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

final class KeyboardUsage {
    static final byte META_LEFT_CONTROL = (byte) 0x01;
    static final byte META_LEFT_SHIFT = (byte) 0x02;
    static final byte META_LEFT_ALT = (byte) 0x04;
    static final byte META_LEFT_META = (byte) 0x08;
    static final byte META_RIGHT_ALT = (byte) 0x40;

    static final KeyboardUsage[] KEYBOARD_USAGES = {
            new KeyboardUsage(KeyEvent.KEYCODE_A, (byte) 0x04, 'a'),
            new KeyboardUsage(KeyEvent.KEYCODE_B, (byte) 0x05, 'b'),
            new KeyboardUsage(KeyEvent.KEYCODE_C, (byte) 0x06, 'c'),
            new KeyboardUsage(KeyEvent.KEYCODE_D, (byte) 0x07, 'd'),
            new KeyboardUsage(KeyEvent.KEYCODE_E, (byte) 0x08, 'e'),
            new KeyboardUsage(KeyEvent.KEYCODE_F, (byte) 0x09, 'f'),
            new KeyboardUsage(KeyEvent.KEYCODE_G, (byte) 0x0a, 'g'),
            new KeyboardUsage(KeyEvent.KEYCODE_H, (byte) 0x0b, 'h'),
            new KeyboardUsage(KeyEvent.KEYCODE_I, (byte) 0x0c, 'i'),
            new KeyboardUsage(KeyEvent.KEYCODE_J, (byte) 0x0d, 'j'),
            new KeyboardUsage(KeyEvent.KEYCODE_K, (byte) 0x0e, 'k'),
            new KeyboardUsage(KeyEvent.KEYCODE_L, (byte) 0x0f, 'l'),
            new KeyboardUsage(KeyEvent.KEYCODE_M, (byte) 0x10, 'm'),
            new KeyboardUsage(KeyEvent.KEYCODE_N, (byte) 0x11, 'n'),
            new KeyboardUsage(KeyEvent.KEYCODE_O, (byte) 0x12, 'o'),
            new KeyboardUsage(KeyEvent.KEYCODE_P, (byte) 0x13, 'p'),
            new KeyboardUsage(KeyEvent.KEYCODE_Q, (byte) 0x14, 'q'),
            new KeyboardUsage(KeyEvent.KEYCODE_R, (byte) 0x15, 'r'),
            new KeyboardUsage(KeyEvent.KEYCODE_S, (byte) 0x16, 's'),
            new KeyboardUsage(KeyEvent.KEYCODE_T, (byte) 0x17, 't'),
            new KeyboardUsage(KeyEvent.KEYCODE_U, (byte) 0x18, 'u'),
            new KeyboardUsage(KeyEvent.KEYCODE_V, (byte) 0x19, 'v'),
            new KeyboardUsage(KeyEvent.KEYCODE_W, (byte) 0x1a, 'w'),
            new KeyboardUsage(KeyEvent.KEYCODE_X, (byte) 0x1b, 'x'),
            new KeyboardUsage(KeyEvent.KEYCODE_Y, (byte) 0x1c, 'y'),
            new KeyboardUsage(KeyEvent.KEYCODE_Z, (byte) 0x1d, 'z'),
            new KeyboardUsage(KeyEvent.KEYCODE_1, (byte) 0x1e, '1'),
            new KeyboardUsage(KeyEvent.KEYCODE_2, (byte) 0x1f, '2'),
            new KeyboardUsage(KeyEvent.KEYCODE_3, (byte) 0x20, '3'),
            new KeyboardUsage(KeyEvent.KEYCODE_4, (byte) 0x21, '4'),
            new KeyboardUsage(KeyEvent.KEYCODE_5, (byte) 0x22, '5'),
            new KeyboardUsage(KeyEvent.KEYCODE_6, (byte) 0x23, '6'),
            new KeyboardUsage(KeyEvent.KEYCODE_7, (byte) 0x24, '7'),
            new KeyboardUsage(KeyEvent.KEYCODE_8, (byte) 0x25, '8'),
            new KeyboardUsage(KeyEvent.KEYCODE_9, (byte) 0x26, '9'),
            new KeyboardUsage(KeyEvent.KEYCODE_0, (byte) 0x27, '0'),
            new KeyboardUsage(KeyEvent.KEYCODE_ENTER, (byte) 0x28, '\n', "Enter", true),
            new KeyboardUsage(KeyEvent.KEYCODE_ESCAPE, (byte) 0x29, '\0', "Escape", true),
            new KeyboardUsage(KeyEvent.KEYCODE_DEL, (byte) 0x2a, '\0', "Backspace", true),
            new KeyboardUsage(KeyEvent.KEYCODE_TAB, (byte) 0x2b, '\t', "TAB", true),
            new KeyboardUsage(KeyEvent.KEYCODE_SPACE, (byte) 0x2c, ' '),
            new KeyboardUsage(KeyEvent.KEYCODE_MINUS, (byte) 0x2d, '-'),
            new KeyboardUsage(KeyEvent.KEYCODE_EQUALS, (byte) 0x2e, '='),
            new KeyboardUsage(KeyEvent.KEYCODE_LEFT_BRACKET, (byte) 0x2f, '['),
            new KeyboardUsage(KeyEvent.KEYCODE_RIGHT_BRACKET, (byte) 0x30, ']'),
            new KeyboardUsage(KeyEvent.KEYCODE_BACKSLASH, (byte) 0x31, '\\'),
            new KeyboardUsage(KeyEvent.KEYCODE_BACKSLASH, (byte) 0x32, '\\'),
            new KeyboardUsage(KeyEvent.KEYCODE_SEMICOLON, (byte) 0x33, ';'),
            new KeyboardUsage(KeyEvent.KEYCODE_APOSTROPHE, (byte) 0x34, '\''),
            new KeyboardUsage(KeyEvent.KEYCODE_GRAVE, (byte) 0x35, '`'),
            new KeyboardUsage(KeyEvent.KEYCODE_COMMA, (byte) 0x36, ','),
            new KeyboardUsage(KeyEvent.KEYCODE_NUMPAD_DOT, (byte) 0x37, '.'),
            new KeyboardUsage(KeyEvent.KEYCODE_PERIOD, (byte) 0x37, '.'),
            new KeyboardUsage(KeyEvent.KEYCODE_SLASH, (byte) 0x38, '/'),
            new KeyboardUsage("CapsLock", (byte) 0x39, '\0', false), // Hidden from spinner
            new KeyboardUsage(KeyEvent.KEYCODE_F1, (byte) 0x3a, '\0', "F1", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F2, (byte) 0x3b, '\0', "F2", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F3, (byte) 0x3c, '\0', "F3", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F4, (byte) 0x3d, '\0', "F4", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F5, (byte) 0x3e, '\0', "F5", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F6, (byte) 0x3f, '\0', "F6", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F7, (byte) 0x40, '\0', "F7", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F8, (byte) 0x41, '\0', "F8", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F9, (byte) 0x42, '\0', "F9", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F10, (byte) 0x43, '\0', "F10", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F11, (byte) 0x44, '\0', "F11", true),
            new KeyboardUsage(KeyEvent.KEYCODE_F12, (byte) 0x45, '\0', "F12", true),
            new KeyboardUsage(KeyEvent.KEYCODE_SYSRQ, (byte) 0x46, '\0', "SysRq", true),
            new KeyboardUsage("ScrollLock", (byte) 0x47, '\0', false), // Hidden
            new KeyboardUsage("Pause", (byte) 0x48, '\0', (byte) 0, true),
            new KeyboardUsage(KeyEvent.KEYCODE_INSERT, (byte) 0x49, '\0', "Insert", true),
            new KeyboardUsage(KeyEvent.KEYCODE_MOVE_HOME, (byte) 0x4a, '\0', "Home", true),
            new KeyboardUsage(KeyEvent.KEYCODE_PAGE_UP, (byte) 0x4b, '\0', "Page Up", true),
            new KeyboardUsage(KeyEvent.KEYCODE_FORWARD_DEL, (byte) 0x4c, '\0', "Delete", true),
            new KeyboardUsage(KeyEvent.KEYCODE_MOVE_END, (byte) 0x4d, '\0', "End", true),
            new KeyboardUsage(KeyEvent.KEYCODE_PAGE_DOWN, (byte) 0x4e, '\0', "Page Down", true),
            new KeyboardUsage(KeyEvent.KEYCODE_DPAD_RIGHT, (byte) 0x4f, '\0', "Right", true),
            new KeyboardUsage(KeyEvent.KEYCODE_DPAD_LEFT, (byte) 0x50, '\0', "Left", true),
            new KeyboardUsage(KeyEvent.KEYCODE_DPAD_DOWN, (byte) 0x51, '\0', "Down", true),
            new KeyboardUsage(KeyEvent.KEYCODE_DPAD_UP, (byte) 0x52, '\0', "Up", true),
            new KeyboardUsage("NumLock", (byte) 0x53, '\0', false), // Hidden
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x54, '\0', "KP /", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x55, '\0', "KP *", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x56, '\0', "KP -", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x57, '\0', "KP +", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x58, '\0', "KP Enter", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x59, '\0', "KP 1", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x5a, '\0', "KP 2", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x5b, '\0', "KP 3", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x5c, '\0', "KP 4", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x5d, '\0', "KP 5", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x5e, '\0', "KP 6", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x5f, '\0', "KP 7", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x60, '\0', "KP 8", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x61, '\0', "KP 9", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x62, '\0', "KP 0", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x63, '\0', "KP .", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x68, '\0', "F13", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x69, '\0', "F14", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x6a, '\0', "F15", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x6b, '\0', "F16", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x6c, '\0', "F17", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x6d, '\0', "F18", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x6e, '\0', "F19", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x6f, '\0', "F20", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x70, '\0', "F21", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x71, '\0', "F22", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x72, '\0', "F23", true),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x73, '\0', "F24", true),

            // Common + META keys (HIDDEN FROM SPINNER)
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x1e, '!', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_AT, (byte) 0x1f, '@', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_POUND, (byte) 0x20, '#', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x21, '$', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x22, '%', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x23, '^', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x24, '&', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_STAR, (byte) 0x25, '*', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x26, '(', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x27, ')', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x2d, '_', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_PLUS, (byte) 0x2e, '+', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x2f, '{', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x30, '}', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x31, '|', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x33, ':', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x34, '"', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x35, '~', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x36, '<', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x37, '>', META_LEFT_SHIFT, false),
            new KeyboardUsage(KeyEvent.KEYCODE_UNKNOWN, (byte) 0x38, '?', META_LEFT_SHIFT, false),

            // Polish letters (HIDDEN FROM SPINNER)
            new KeyboardUsage("ą", (byte) 0x04, 'ą', META_RIGHT_ALT, false),
            new KeyboardUsage("ć", (byte) 0x06, 'ć', META_RIGHT_ALT, false),
            new KeyboardUsage("ę", (byte) 0x08, 'ę', META_RIGHT_ALT, false),
            new KeyboardUsage("ł", (byte) 0x0f, 'ł', META_RIGHT_ALT, false),
            new KeyboardUsage("ń", (byte) 0x11, 'ń', META_RIGHT_ALT, false),
            new KeyboardUsage("ó", (byte) 0x12, 'ó', META_RIGHT_ALT, false),
            new KeyboardUsage("ś", (byte) 0x16, 'ś', META_RIGHT_ALT, false),
            new KeyboardUsage("ź", (byte) 0x1b, 'ź', META_RIGHT_ALT, false),
            new KeyboardUsage("ż", (byte) 0x1d, 'ż', META_RIGHT_ALT, false),
    };
    public final String description;
    final int key_code;
    final byte usage;
    final char character;
    final byte meta;
    final boolean non_keyboard;

    private KeyboardUsage(int kc, byte u, char c, byte m, boolean nk) {
        key_code = kc;
        usage = u;
        character = c;
        meta = m;
        non_keyboard = nk;
        description = (nk && c == '\0') ? "" : "" + character;
    }

    private KeyboardUsage(String d, byte u, char c, byte m, boolean nk) {
        key_code = KeyEvent.KEYCODE_UNKNOWN;
        usage = u;
        character = c;
        meta = m;
        non_keyboard = nk;
        description = d;
    }

    private KeyboardUsage(int kc, byte u, char c, String d, boolean nk) {
        key_code = kc;
        usage = u;
        character = c;
        meta = (byte) 0;
        non_keyboard = nk;
        description = d;
    }

    private KeyboardUsage(String d, byte u, char c, boolean nk) {
        this(d, u, c, (byte) 0, nk);
    }

    private KeyboardUsage(int kc, byte u, char c) {
        this(kc, u, c, (byte) 0, false);
    }

    static public List<String> getUsageNames() {
        List<String> l = new ArrayList<>();
        for (KeyboardUsage KEYBOARD_USAGE : KEYBOARD_USAGES) {
            // Only function keys with a readable description
            if (KEYBOARD_USAGE.non_keyboard && KEYBOARD_USAGE.description.length() > 1) {
                l.add(KEYBOARD_USAGE.description);
            }
        }
        return l;
    }
}
