package com.fishingmacro.pathfinding;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.KeySimulator;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.util.Clock;
import com.fishingmacro.util.MathUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class ReturnHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final boolean baritoneAvailable;
    static {
        boolean found = false;
        if (FabricLoader.getInstance().isModLoaded("baritone")) {
            try {
                Class.forName("baritone.api.BaritoneAPI");
                found = true;
                System.out.println("[FishingMacro] Baritone detected, A* pathfinding available");
            } catch (ClassNotFoundException e) {
                System.out.println("[FishingMacro] Baritone mod loaded but API not found, using manual walk-back");
            }
        } else {
            System.out.println("[FishingMacro] Baritone not installed, using manual walk-back");
        }
        baritoneAvailable = found;
    }

    public static boolean isBaritoneAvailable() {
        return baritoneAvailable;
    }

    private enum ReturnPhase {
        AIMING, WALKING, STUCK_JUMP, STUCK_STRAFE, ARRIVED, TIMED_OUT
    }

    private Vec3d savedPos;
    private float savedYaw;
    private float savedPitch;
    private boolean returning = false;

    // Phase-based movement (manual fallback)
    private ReturnPhase phase = ReturnPhase.AIMING;
    private Vec3d lastTickPos;
    private int stuckTicks;
    private int stuckAttempts;
    private boolean strafeLeft;
    private final Clock strafeDuration = new Clock();
    private long returnStartTime;
    private boolean sprinting;
    private final Clock sprintDecisionClock = new Clock();
    private final Clock reaimCooldown = new Clock();
    private boolean timedOut;
    private int jumpRecoveryTicks;

    // Baritone integration
    private BaritoneReturnStrategy baritoneStrategy;
    private boolean usingBaritone = false;

    public void savePosition() {
        if (mc.player == null) return;
        savedPos = mc.player.getEntityPos();
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
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
        timedOut = false;
        returnStartTime = System.currentTimeMillis();

        // Try Baritone if available and enabled
        if (baritoneAvailable && MacroConfig.useBaritone) {
            try {
                if (baritoneStrategy == null) {
                    baritoneStrategy = new BaritoneReturnStrategy();
                }
                baritoneStrategy.startPathfinding(savedPos.x, savedPos.y, savedPos.z);
                usingBaritone = true;
                return;
            } catch (Exception e) {
                System.err.println("[FishingMacro] Baritone failed, falling back to manual: " + e.getMessage());
                usingBaritone = false;
            }
        }

        // Manual walk-back fallback
        startManualReturn();
    }

    private void startManualReturn() {
        usingBaritone = false;
        phase = ReturnPhase.AIMING;
        stuckTicks = 0;
        stuckAttempts = 0;
        strafeLeft = Math.random() < 0.5;
        sprinting = false;
        jumpRecoveryTicks = 0;
        lastTickPos = mc.player != null ? mc.player.getEntityPos() : null;

        // Initial aim toward target
        if (mc.player != null) {
            float[] rot = RotationHandler.getInstance().getRotationTo(savedPos);
            float yawOffset = MathUtil.randomFloat(-1.5f, 1.5f);
            RotationHandler.getInstance().easeSmoothTo(rot[0] + yawOffset, rot[1],
                    (long) MathUtil.randomFloat(600, 1000));
        }
    }

    public boolean isReturning() {
        return returning;
    }

    public boolean hasArrived() {
        if (mc.player == null || savedPos == null) return false;
        double distSq = mc.player.getEntityPos().squaredDistanceTo(savedPos);
        return distSq <= 1.0;
    }

    public boolean hasTimedOut() {
        return timedOut;
    }

    public void onTick() {
        if (!returning || mc.player == null) return;

        // Global timeout
        if (System.currentTimeMillis() - returnStartTime > MacroConfig.returnTimeoutMs) {
            if (usingBaritone && baritoneStrategy != null) {
                baritoneStrategy.cancel();
            }
            releaseMovementKeys();
            timedOut = true;
            returning = false;
            usingBaritone = false;
            return;
        }

        if (hasArrived()) {
            if (usingBaritone && baritoneStrategy != null) {
                baritoneStrategy.cancel();
            }
            releaseMovementKeys();
            returning = false;
            usingBaritone = false;
            return;
        }

        if (usingBaritone) {
            tickBaritone();
        } else {
            tickManual();
        }
    }

    private void tickBaritone() {
        // Baritone handles all movement; just check if it finished or failed
        if (baritoneStrategy != null && baritoneStrategy.hasFinished()) {
            if (hasArrived()) {
                returning = false;
                usingBaritone = false;
            } else {
                // Baritone finished but we haven't arrived - fall back to manual
                System.out.println("[FishingMacro] Baritone path ended without arriving, falling back to manual");
                startManualReturn();
            }
        }
    }

    private void tickManual() {
        switch (phase) {
            case AIMING -> tickAiming();
            case WALKING -> tickWalking();
            case STUCK_JUMP -> tickStuckJump();
            case STUCK_STRAFE -> tickStuckStrafe();
            case ARRIVED -> {
                releaseMovementKeys();
                returning = false;
            }
            case TIMED_OUT -> {
                releaseMovementKeys();
                timedOut = true;
                returning = false;
            }
        }
    }

    private void tickAiming() {
        // Wait for initial rotation to finish, then start walking
        if (!RotationHandler.getInstance().isRotating()) {
            phase = ReturnPhase.WALKING;
            lastTickPos = mc.player.getEntityPos();
            stuckTicks = 0;
            reaimCooldown.schedule(MathUtil.randomBetween(800, 1500));
            sprintDecisionClock.schedule(0); // evaluate sprint immediately
        }
    }

    private void tickWalking() {
        // Hold forward
        KeySimulator.pressKey(mc.options.forwardKey);

        // Sprint decision based on distance
        if (!sprintDecisionClock.isScheduled() || sprintDecisionClock.passed()) {
            double dist = mc.player.getEntityPos().distanceTo(savedPos);
            if (dist > 4.0) {
                if (!sprinting) {
                    KeySimulator.pressKey(mc.options.sprintKey);
                    sprinting = true;
                }
            } else if (dist < 2.0) {
                if (sprinting) {
                    KeySimulator.releaseKey(mc.options.sprintKey);
                    sprinting = false;
                }
            }
            sprintDecisionClock.schedule(MathUtil.randomBetween(1500, 3500));
        }

        // Re-aim with cooldown
        if (!RotationHandler.getInstance().isRotating()
                && (!reaimCooldown.isScheduled() || reaimCooldown.passed())) {
            float[] rot = RotationHandler.getInstance().getRotationTo(savedPos);
            float yawOffset = MathUtil.randomFloat(-1.5f, 1.5f);
            RotationHandler.getInstance().easeSmoothTo(rot[0] + yawOffset, rot[1],
                    (long) MathUtil.randomFloat(700, 1200));
            reaimCooldown.schedule(MathUtil.randomBetween(800, 1500));
        }

        // Stuck detection
        if (lastTickPos != null) {
            double movedSq = mc.player.getEntityPos().squaredDistanceTo(lastTickPos);
            // 0.03 blocks/tick threshold: 0.03^2 = 0.0009
            if (movedSq < 0.0009) {
                stuckTicks++;
            } else {
                stuckTicks = 0;
            }
        }
        lastTickPos = mc.player.getEntityPos();

        if (stuckTicks >= MacroConfig.returnStuckThresholdTicks) {
            stuckTicks = 0;
            stuckAttempts++;
            if (stuckAttempts > MacroConfig.returnMaxStuckAttempts) {
                phase = ReturnPhase.TIMED_OUT;
            } else if (stuckAttempts <= 2) {
                // First attempts: jump
                phase = ReturnPhase.STUCK_JUMP;
                jumpRecoveryTicks = 0;
            } else {
                // Escalate to strafe
                phase = ReturnPhase.STUCK_STRAFE;
                KeySimulator.releaseKey(mc.options.forwardKey);
                strafeDuration.schedule(MathUtil.randomBetween(400, 800));
                strafeLeft = !strafeLeft;
            }
        }
    }

    private void tickStuckJump() {
        // Hold forward + jump for 10 ticks
        KeySimulator.pressKey(mc.options.forwardKey);
        KeySimulator.pressKey(mc.options.jumpKey);
        jumpRecoveryTicks++;

        if (jumpRecoveryTicks >= 10) {
            KeySimulator.releaseKey(mc.options.jumpKey);
            // Check if we moved
            if (lastTickPos != null) {
                double movedSq = mc.player.getEntityPos().squaredDistanceTo(lastTickPos);
                if (movedSq > 0.01) {
                    stuckTicks = 0;
                }
            }
            lastTickPos = mc.player.getEntityPos();
            phase = ReturnPhase.WALKING;
            // Re-aim after unstuck attempt
            float[] rot = RotationHandler.getInstance().getRotationTo(savedPos);
            RotationHandler.getInstance().easeSmoothTo(rot[0], rot[1],
                    (long) MathUtil.randomFloat(600, 900));
            reaimCooldown.schedule(MathUtil.randomBetween(800, 1500));
        }
    }

    private void tickStuckStrafe() {
        // Strafe left/right + jump
        if (strafeLeft) {
            KeySimulator.pressKey(mc.options.leftKey);
        } else {
            KeySimulator.pressKey(mc.options.rightKey);
        }
        KeySimulator.pressKey(mc.options.jumpKey);

        if (strafeDuration.passed()) {
            KeySimulator.releaseKey(mc.options.leftKey);
            KeySimulator.releaseKey(mc.options.rightKey);
            KeySimulator.releaseKey(mc.options.jumpKey);
            lastTickPos = mc.player.getEntityPos();
            stuckTicks = 0;
            phase = ReturnPhase.WALKING;
            // Re-aim after strafe
            float[] rot = RotationHandler.getInstance().getRotationTo(savedPos);
            RotationHandler.getInstance().easeSmoothTo(rot[0], rot[1],
                    (long) MathUtil.randomFloat(600, 900));
            reaimCooldown.schedule(MathUtil.randomBetween(800, 1500));
        }
    }

    private void releaseMovementKeys() {
        KeySimulator.releaseKey(mc.options.forwardKey);
        KeySimulator.releaseKey(mc.options.sprintKey);
        KeySimulator.releaseKey(mc.options.jumpKey);
        KeySimulator.releaseKey(mc.options.leftKey);
        KeySimulator.releaseKey(mc.options.rightKey);
    }

    public void restoreRotation() {
        RotationHandler.getInstance().easeSmoothTo(savedYaw, savedPitch,
                (long) MathUtil.randomFloat(400, 700));
    }

    public void stopReturn() {
        if (usingBaritone && baritoneStrategy != null) {
            baritoneStrategy.cancel();
        }
        returning = false;
        usingBaritone = false;
        releaseMovementKeys();
    }

    public void reset() {
        stopReturn();
        savedPos = null;
        timedOut = false;
        phase = ReturnPhase.AIMING;
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

    public double getDistanceRemaining() {
        if (mc.player == null || savedPos == null) return 0;
        return mc.player.getEntityPos().distanceTo(savedPos);
    }
}
