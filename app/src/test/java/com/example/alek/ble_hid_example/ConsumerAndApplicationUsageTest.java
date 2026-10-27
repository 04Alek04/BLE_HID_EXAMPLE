package com.example.alek.ble_hid_example;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConsumerAndApplicationUsageTest {

    @Test
    public void testApplicationControlUsage_getUsageByPositionAndNames() {
        // Test valid position
        ApplicationControlUsage usage = ApplicationControlUsage.getUsageByPosition(1);
        assertNotNull(usage);
        assertEquals((short) 0x0201, usage.usage);

        // Test out of bounds position
        assertNull(ApplicationControlUsage.getUsageByPosition(0));
        assertNull(ApplicationControlUsage.getUsageByPosition(999));

        // Test getUsageNames
        List<String> names = ApplicationControlUsage.getUsageNames();
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertTrue(names.contains("New"));
        assertTrue(names.contains("Save"));
    }

    @Test
    public void testApplicationLaunchButtonsUsage_getUsageByPositionAndNames() {
        // Test valid position
        ApplicationLaunchButtonsUsage usage = ApplicationLaunchButtonsUsage.getUsageByPosition(1);
        assertNotNull(usage);
        assertEquals((short) 0x0181, usage.usage);

        // Test out of bounds position
        assertNull(ApplicationLaunchButtonsUsage.getUsageByPosition(0));
        assertNull(ApplicationLaunchButtonsUsage.getUsageByPosition(999));

        // Test getUsageNames
        List<String> names = ApplicationLaunchButtonsUsage.getUsageNames();
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertTrue(names.contains("Calculator"));
    }

    @Test
    public void testConsumerControlUsage_bindAndGetUsage() {
        // Bind button ID 101 to VOLUME_UP
        ConsumerControlUsage.bindButtonId(101, ConsumerControlUsage.VOLUME_UP);

        short usage = ConsumerControlUsage.getUsage(101);
        assertEquals((short) 0x0080, usage);

        // Unbound button ID should return 0
        assertEquals((short) 0, ConsumerControlUsage.getUsage(-999));
    }
}
