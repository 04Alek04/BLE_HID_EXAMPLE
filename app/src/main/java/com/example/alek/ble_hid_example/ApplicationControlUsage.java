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

public class ApplicationControlUsage {
    public static final ApplicationControlUsage[] AC_USAGES = {
            new ApplicationControlUsage(0x0201, "New"),
            new ApplicationControlUsage(0x0202, "Open"),
            new ApplicationControlUsage(0x0203, "Close"),
            new ApplicationControlUsage(0x0204, "Exit"),
            new ApplicationControlUsage(0x0205, "Maximize"),
            new ApplicationControlUsage(0x0206, "Minimize"),
            new ApplicationControlUsage(0x0207, "Save"),
            new ApplicationControlUsage(0x0208, "Print"),
            new ApplicationControlUsage(0x0209, "Properties"),
            new ApplicationControlUsage(0x021A, "Undo"),
            new ApplicationControlUsage(0x021B, "Copy"),
            new ApplicationControlUsage(0x021C, "Cut"),
            new ApplicationControlUsage(0x021D, "Paste"),
            new ApplicationControlUsage(0x021E, "Select All"),
            new ApplicationControlUsage(0x021F, "Find"),
            new ApplicationControlUsage(0x0220, "Find and Replace"),
            new ApplicationControlUsage(0x0221, "Search"),
            new ApplicationControlUsage(0x0222, "Go To"),
            new ApplicationControlUsage(0x0223, "Home"),
            new ApplicationControlUsage(0x0224, "Back"),
            new ApplicationControlUsage(0x0225, "Forward"),
            new ApplicationControlUsage(0x0226, "Stop"),
            new ApplicationControlUsage(0x0227, "Refresh"),
            new ApplicationControlUsage(0x0228, "Previous Link"),
            new ApplicationControlUsage(0x0229, "Next Link"),
            new ApplicationControlUsage(0x022A, "Bookmarks"),
            new ApplicationControlUsage(0x022B, "History"),
            new ApplicationControlUsage(0x022C, "Subscriptions"),
            new ApplicationControlUsage(0x022D, "Zoom In"),
            new ApplicationControlUsage(0x022E, "Zoom Out"),
            new ApplicationControlUsage(0x022F, "Zoom"),
            new ApplicationControlUsage(0x0230, "Full Screen View"),
            new ApplicationControlUsage(0x0231, "Normal View"),
            new ApplicationControlUsage(0x0232, "View Toggle"),
            new ApplicationControlUsage(0x0233, "Scroll Up"),
            new ApplicationControlUsage(0x0234, "Scroll Down"),
            new ApplicationControlUsage(0x0235, "Scroll"),
            new ApplicationControlUsage(0x0236, "Pan Left"),
            new ApplicationControlUsage(0x0237, "Pan Right"),
            new ApplicationControlUsage(0x0238, "Pan"),
            new ApplicationControlUsage(0x0239, "New Window"),
            new ApplicationControlUsage(0x023A, "Tile Horizontally"),
            new ApplicationControlUsage(0x023B, "Tile Vertically"),
            new ApplicationControlUsage(0x023C, "Format"),
            new ApplicationControlUsage(0x023D, "Edit"),
            new ApplicationControlUsage(0x023E, "Bold"),
            new ApplicationControlUsage(0x023F, "Italics"),
            new ApplicationControlUsage(0x0240, "Underline"),
            new ApplicationControlUsage(0x0241, "Strikethrough"),
            new ApplicationControlUsage(0x0242, "Subscript"),
            new ApplicationControlUsage(0x0243, "Superscript"),
            new ApplicationControlUsage(0x0244, "All Caps"),
            new ApplicationControlUsage(0x0245, "Rotate"),
            new ApplicationControlUsage(0x0246, "Resize"),
            new ApplicationControlUsage(0x0247, "Flip horizontal"),
            new ApplicationControlUsage(0x0248, "Flip Vertical"),
            new ApplicationControlUsage(0x0249, "Mirror Horizontal"),
            new ApplicationControlUsage(0x024A, "Mirror Vertical"),
            new ApplicationControlUsage(0x024B, "Font Select"),
            new ApplicationControlUsage(0x024C, "Font Color"),
            new ApplicationControlUsage(0x024D, "Font Size"),
            new ApplicationControlUsage(0x024E, "Justify Left"),
            new ApplicationControlUsage(0x024F, "Justify Center H"),
            new ApplicationControlUsage(0x0250, "Justify Right"),
            new ApplicationControlUsage(0x0251, "Justify Block H"),
            new ApplicationControlUsage(0x0252, "Justify Top"),
            new ApplicationControlUsage(0x0253, "Justify Center V"),
            new ApplicationControlUsage(0x0254, "Justify Bottom"),
            new ApplicationControlUsage(0x0255, "Justify Block V"),
            new ApplicationControlUsage(0x0256, "Indent Decrease"),
            new ApplicationControlUsage(0x0257, "Indent Increase"),
            new ApplicationControlUsage(0x0258, "Numbered List"),
            new ApplicationControlUsage(0x0259, "Restart Numbering"),
            new ApplicationControlUsage(0x025A, "Bulleted List"),
            new ApplicationControlUsage(0x025B, "Promote"),
            new ApplicationControlUsage(0x025C, "Demote"),
            new ApplicationControlUsage(0x025D, "Yes"),
            new ApplicationControlUsage(0x025E, "No"),
            new ApplicationControlUsage(0x025F, "Cancel"),
            new ApplicationControlUsage(0x0260, "Catalog"),
            new ApplicationControlUsage(0x0261, "Buy/Checkout"),
            new ApplicationControlUsage(0x0262, "Add to Cart"),
            new ApplicationControlUsage(0x0263, "Expand"),
            new ApplicationControlUsage(0x0264, "Expand All"),
            new ApplicationControlUsage(0x0265, "Collapse"),
            new ApplicationControlUsage(0x0266, "Collapse All"),
            new ApplicationControlUsage(0x0267, "Print Preview"),
            new ApplicationControlUsage(0x0268, "Paste Special"),
            new ApplicationControlUsage(0x0269, "Insert Mode"),
            new ApplicationControlUsage(0x026A, "Delete"),
            new ApplicationControlUsage(0x026B, "Lock"),
            new ApplicationControlUsage(0x026C, "Unlock"),
            new ApplicationControlUsage(0x026D, "Protect"),
            new ApplicationControlUsage(0x026E, "Unprotect"),
            new ApplicationControlUsage(0x026F, "Attach Comment"),
            new ApplicationControlUsage(0x0270, "Delete Comment"),
            new ApplicationControlUsage(0x0271, "View Comment"),
            new ApplicationControlUsage(0x0272, "Select Word"),
            new ApplicationControlUsage(0x0273, "Select Sentence"),
            new ApplicationControlUsage(0x0274, "Select Paragraph"),
            new ApplicationControlUsage(0x0275, "Select Column"),
            new ApplicationControlUsage(0x0276, "Select Row"),
            new ApplicationControlUsage(0x0277, "Select Table"),
            new ApplicationControlUsage(0x0278, "Select Object"),
            new ApplicationControlUsage(0x0279, "Redo/Repeat"),
            new ApplicationControlUsage(0x027A, "Sort"),
            new ApplicationControlUsage(0x027B, "Sort Ascending"),
            new ApplicationControlUsage(0x027C, "Sort Descending"),
            new ApplicationControlUsage(0x027D, "Filter"),
            new ApplicationControlUsage(0x027E, "Set Clock"),
            new ApplicationControlUsage(0x027F, "View Clock"),
            new ApplicationControlUsage(0x0280, "Select Time Zone"),
            new ApplicationControlUsage(0x0281, "Edit Time Zones"),
            new ApplicationControlUsage(0x0282, "Set Alarm"),
            new ApplicationControlUsage(0x0283, "Clear Alarm"),
            new ApplicationControlUsage(0x0284, "Snooze Alarm"),
            new ApplicationControlUsage(0x0285, "Reset Alarm"),
            new ApplicationControlUsage(0x0286, "Synchronize"),
            new ApplicationControlUsage(0x0287, "Send/Receive"),
            new ApplicationControlUsage(0x0288, "Send To"),
            new ApplicationControlUsage(0x0289, "Reply"),
            new ApplicationControlUsage(0x028A, "Reply All"),
            new ApplicationControlUsage(0x028B, "Forward Msg"),
            new ApplicationControlUsage(0x028C, "Send"),
            new ApplicationControlUsage(0x028D, "Attach File"),
            new ApplicationControlUsage(0x028E, "Upload"),
            new ApplicationControlUsage(0x028F, "Download (Save Target As)"),
            new ApplicationControlUsage(0x0290, "Set Borders"),
            new ApplicationControlUsage(0x0291, "Insert Row"),
            new ApplicationControlUsage(0x0292, "Insert Column"),
            new ApplicationControlUsage(0x0293, "Insert File"),
            new ApplicationControlUsage(0x0294, "Insert Picture"),
            new ApplicationControlUsage(0x0295, "Insert Object"),
            new ApplicationControlUsage(0x0296, "Insert Symbol"),
            new ApplicationControlUsage(0x0297, "Save and Close"),
            new ApplicationControlUsage(0x0298, "Rename"),
            new ApplicationControlUsage(0x0299, "Merge"),
            new ApplicationControlUsage(0x029A, "Split"),
            new ApplicationControlUsage(0x029B, "Disribute Horizontally"),
            new ApplicationControlUsage(0x029C, "Distribute Vertically"),
    };
    public final short usage;
    private final String name;

    private ApplicationControlUsage(int u, String n) {
        usage = (short) u;
        name = n;
    }

    public static ApplicationControlUsage getUsageByPosition(int position) {
        if (position > 0 && position <= AC_USAGES.length) {
            return AC_USAGES[position - 1];
        }
        return null;
    }

    public static List<String> getUsageNames() {
        List<String> l = new ArrayList<>();

        for (ApplicationControlUsage AC_USAGE : AC_USAGES) {
            l.add(AC_USAGE.name);
        }

        return l;
    }
}
