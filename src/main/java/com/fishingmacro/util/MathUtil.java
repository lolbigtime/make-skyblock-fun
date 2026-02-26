package com.fishingmacro.util;

import java.util.concurrent.ThreadLocalRandom;

public class MathUtil {

    public static long randomBetween(long min, long max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    public static int randomBetween(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static float randomFloat(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    public static double randomDouble(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    public static float wrapAngleDeg(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    public static float easeOutExpo(float x) {
        return x >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2, -10 * x);
    }

    public static float easeOutBack(float x, float modifier) {
        float c1 = 1.70158f + modifier;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    public static float easeInOutCubic(float x) {
        return x < 0.5f
                ? 4.0f * x * x * x
                : 1.0f - (float) Math.pow(-2.0 * x + 2.0, 3) / 2.0f;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
