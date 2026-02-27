package com.fishingmacro.feature;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RotationTestMode {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static RotationTestMode instance;

    private boolean running = false;
    private int step = 0;
    private boolean waitingForRotation = false;
    private long pauseUntil = 0;

    private static final String[] STEP_NAMES = {
            "Left 90\u00B0", "Right 90\u00B0", "Up 45\u00B0", "Down to floor"
    };

    public static RotationTestMode getInstance() {
        if (instance == null) instance = new RotationTestMode();
        return instance;
    }

    public void start() {
        if (mc.player == null) return;
        running = true;
        step = 0;
        waitingForRotation = false;
        pauseUntil = 0;
        sendMessage("Rotation test started (speed: " + MacroConfig.rotationBaseTimeMs + " ms)");
        initiateStep();
    }

    public void stop() {
        running = false;
        step = 0;
        waitingForRotation = false;
    }

    public boolean isRunning() {
        return running;
    }

    public String getCurrentStepName() {
        if (step >= 0 && step < STEP_NAMES.length) {
            return STEP_NAMES[step];
        }
        return "Done";
    }

    public int getStep() {
        return step;
    }

    public int getTotalSteps() {
        return STEP_NAMES.length;
    }

    public void onTick() {
        if (!running || mc.player == null) return;

        // Waiting for pause between steps
        if (pauseUntil > 0) {
            if (System.currentTimeMillis() < pauseUntil) return;
            pauseUntil = 0;
            initiateStep();
            return;
        }

        // Waiting for rotation to complete
        if (waitingForRotation) {
            if (!RotationHandler.getInstance().isRotating()) {
                waitingForRotation = false;
                step++;
                if (step >= STEP_NAMES.length) {
                    sendMessage("Rotation test complete!");
                    stop();
                    return;
                }
                // Pause 500ms between steps
                pauseUntil = System.currentTimeMillis() + 500;
            }
        }
    }

    private void initiateStep() {
        if (mc.player == null) return;
        RotationHandler rh = RotationHandler.getInstance();
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        long baseTime = MacroConfig.rotationBaseTimeMs;

        switch (step) {
            case 0 -> // Left 90 degrees
                    rh.easeTo(currentYaw - 90, currentPitch, baseTime);
            case 1 -> // Right 90 degrees (back to original + 90 more right)
                    rh.easeTo(currentYaw + 90, currentPitch, baseTime);
            case 2 -> // Up 45 degrees
                    rh.easeTo(currentYaw, currentPitch - 45, baseTime);
            case 3 -> // Down to floor
                    rh.easeTo(currentYaw, 80, (long) (baseTime * 0.8));
        }
        waitingForRotation = true;
    }

    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(
                    Text.literal("[MSF] " + message).formatted(Formatting.AQUA),
                    true
            );
        }
    }
}
