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

        // Calculate drift with bias back toward zero
        float maxYaw = MacroConfig.antiAfkMaxYawDrift;
        float maxPitch = MacroConfig.antiAfkMaxPitchDrift;

        float yawDelta = MathUtil.randomFloat(-maxYaw, maxYaw);
        float pitchDelta = MathUtil.randomFloat(-maxPitch, maxPitch);

        // Bias toward center if we've drifted too far
        if (Math.abs(cumulativeYawDrift + yawDelta) > maxYaw * 3) {
            yawDelta = -cumulativeYawDrift * MathUtil.randomFloat(0.3f, 0.6f);
        }
        if (Math.abs(cumulativePitchDrift + pitchDelta) > maxPitch * 3) {
            pitchDelta = -cumulativePitchDrift * MathUtil.randomFloat(0.3f, 0.6f);
        }

        // Randomize the magnitude slightly
        yawDelta *= MathUtil.randomFloat(0.5f, 1.5f);
        pitchDelta *= MathUtil.randomFloat(0.5f, 1.5f);

        cumulativeYawDrift += yawDelta;
        cumulativePitchDrift += pitchDelta;

        float targetYaw = mc.player.getYaw() + yawDelta;
        float targetPitch = mc.player.getPitch() + pitchDelta;

        long duration = (long) MathUtil.randomFloat(200, 500);
        RotationHandler.getInstance().easeTo(targetYaw, targetPitch, duration);

        scheduleNext();
    }

    public void reset() {
        timer.reset();
        cumulativeYawDrift = 0;
        cumulativePitchDrift = 0;
    }

    private void scheduleNext() {
        long interval = MathUtil.randomBetween(
                (long) MacroConfig.antiAfkMinIntervalMs,
                (long) MacroConfig.antiAfkMaxIntervalMs
        );
        timer.schedule(interval);
    }
}
