package com.fishingmacro.feature;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.macro.FishingMacro;
import com.fishingmacro.macro.MacroState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class FailsafeHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static FailsafeHandler instance;

    private Vec3d lastPos;
    private float expectedYaw;
    private float expectedPitch;
    private boolean initialized = false;

    public static FailsafeHandler getInstance() {
        if (instance == null) instance = new FailsafeHandler();
        return instance;
    }

    public void reset() {
        initialized = false;
        lastPos = null;
    }

    public void onTick() {
        if (!MacroConfig.failsafeEnabled) return;

        FishingMacro macro = FishingMacro.getInstance();
        if (!macro.isRunning() || mc.player == null) {
            reset();
            return;
        }

        Vec3d currentPos = mc.player.getEntityPos();
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        if (!initialized) {
            lastPos = currentPos;
            expectedYaw = currentYaw;
            expectedPitch = currentPitch;
            initialized = true;
            return;
        }

        // --- Teleport Detection ---
        MacroState state = macro.getState();
        boolean isMoving = state == MacroState.RETURNING_TO_SPOT
                || state == MacroState.KILLING;

        if (!isMoving && lastPos != null) {
            double displacement = currentPos.distanceTo(lastPos);
            double velocity = mc.player.getVelocity().length();

            // If displacement is large and velocity doesn't account for it, it's a teleport
            if (displacement > MacroConfig.failsafeTeleportThreshold && velocity < displacement * 0.3) {
                trigger("Teleport detected! Moved " + String.format("%.1f", displacement)
                        + " blocks in one tick");
                lastPos = currentPos;
                return;
            }
        }
        lastPos = currentPos;

        // --- Rotation Detection ---
        // Only check when RotationHandler is NOT actively rotating (we expect it to change angles)
        if (!RotationHandler.getInstance().isRotating()) {
            float yawDiff = Math.abs(wrapAngle(currentYaw - expectedYaw));
            float pitchDiff = Math.abs(currentPitch - expectedPitch);

            if (yawDiff > MacroConfig.failsafeRotationThreshold
                    || pitchDiff > MacroConfig.failsafeRotationThreshold) {
                trigger("Forced rotation detected! Yaw diff: " + String.format("%.1f", yawDiff)
                        + ", Pitch diff: " + String.format("%.1f", pitchDiff));
                expectedYaw = currentYaw;
                expectedPitch = currentPitch;
                return;
            }
        }
        expectedYaw = currentYaw;
        expectedPitch = currentPitch;

        // --- Item Swap Detection ---
        int rodSlot = macro.getDetectedRodSlot();
        int weaponSlot = macro.getDetectedWeaponSlot();

        ItemStack rodStack = mc.player.getInventory().getStack(rodSlot);
        if (!(rodStack.getItem() instanceof FishingRodItem) && !rodStack.isEmpty()) {
            // Rod slot changed to something else
            trigger("Rod slot " + rodSlot + " no longer contains a fishing rod!");
            return;
        }

        if (MacroConfig.useHyperion) {
            ItemStack weaponStack = mc.player.getInventory().getStack(weaponSlot);
            if (!weaponStack.isEmpty()
                    && !weaponStack.getName().getString().toLowerCase().contains("hyperion")) {
                trigger("Weapon slot " + weaponSlot + " no longer contains Hyperion!");
            }
        }
    }

    private void trigger(String reason) {
        System.err.println("[MSF Failsafe] TRIGGERED: " + reason);

        FishingMacro.getInstance().stop();

        // Red warning in chat
        if (mc.player != null) {
            mc.player.sendMessage(
                    Text.literal("[MSF FAILSAFE] " + reason).formatted(Formatting.RED, Formatting.BOLD),
                    false
            );
        }

        // System alert
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}

        // Focus Minecraft window
        try {
            long window = mc.getWindow().getHandle();
            GLFW.glfwFocusWindow(window);
            GLFW.glfwRequestWindowAttention(window);
        } catch (Exception ignored) {}

        reset();
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
}
