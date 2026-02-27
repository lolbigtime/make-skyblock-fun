package com.fishingmacro.feature;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.util.Clock;
import com.fishingmacro.util.MathUtil;
import net.minecraft.client.MinecraftClient;

public class AntiAfk {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Clock timer = new Clock();
    private float cumulativeYawDrift = 0;
    private float cumulativePitchDrift = 0;

    public void start() {
        scheduleNext();
        cumulativeYawDrift = 0;
        cumulativePitchDrift = 0;
    }

    public void onTick() {
        if (mc.player == null) return;
        if (!timer.isScheduled() || !timer.passed()) return;
        if (RotationHandler.getInstance().isRotating()) return;

        // 15% chance to skip entirely (anti-pattern)
        if (Math.random() < 0.15) {
            scheduleNext();
            return;
        }

        // Calculate mouse sensitivity scaling
        // Vanilla formula: ((sens * 0.6 + 0.2)^3 * 8.0) * 0.15 degrees per pixel
        double sens = mc.options.getMouseSensitivity().getValue();
        double degreesPerPixel = Math.pow(sens * 0.6 + 0.2, 3) * 8.0 * 0.15;

        // Scale wiggle to correspond to natural mouse movement (5-25 pixels)
        float pixelRange = MathUtil.randomFloat(5.0f, 25.0f);
        float maxYawDeg = (float) (pixelRange * degreesPerPixel);
        float maxPitchDeg = (float) (pixelRange * degreesPerPixel * 0.5);

        // Clamp to configured limits
        maxYawDeg = Math.min(maxYawDeg, MacroConfig.antiAfkMaxYawDrift);
        maxPitchDeg = Math.min(maxPitchDeg, MacroConfig.antiAfkMaxPitchDrift);

        float yawDelta = MathUtil.randomFloat(-maxYawDeg, maxYawDeg);
        float pitchDelta = MathUtil.randomFloat(-maxPitchDeg, maxPitchDeg);

        // Bias toward center if we've drifted too far
        if (Math.abs(cumulativeYawDrift + yawDelta) > MacroConfig.antiAfkMaxYawDrift * 3) {
            yawDelta = -cumulativeYawDrift * MathUtil.randomFloat(0.3f, 0.6f);
        }
        if (Math.abs(cumulativePitchDrift + pitchDelta) > MacroConfig.antiAfkMaxPitchDrift * 3) {
            pitchDelta = -cumulativePitchDrift * MathUtil.randomFloat(0.3f, 0.6f);
        }

        // Randomize the magnitude slightly
        yawDelta *= MathUtil.randomFloat(0.5f, 1.5f);
        pitchDelta *= MathUtil.randomFloat(0.5f, 1.5f);

        cumulativeYawDrift += yawDelta;
        cumulativePitchDrift += pitchDelta;

        float targetYaw = mc.player.getYaw() + yawDelta;
        float targetPitch = mc.player.getPitch() + pitchDelta;

        // Use smooth easing (400-800ms) for gentle, natural movement
        long duration = (long) MathUtil.randomFloat(400, 800);
        RotationHandler.getInstance().easeSmoothTo(targetYaw, targetPitch, duration);

        scheduleNext();
    }

    public void reset() {
        timer.reset();
        cumulativeYawDrift = 0;
        cumulativePitchDrift = 0;
    }

    private void scheduleNext() {
        long[] range = MacroConfig.humanize(MacroConfig.antiAfkIntervalMs);
        timer.schedule(MathUtil.randomBetween(range[0], range[1]));
    }
}
