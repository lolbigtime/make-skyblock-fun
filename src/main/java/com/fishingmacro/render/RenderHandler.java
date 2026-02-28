package com.fishingmacro.render;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.feature.RotationTestMode;
import com.fishingmacro.macro.FishingMacro;
import com.fishingmacro.macro.MacroState;
import com.fishingmacro.pathfinding.ReturnHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class RenderHandler {
    private static RenderHandler instance;

    private RenderHandler() {}

    public static RenderHandler getInstance() {
        if (instance == null) instance = new RenderHandler();
        return instance;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    // --- HUD Overlay (top-left corner) ---

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        // Rotation test mode HUD
        RotationTestMode testMode = RotationTestMode.getInstance();
        if (testMode.isRunning()) {
            renderTestModeHud(context);
        }

        FishingMacro macro = FishingMacro.getInstance();
        if (!macro.isRunning()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        MacroState state = macro.getState();
        String statusText = state.getDisplayName();
        int color = getStateColor(state);

        int textWidth = textRenderer.getWidth(statusText);

        int padding = 4;
        int x = padding + 4;
        int y = 4;

        // Background rectangle
        context.fill(x - padding, y - padding,
                x + textWidth + padding, y + textRenderer.fontHeight + padding,
                0xAA000000);

        // Status text
        context.drawText(textRenderer, statusText, x, y, color, true);

        // Show distance when returning
        if (state == MacroState.RETURNING_TO_SPOT) {
            ReturnHandler returnHandler = macro.getReturnHandler();
            double dist = returnHandler.getDistanceRemaining();
            String distText = String.format("%.1f blocks away", dist);
            int distWidth = textRenderer.getWidth(distText);

            int distX = padding + 4;
            int distY = y + textRenderer.fontHeight + padding + 2;

            context.fill(distX - padding, distY - padding,
                    distX + distWidth + padding, distY + textRenderer.fontHeight + padding,
                    0xAA000000);
            context.drawText(textRenderer, distText, distX, distY, 0xFFFF8800, true);
        }
    }

    private void renderTestModeHud(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        RotationTestMode testMode = RotationTestMode.getInstance();
        String title = "ROTATION TEST";
        String stepText = "Step " + (testMode.getStep() + 1) + "/" + testMode.getTotalSteps()
                + ": " + testMode.getCurrentStepName();
        String speedText = "Speed: " + MacroConfig.rotationBaseTimeMs + " ms";

        int padding = 4;
        int x = padding + 4;
        int y = 4;

        // Title background
        int titleWidth = textRenderer.getWidth(title);
        context.fill(x - padding, y - padding,
                x + titleWidth + padding, y + textRenderer.fontHeight + padding,
                0xAA000000);
        context.drawText(textRenderer, title, x, y, 0xFFFF5555, true);

        // Step info
        y += textRenderer.fontHeight + padding + 2;
        int stepWidth = textRenderer.getWidth(stepText);
        context.fill(x - padding, y - padding,
                x + stepWidth + padding, y + textRenderer.fontHeight + padding,
                0xAA000000);
        context.drawText(textRenderer, stepText, x, y, 0xFFFFFF55, true);

        // Speed info
        y += textRenderer.fontHeight + padding + 2;
        int speedWidth = textRenderer.getWidth(speedText);
        context.fill(x - padding, y - padding,
                x + speedWidth + padding, y + textRenderer.fontHeight + padding,
                0xAA000000);
        context.drawText(textRenderer, speedText, x, y, 0xFFAAAAAA, true);
    }

    private int getStateColor(MacroState state) {
        return switch (state) {
            case WAITING_FOR_BITE -> 0xFF55FF55;   // Green
            case CASTING, RE_CASTING, RESUMING -> 0xFFFFFF55; // Yellow
            case KILLING, SWAPPING_TO_WEAPON -> 0xFFFF5555; // Red
            case RETURNING_TO_SPOT -> 0xFFFF8800;  // Orange
            case SEA_CREATURE_DETECTED -> 0xFFFF5555; // Red
            default -> 0xFFFFFFFF; // White
        };
    }

}
