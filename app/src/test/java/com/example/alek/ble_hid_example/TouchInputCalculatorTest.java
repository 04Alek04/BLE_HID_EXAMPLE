package com.example.alek.ble_hid_example;

import android.graphics.Point;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class TouchInputCalculatorTest {

    private TouchInputCalculator calculator;

    @Before
    public void setUp() {
        calculator = new TouchInputCalculator();
    }

    @Test
    public void testResetRemainder_clearsAccumulatedSubPixelDeltas() {
        // Build up sub-pixel remainder 0.4f
        calculator.calculateRelativeDelta(0.4f, 0.4f, 1.0f);

        // Reset remainder
        calculator.resetRemainder();

        // Calculate 0.4f again -> should still return 0 because remainder was reset to 0
        int result = calculator.calculateRelativeDelta(0.4f, 0.4f, 1.0f);
        assertEquals(0, result);
    }

    @Test
    public void testCalculateRelativeDelta_accumulatesAndClamps() {
        // dx = 10, dy = -5, sensitivity = 1.0f
        int result = calculator.calculateRelativeDelta(10f, -5f, 1.0f);

        byte x = (byte) (result & 0xFF);
        byte y = (byte) ((result >> 8) & 0xFF);

        assertEquals(10, x);
        assertEquals(-5, y);
    }

    @Test
    public void testCalculateRelativeDelta_handlesSubPixelRemainder() {
        // dx = 0.4f, sensitivity = 1.0f -> rounded intX = 0, remainderX = 0.4f
        int result1 = calculator.calculateRelativeDelta(0.4f, 0f, 1.0f);
        assertEquals(0, result1);

        // Next delta dx = 0.8f + 0.4f = 1.2f -> intX = 1, remainderX = 0.2f
        int result2 = calculator.calculateRelativeDelta(0.8f, 0f, 1.0f);
        byte x2 = (byte) (result2 & 0xFF);
        assertEquals(1, x2);
    }

    @Test
    public void testCalculateScrollStep_accumulatesRemainder() {
        int step1 = calculator.calculateScrollStep(0.3f, 1.0f);
        assertEquals(0, step1);

        int step2 = calculator.calculateScrollStep(0.8f, 1.0f);
        assertEquals(1, step2);
    }

    @Test
    public void testCalculateAbsoluteCoordinates_scalesTo32767() {
        // Test center point (500, 500) on 1000x1000 surface
        Point center = TouchInputCalculator.calculateAbsoluteCoordinates(500f, 500f, 1000f, 1000f);

        // Expect near 16384 (center of 0..32767 range)
        assertEquals(16384, center.x);
        assertEquals(16384, center.y);
    }

    @Test
    public void testCalculateJoystickAxis_clampsTo127() {
        Point axisMax = TouchInputCalculator.calculateJoystickAxis(200f, -200f, 100f);
        assertEquals(127, axisMax.x);
        assertEquals(-127, axisMax.y);

        Point axisZero = TouchInputCalculator.calculateJoystickAxis(0f, 0f, 100f);
        assertEquals(0, axisZero.x);
        assertEquals(0, axisZero.y);
    }
}
