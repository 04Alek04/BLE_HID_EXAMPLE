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

public class ConsumerControlUsage {
    public static final String BRIGHT_UP = "Bright Up";
    public static final String BRIGHT_DOWN = "Bright Down";
    public static final String SCAN_NEXT_TRACK = "Scan Next Track";
    public static final String SCAN_PREVIOUS_TRACK = "Scan Previous Track";
    public static final String STOP = "Stop";
    public static final String PLAY_PAUSE = "Play/Pause";
    public static final String MUTE = "Mute";
    public static final String VOLUME_UP = "Volume UP";
    public static final String VOLUME_DOWN = "Volume Down";
    public static final String EJECT = "Eject";
    public static final String SNAPSHOT = "Snapshot";
    public static final String SYSTEM_SLEEP = "System Sleep";
    public static final String SYSTEM_HIBERNATE = "System Hibernate";
    public static final String SYSTEM_POWER_DOWN = "System Power Down";
    public static final String SYSTEM_COLD_RESTART = "System Cold Restart";
    public static final String SYSTEM_WARM_RESTART = "System Warm Restart";

    public static final ConsumerControlUsage[] AC_USAGES = {
            /* Consumer Control Page */
            new ConsumerControlUsage(0x0001, BRIGHT_UP),
            new ConsumerControlUsage(0x0002, BRIGHT_DOWN),
            new ConsumerControlUsage(0x0004, SCAN_NEXT_TRACK),
            new ConsumerControlUsage(0x0008, SCAN_PREVIOUS_TRACK),
            new ConsumerControlUsage(0x0010, STOP),
            new ConsumerControlUsage(0x0020, PLAY_PAUSE),
            new ConsumerControlUsage(0x0040, MUTE),
            new ConsumerControlUsage(0x0080, VOLUME_UP),
            new ConsumerControlUsage(0x0100, VOLUME_DOWN),
            new ConsumerControlUsage(0x0200, EJECT),
            new ConsumerControlUsage(0x0400, SNAPSHOT),

            /* Generic Desktop Page */
            new ConsumerControlUsage(0x0800, SYSTEM_SLEEP),
            new ConsumerControlUsage(0x1000, SYSTEM_HIBERNATE),
            new ConsumerControlUsage(0x2000, SYSTEM_POWER_DOWN),
            new ConsumerControlUsage(0x4000, SYSTEM_COLD_RESTART),
            new ConsumerControlUsage(0x8000, SYSTEM_WARM_RESTART),
    };
    public final short usage;
    public final String name;
    public int button_id;

    public ConsumerControlUsage(int b_id, int u, String n) {
        button_id = b_id;
        usage = (short) u;
        name = n;
    }

    public ConsumerControlUsage(int u, String n) {
        this(-1, u, n);
    }

    static short getUsage(int b_id) {
        for (ConsumerControlUsage AC_USAGE : AC_USAGES) {
            if (b_id == AC_USAGE.button_id) {
                return AC_USAGE.usage;
            }
        }

        return 0;
    }

    static public void bindButtonId(int id, String name) {
        for (ConsumerControlUsage AC_USAGE : AC_USAGES) {
            if (AC_USAGE.name.equals(name)) {
                AC_USAGE.button_id = id;
                return;
            }
        }
    }
}
