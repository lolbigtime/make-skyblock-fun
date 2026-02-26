package com.fishingmacro.pathfinding;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Handles saving the fishing position and pathfinding back if knocked off.
 * Baritone integration is stubbed — requires Baritone as a dependency at runtime.
 * Falls back to simple walk-toward logic if Baritone is not present.
 */
public class ReturnHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private BlockPos savedPos;
    private float savedYaw;
    private float savedPitch;
    private boolean returning = false;
    private boolean baritoneAvailable = false;
    private Object baritoneProcess = null;

    public void savePosition() {
        if (mc.player == null) return;
        savedPos = mc.player.getBlockPos();
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();

        // Try to detect Baritone
        try {
            Class.forName("baritone.api.BaritoneAPI");
            baritoneAvailable = true;
        } catch (ClassNotFoundException e) {
            baritoneAvailable = false;
            System.out.println("[FishingMacro] Baritone not found, using simple walk-back fallback");
        }
    }

    public boolean isKnockedOff() {
        if (mc.player == null || savedPos == null) return false;
        double distSq = mc.player.getBlockPos().getSquaredDistance(savedPos);
        double threshold = MacroConfig.knockbackThreshold;
        return distSq > threshold * threshold;
    }

    public void startReturn() {
        if (savedPos == null) return;
        returning = true;

        if (baritoneAvailable) {
            startBaritonePathfinding();
        } else {
            startSimpleWalkBack();
        }
    }

    public boolean isReturning() {
        return returning;
    }

    public boolean hasArrived() {
        if (mc.player == null || savedPos == null) return false;
        double distSq = mc.player.getBlockPos().getSquaredDistance(savedPos);
        return distSq <= 2.0;
    }

    public void onTick() {
        if (!returning || mc.player == null) return;

        if (hasArrived()) {
            stopReturn();
            return;
        }

        if (!baritoneAvailable) {
            tickSimpleWalkBack();
        }
        // Baritone handles its own ticking when available
    }

    public void restoreRotation() {
        RotationHandler.getInstance().easeTo(savedYaw, savedPitch,
                (long) MathUtil.randomFloat(300, 600));
    }

    public void stopReturn() {
        returning = false;
        if (baritoneAvailable) {
            stopBaritone();
        }
    }

    public void reset() {
        stopReturn();
        savedPos = null;
    }

    // --- Baritone integration (reflection-based to avoid hard dependency) ---

    private void startBaritonePathfinding() {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            baritoneProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);

            Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Object goal = goalBlockClass.getConstructor(int.class, int.class, int.class)
                    .newInstance(savedPos.getX(), savedPos.getY(), savedPos.getZ());

            baritoneProcess.getClass().getMethod("setGoalAndPath",
                    Class.forName("baritone.api.pathing.goals.Goal")).invoke(baritoneProcess, goal);
        } catch (Exception e) {
            System.err.println("[FishingMacro] Failed to start Baritone pathfinding: " + e.getMessage());
            baritoneAvailable = false;
            startSimpleWalkBack();
        }
    }

    private void stopBaritone() {
        if (baritoneProcess == null) return;
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        baritoneProcess = null;
    }

    // --- Simple walk-back fallback (no Baritone) ---

    private void startSimpleWalkBack() {
        // Point toward saved position
        if (mc.player == null || savedPos == null) return;
        float[] rot = RotationHandler.getInstance().getRotationTo(
                savedPos.toCenterPos()
        );
        RotationHandler.getInstance().easeTo(rot[0], rot[1],
                (long) MathUtil.randomFloat(300, 500));
    }

    private void tickSimpleWalkBack() {
        if (mc.player == null || savedPos == null) return;

        // Re-aim toward target periodically
        if (!RotationHandler.getInstance().isRotating()) {
            float[] rot = RotationHandler.getInstance().getRotationTo(
                    savedPos.toCenterPos()
            );
            RotationHandler.getInstance().easeTo(rot[0], rot[1],
                    (long) MathUtil.randomFloat(150, 300));
        }

        // Hold W key to walk forward
        com.fishingmacro.handler.KeySimulator.pressKey(mc.options.forwardKey);

        // Random sprint toggling
        if (Math.random() < 0.02) {
            if (((com.fishingmacro.mixin.KeyBindingAccessor) mc.options.sprintKey).getPressed()) {
                com.fishingmacro.handler.KeySimulator.releaseKey(mc.options.sprintKey);
            } else {
                com.fishingmacro.handler.KeySimulator.pressKey(mc.options.sprintKey);
            }
        }
    }
}
