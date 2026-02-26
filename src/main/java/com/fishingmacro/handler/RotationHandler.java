package com.fishingmacro.handler;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static RotationHandler instance;

    private boolean rotating = false;
    private float startYaw, startPitch;
    private float targetYaw, targetPitch;
    private long startTime, endTime;
    private float easingModifier;
    private boolean useOvershoot;
    private float overshootYaw, overshootPitch;
    private boolean overshootPhase;

    public static RotationHandler getInstance() {
        if (instance == null) instance = new RotationHandler();
        return instance;
    }

    public void easeTo(float targetYaw, float targetPitch, long durationMs) {
        if (mc.player == null) return;

        this.startYaw = mc.player.getYaw();
        this.startPitch = mc.player.getPitch();
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;

        // Wrap yaw difference to shortest path
        float yawDiff = MathUtil.wrapAngleDeg(targetYaw - startYaw);
        this.targetYaw = startYaw + yawDiff;

        // Randomize duration ±20%
        float jitter = MathUtil.randomFloat(0.8f, 1.2f);
        long adjustedDuration = (long) (durationMs * jitter);

        // Scale by angle distance
        float totalAngle = (float) Math.sqrt(yawDiff * yawDiff +
                (targetPitch - startPitch) * (targetPitch - startPitch));
        if (totalAngle < 25) adjustedDuration = (long) (adjustedDuration * 0.65f);
        else if (totalAngle < 45) adjustedDuration = (long) (adjustedDuration * 0.77f);
        else if (totalAngle < 80) adjustedDuration = (long) (adjustedDuration * 0.9f);
        else if (totalAngle > 100) adjustedDuration = (long) (adjustedDuration * 1.1f);

        adjustedDuration = Math.max(adjustedDuration, 50);

        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + adjustedDuration;
        this.easingModifier = MathUtil.randomFloat(-0.25f, 0.25f);
        this.rotating = true;

        // Random overshoot chance (30%)
        this.useOvershoot = Math.random() < 0.3 && totalAngle > 5;
        if (useOvershoot) {
            this.overshootYaw = MathUtil.randomFloat(-2.0f, 2.0f);
            this.overshootPitch = MathUtil.randomFloat(-1.0f, 1.0f);
            this.overshootPhase = false;
        }
    }

    public void easeToEntity(Entity target) {
        if (mc.player == null || target == null) return;
        float[] rot = getHumanizedRotationTo(target);
        float dist = (float) mc.player.getEntityPos().distanceTo(target.getEntityPos());
        long duration = (long) (MacroConfig.rotationBaseTimeMs * MathUtil.randomFloat(0.8f, 1.2f));
        if (dist > 8) duration = (long) (duration * 1.3f);
        easeTo(rot[0], rot[1], duration);
    }

    public void lookDownAtFloor() {
        if (mc.player == null) return;
        float pitch = MathUtil.randomFloat(78.0f, 85.0f);
        float yaw = mc.player.getYaw() + MathUtil.randomFloat(-2.0f, 2.0f);
        easeTo(yaw, pitch, (long) (MacroConfig.rotationBaseTimeMs * 0.7f));
    }

    public void onTick() {
        if (!rotating || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (now >= endTime) {
            if (useOvershoot && !overshootPhase) {
                // Overshoot: we went past target, now ease back
                overshootPhase = true;
                startYaw = mc.player.getYaw();
                startPitch = mc.player.getPitch();
                targetYaw = targetYaw - overshootYaw;
                targetPitch = targetPitch - overshootPitch;
                startTime = now;
                endTime = now + (long) MathUtil.randomFloat(80, 150);
                return;
            }
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(MathHelper.clamp(targetPitch, -90.0f, 90.0f));
            rotating = false;
            return;
        }

        float progress = (float) (now - startTime) / (float) (endTime - startTime);
        float easedProgress = MathUtil.easeOutBack(progress, easingModifier);
        easedProgress = MathUtil.clamp(easedProgress, 0.0f, 1.15f);

        float currentYaw, currentPitch;
        if (useOvershoot && !overshootPhase) {
            // During first phase, overshoot the target slightly
            currentYaw = startYaw + (targetYaw + overshootYaw - startYaw) * easedProgress;
            currentPitch = startPitch + (targetPitch + overshootPitch - startPitch) * easedProgress;
        } else {
            currentYaw = startYaw + (targetYaw - startYaw) * easedProgress;
            currentPitch = startPitch + (targetPitch - startPitch) * easedProgress;
        }

        mc.player.setYaw(currentYaw);
        mc.player.setPitch(MathHelper.clamp(currentPitch, -90.0f, 90.0f));
    }

    public boolean isRotating() {
        return rotating;
    }

    public void stop() {
        rotating = false;
    }

    /**
     * Gets rotation to a random point within the entity's hitbox (humanized).
     * Never aims at exact center.
     */
    public float[] getHumanizedRotationTo(Entity target) {
        if (mc.player == null) return new float[]{0, 0};

        Box box = target.getBoundingBox();
        double offsetX = MathUtil.randomDouble(-0.3, 0.3) * box.getLengthX();
        double offsetY = MathUtil.randomDouble(-0.3, 0.3) * box.getLengthY();
        double offsetZ = MathUtil.randomDouble(-0.3, 0.3) * box.getLengthZ();

        Vec3d targetPos = new Vec3d(
                (box.minX + box.maxX) / 2.0 + offsetX,
                (box.minY + box.maxY) / 2.0 + offsetY,
                (box.minZ + box.maxZ) / 2.0 + offsetZ
        );

        return getRotationTo(targetPos);
    }

    public float[] getRotationTo(Vec3d target) {
        if (mc.player == null) return new float[]{0, 0};

        Vec3d eyePos = mc.player.getEyePos();
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{yaw, pitch};
    }
}
