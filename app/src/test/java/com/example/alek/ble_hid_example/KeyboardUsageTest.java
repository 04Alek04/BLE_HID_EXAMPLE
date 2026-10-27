package com.example.alek.ble_hid_example;

import android.view.KeyEvent;
import org.junit.Test;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KeyboardUsageTest {

    @Test
    public void testFindByCharacter_letters() {
        KeyboardUsage usageA = KeyboardUsage.findByCharacter('a');
        assertNotNull(usageA);
        assertEquals((byte) 0x04, usageA.usage); // Usage 0x04 for 'a'

        KeyboardUsage usageZ = KeyboardUsage.findByCharacter('z');
        assertNotNull(usageZ);
        assertEquals((byte) 0x1d, usageZ.usage); // Usage 0x1d for 'z'
    }

    @Test
    public void testFindByCharacter_polishLettersMapToAltGr() {
        char[] polishChars = {'ą', 'ć', 'ę', 'ł', 'ń', 'ó', 'ś', 'ź', 'ż'};

        for (char c : polishChars) {
            KeyboardUsage usage = KeyboardUsage.findByCharacter(c);
            assertNotNull("Usage should exist for Polish char: " + c, usage);
            assertEquals("Polish char should use META_RIGHT_ALT (0x40)",
                    KeyboardUsage.META_RIGHT_ALT, usage.meta);
        }
    }

    @Test
    public void testFindByCharacter_symbolsAndDigits() {
        KeyboardUsage usageOne = KeyboardUsage.findByCharacter('1');
        assertNotNull(usageOne);
        assertEquals((byte) 0x1e, usageOne.usage);

        KeyboardUsage usageAt = KeyboardUsage.findByCharacter('@');
        assertNotNull(usageAt);
        assertEquals((byte) 0x1f, usageAt.usage);
        assertEquals(KeyboardUsage.META_LEFT_SHIFT, usageAt.meta);
    }

    @Test
    public void testFindByCharacter_nonExistentCharReturnsNull() {
        KeyboardUsage usageNull = KeyboardUsage.findByCharacter('\uFFFF');
        assertNull(usageNull);
    }

    @Test
    public void testFindByKeyCode_matchesAndroidKeyEvent() {
        KeyboardUsage usageEnter = KeyboardUsage.findByKeyCode(KeyEvent.KEYCODE_ENTER);
        assertNotNull(usageEnter);
        assertEquals((byte) 0x28, usageEnter.usage);

        KeyboardUsage usageSpace = KeyboardUsage.findByKeyCode(KeyEvent.KEYCODE_SPACE);
        assertNotNull(usageSpace);
        assertEquals((byte) 0x2c, usageSpace.usage);

        KeyboardUsage usageInvalid = KeyboardUsage.findByKeyCode(-9999);
        assertNull(usageInvalid);
    }

    @Test
    public void testFindByDescription_matchesStringName() {
        KeyboardUsage usageEnter = KeyboardUsage.findByDescription("Enter");
        assertNotNull(usageEnter);
        assertEquals((byte) 0x28, usageEnter.usage);

        KeyboardUsage usageF1 = KeyboardUsage.findByDescription("F1");
        assertNotNull(usageF1);
        assertEquals((byte) 0x3a, usageF1.usage);

        KeyboardUsage usageNull = KeyboardUsage.findByDescription(null);
        assertNull(usageNull);
    }

    @Test
    public void testGetUsageNames_returnsNonKeyboardDescriptions() {
        List<String> names = KeyboardUsage.getUsageNames();
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertTrue(names.contains("Enter"));
        assertTrue(names.contains("F1"));
    }
}
