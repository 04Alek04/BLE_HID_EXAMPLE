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

import android.graphics.Point;

public class TouchInputCalculator {

    private float remainderX = 0f;
    private float remainderY = 0f;

    public void resetRemainder() {
        remainderX = 0f;
        remainderY = 0f;
    }

    /**
     * Calculates 16-bit packed relative mouse movement deltas (dx, dy) with remainder accumulation.
     */
    public int calculateRelativeDelta(float dx, float dy, float sensitivity) {
        float moveX = dx * sensitivity + remainderX;
        float moveY = dy * sensitivity + remainderY;

        int intX = Math.round(moveX);
        int intY = Math.round(moveY);

        if (intX != 0 || intY != 0) {
            remainderX = moveX - intX;
            remainderY = moveY - intY;

            byte clampedX = (byte) Math.max(-128, Math.min(127, intX));
            byte clampedY = (byte) Math.max(-128, Math.min(127, intY));
            return (clampedX & 0xFF) | ((clampedY & 0xFF) << 8);
        } else {
            remainderX = moveX;
            remainderY = moveY;
            return 0;
        }
    }

    /**
     * Calculates 1D scroll steps from delta displacement.
     */
    public int calculateScrollStep(float delta, float sensitivity) {
        float move = delta * sensitivity + remainderX;
        int step = Math.round(move);
        if (step != 0) {
            remainderX = move - step;
            return step;
        } else {
            remainderX = move;
            return 0;
        }
    }

    /**
     * Calculates 0..32767 scaled absolute mouse coordinates from touch surface touch point.
     */
    public static Point calculateAbsoluteCoordinates(float touchX, float touchY, float viewWidth, float viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return new Point(0, 0);
        }

        int absX = Math.round((touchY / viewHeight) * HidMouseCodes.ABS_MOUSE_MAX_COORD);
        int absY = Math.round((1f - (touchX / viewWidth)) * HidMouseCodes.ABS_MOUSE_MAX_COORD);

        absX = Math.max(0, Math.min(HidMouseCodes.ABS_MOUSE_MAX_COORD, absX));
        absY = Math.max(0, Math.min(HidMouseCodes.ABS_MOUSE_MAX_COORD, absY));

        return new Point(absX, absY);
    }

    /**
     * Calculates -127..127 8-bit signed joystick axis displacement from touch delta and maximum radius.
     */
    public static Point calculateJoystickAxis(float dx, float dy, float maxRadius) {
        if (maxRadius <= 0f) {
            return new Point(0, 0);
        }

        int axisX = Math.round(Math.max(HidGamepadCodes.STICK_AXIS_MIN,
                Math.min(HidGamepadCodes.STICK_AXIS_MAX, (dx / maxRadius) * HidGamepadCodes.STICK_AXIS_MAX)));
        int axisY = Math.round(Math.max(HidGamepadCodes.STICK_AXIS_MIN,
                Math.min(HidGamepadCodes.STICK_AXIS_MAX, (dy / maxRadius) * HidGamepadCodes.STICK_AXIS_MAX)));

        return new Point(axisX, axisY);
    }
}
