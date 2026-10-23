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

import java.util.ArrayList;
import java.util.List;

public class ApplicationLaunchButtonsUsage {
    public static final ApplicationLaunchButtonsUsage[] AL_USAGES = {
            new ApplicationLaunchButtonsUsage(0x0181, "Configuration Tool"),
            new ApplicationLaunchButtonsUsage(0x0182, "Programmable Button Configuration "),
            new ApplicationLaunchButtonsUsage(0x0183, "Consumer Control Configuration"),
            new ApplicationLaunchButtonsUsage(0x0184, "Word Processor"),
            new ApplicationLaunchButtonsUsage(0x0185, "Text Editor"),
            new ApplicationLaunchButtonsUsage(0x0186, "Spreadsheet"),
            new ApplicationLaunchButtonsUsage(0x0187, "Graphics Editor"),
            new ApplicationLaunchButtonsUsage(0x0188, "Presentation App"),
            new ApplicationLaunchButtonsUsage(0x0189, "Database App "),
            new ApplicationLaunchButtonsUsage(0x018A, "Email Reader"),
            new ApplicationLaunchButtonsUsage(0x018B, "Newsreader"),
            new ApplicationLaunchButtonsUsage(0x018C, "Voicemail"),
            new ApplicationLaunchButtonsUsage(0x018D, "Contacts/Address Book"),
            new ApplicationLaunchButtonsUsage(0x018E, "Calendar/Schedule "),
            new ApplicationLaunchButtonsUsage(0x018F, "Task/Project Manager"),
            new ApplicationLaunchButtonsUsage(0x0190, "Log/Journal/Timecard"),
            new ApplicationLaunchButtonsUsage(0x0191, "Checkbook/Finance"),
            new ApplicationLaunchButtonsUsage(0x0192, "Calculator"),
            new ApplicationLaunchButtonsUsage(0x0193, "A/V Capture/Playback"),
            new ApplicationLaunchButtonsUsage(0x0194, "Local Machine Browser"),
            new ApplicationLaunchButtonsUsage(0x0195, "LAN/WAN Browser"),
            new ApplicationLaunchButtonsUsage(0x0196, "Internet Browser"),
            new ApplicationLaunchButtonsUsage(0x0197, "Remote Networking/ISP Connect "),
            new ApplicationLaunchButtonsUsage(0x0198, "Network Conference"),
            new ApplicationLaunchButtonsUsage(0x0199, "Network Chat"),
            new ApplicationLaunchButtonsUsage(0x019A, "Telephony/Dialer"),
            new ApplicationLaunchButtonsUsage(0x019B, "Logon"),
            new ApplicationLaunchButtonsUsage(0x019C, "Logoff"),
            new ApplicationLaunchButtonsUsage(0x019D, "Logon/Logoff"),
            new ApplicationLaunchButtonsUsage(0x019E, "Terminal Lock/Screensaver"),
            new ApplicationLaunchButtonsUsage(0x019F, "Control Panel"),
            new ApplicationLaunchButtonsUsage(0x01A0, "Command Line Processor/Run"),
            new ApplicationLaunchButtonsUsage(0x01A1, "Process/Task Manager"),
            new ApplicationLaunchButtonsUsage(0x01A2, "Select Task/Application"),
            new ApplicationLaunchButtonsUsage(0x01A3, "Next Task/Application"),
            new ApplicationLaunchButtonsUsage(0x01A4, "Previous Task/Application"),
            new ApplicationLaunchButtonsUsage(0x01A5, "Preemptive Halt Task/Application"),
            new ApplicationLaunchButtonsUsage(0x01A6, "Integrated Help Center"),
            new ApplicationLaunchButtonsUsage(0x01A7, "Documents"),
            new ApplicationLaunchButtonsUsage(0x01A8, "Thesaurus"),
            new ApplicationLaunchButtonsUsage(0x01A9, "Dictionary"),
            new ApplicationLaunchButtonsUsage(0x01AA, "Desktop"),
            new ApplicationLaunchButtonsUsage(0x01AB, "Spell Check"),
            new ApplicationLaunchButtonsUsage(0x01AC, "Grammar Check"),
            new ApplicationLaunchButtonsUsage(0x01AD, "Wireless Status"),
            new ApplicationLaunchButtonsUsage(0x01AE, "Keyboard Layout"),
            new ApplicationLaunchButtonsUsage(0x01AF, "Virus Protection"),
            new ApplicationLaunchButtonsUsage(0x01B0, "Encryption"),
            new ApplicationLaunchButtonsUsage(0x01B1, "Screen Saver"),
            new ApplicationLaunchButtonsUsage(0x01B2, "Alarms"),
            new ApplicationLaunchButtonsUsage(0x01B3, "Clock"),
            new ApplicationLaunchButtonsUsage(0x01B4, "File Browser"),
            new ApplicationLaunchButtonsUsage(0x01B5, "Power Status"),
            new ApplicationLaunchButtonsUsage(0x01B6, "Image Browser"),
            new ApplicationLaunchButtonsUsage(0x01B7, "Audio Browser"),
            new ApplicationLaunchButtonsUsage(0x01B8, "Movie Browser"),
            new ApplicationLaunchButtonsUsage(0x01B9, "Digital Rights Manager"),
            new ApplicationLaunchButtonsUsage(0x01BA, "Digital Wallet"),
//        new ApplicationLaunchButtonsUsage(0x01BB, "Reserved"),
            new ApplicationLaunchButtonsUsage(0x01BC, "Instant Messaging"),
            new ApplicationLaunchButtonsUsage(0x01BD, "OEM Features/ Tips/Tutorial Browser"),
            new ApplicationLaunchButtonsUsage(0x01BE, "OEM Help"),
            new ApplicationLaunchButtonsUsage(0x01BF, "Online Community"),
            new ApplicationLaunchButtonsUsage(0x01C0, "Entertainment Content Browser "),
            new ApplicationLaunchButtonsUsage(0x01C1, "Online Shopping Browser"),
            new ApplicationLaunchButtonsUsage(0x01C2, "SmartCard Information/Help"),
            new ApplicationLaunchButtonsUsage(0x01C3, "Market Monitor/Finance Browser"),
            new ApplicationLaunchButtonsUsage(0x01C4, "Customized Corporate News Browser"),
            new ApplicationLaunchButtonsUsage(0x01C5, "Online Activity Browser"),
            new ApplicationLaunchButtonsUsage(0x01C6, "Research/Search Browser"),
            new ApplicationLaunchButtonsUsage(0x01C7, "Audio Player"),
    };
    public final short usage;
    private final String name;

    private ApplicationLaunchButtonsUsage(int u, String n) {
        usage = (short) u;
        name = n;
    }

    public static ApplicationLaunchButtonsUsage getUsageByPosition(int position) {
        if (position > 0 && position <= AL_USAGES.length) {
            return AL_USAGES[position - 1];
        }
        return null;
    }

    public static List<String> getUsageNames() {
        List<String> l = new ArrayList<>();

        for (ApplicationLaunchButtonsUsage AL_USAGE : AL_USAGES) {
            l.add(AL_USAGE.name);
        }

        return l;
    }
}
