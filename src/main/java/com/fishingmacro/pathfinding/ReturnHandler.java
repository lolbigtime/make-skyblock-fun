package com.fishingmacro.pathfinding;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.util.Clock;
import com.fishingmacro.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles saving the fishing position and pathfinding back if knocked off.
 * Baritone integration is stubbed — requires Baritone as a dependency at runtime.
 * Falls back to simple walk-toward logic if Baritone is not present.
 */
public class ReturnHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Vec3d savedPos;
    private BlockPos savedBlockPos;
    private float savedYaw;
    private float savedPitch;
    private boolean returning = false;
    private boolean baritoneAvailable = false;
    private Object baritoneProcess = null;
    private final Clock rotationCooldown = new Clock();

    public void savePosition() {
        if (mc.player == null) return;
        savedPos = mc.player.getEntityPos();
        savedBlockPos = mc.player.getBlockPos();
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
        double distSq = mc.player.getEntityPos().squaredDistanceTo(savedPos);
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
        double distSq = mc.player.getEntityPos().squaredDistanceTo(savedPos);
        return distSq <= 1.0; // Within ~1 block of exact saved position
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
        RotationHandler.getInstance().easeSmoothTo(savedYaw, savedPitch,
                (long) MathUtil.randomFloat(400, 700));
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
        savedBlockPos = null;
    }

    // --- Getters for render overlay ---

    public Vec3d getSavedPos() {
        return savedPos;
    }

    public float getSavedYaw() {
        return savedYaw;
    }

    public float getSavedPitch() {
        return savedPitch;
    }

    public boolean isBaritoneAvailable() {
        return baritoneAvailable;
    }

    /**
     * Returns the current Baritone path nodes for rendering.
     * Uses reflection to read Baritone's active path positions.
     * Returns empty list if Baritone is unavailable or no active path.
     */
    public List<Vec3d> getCurrentPathNodes() {
        if (!baritoneAvailable || !returning) return Collections.emptyList();

        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);

            // Try to get the current path
            Object pathOptional = pathingBehavior.getClass().getMethod("getPath").invoke(pathingBehavior);
            if (pathOptional == null) return Collections.emptyList();

            // java.util.Optional
            java.util.Optional<?> opt = (java.util.Optional<?>) pathOptional;
            if (opt.isEmpty()) return Collections.emptyList();

            Object path = opt.get();
            // IPath.positions() returns List<BetterBlockPos>
            @SuppressWarnings("unchecked")
            List<?> positions = (List<?>) path.getClass().getMethod("positions").invoke(path);

            List<Vec3d> nodes = new ArrayList<>();
            for (Object pos : positions) {
                // BetterBlockPos extends BlockPos
                BlockPos bp = (BlockPos) pos;
                nodes.add(new Vec3d(bp.getX() + 0.5, bp.getY() + 0.1, bp.getZ() + 0.5));
            }
            return nodes;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the distance remaining to the saved position.
     */
    public double getDistanceRemaining() {
        if (mc.player == null || savedPos == null) return 0;
        return mc.player.getEntityPos().distanceTo(savedPos);
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
                    .newInstance(savedBlockPos.getX(), savedBlockPos.getY(), savedBlockPos.getZ());

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
        float[] rot = RotationHandler.getInstance().getRotationTo(savedPos);
        RotationHandler.getInstance().easeSmoothTo(rot[0], rot[1],
                (long) MathUtil.randomFloat(500, 800));
        rotationCooldown.schedule(MathUtil.randomBetween(300, 500));
    }

    private void tickSimpleWalkBack() {
        if (mc.player == null || savedPos == null) return;

        // Re-aim toward target with cooldown between recalculations
        if (!RotationHandler.getInstance().isRotating()
                && (!rotationCooldown.isScheduled() || rotationCooldown.passed())) {
            float[] rot = RotationHandler.getInstance().getRotationTo(savedPos);
            RotationHandler.getInstance().easeSmoothTo(rot[0], rot[1],
                    (long) MathUtil.randomFloat(400, 700));
            rotationCooldown.schedule(MathUtil.randomBetween(300, 500));
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
