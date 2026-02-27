package com.fishingmacro.render;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.feature.RotationTestMode;
import com.fishingmacro.macro.FishingMacro;
import com.fishingmacro.macro.MacroState;
import com.fishingmacro.pathfinding.ReturnHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public class RenderHandler {
    private static RenderHandler instance;

    private RenderHandler() {}

    public static RenderHandler getInstance() {
        if (instance == null) instance = new RenderHandler();
        return instance;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onWorldRender);
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

    // --- World Rendering (line to saved position) ---

    private void onWorldRender(WorldRenderContext context) {
        FishingMacro macro = FishingMacro.getInstance();
        if (!macro.isRunning()) return;

        MacroState state = macro.getState();
        if (state != MacroState.RETURNING_TO_SPOT) return;

        ReturnHandler returnHandler = macro.getReturnHandler();
        Vec3d savedPos = returnHandler.getSavedPos();
        if (savedPos == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        MatrixStack matrices = context.matrices();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        try {
            MatrixStack.Entry entry = matrices.peek();

            VertexConsumerProvider consumers = context.consumers();
            VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());

            // Draw line from player to saved position
            Vec3d playerPos = mc.player.getEntityPos();
            drawLine(lines, entry, playerPos, savedPos, 0, 220, 180, 220);

            // Draw destination marker (X) at saved position
            float size = 0.4f;
            Vec3d a1 = new Vec3d(savedPos.x - size, savedPos.y + 0.1, savedPos.z - size);
            Vec3d a2 = new Vec3d(savedPos.x + size, savedPos.y + 0.1, savedPos.z + size);
            Vec3d b1 = new Vec3d(savedPos.x - size, savedPos.y + 0.1, savedPos.z + size);
            Vec3d b2 = new Vec3d(savedPos.x + size, savedPos.y + 0.1, savedPos.z - size);
            drawLine(lines, entry, a1, a2, 255, 50, 50, 220);
            drawLine(lines, entry, b1, b2, 255, 50, 50, 220);
        } catch (Exception e) {
            // Gracefully handle any rendering API differences
        }

        matrices.pop();
    }

    private void drawLine(VertexConsumer consumer, MatrixStack.Entry entry,
                          Vec3d from, Vec3d to, int r, int g, int b, int a) {
        float dx = (float) (to.x - from.x);
        float dy = (float) (to.y - from.y);
        float dz = (float) (to.z - from.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        } else {
            dy = 1.0f;
        }

        consumer.vertex(entry, (float) from.x, (float) from.y, (float) from.z)
                .color(r, g, b, a)
                .normal(entry, dx, dy, dz);
        consumer.vertex(entry, (float) to.x, (float) to.y, (float) to.z)
                .color(r, g, b, a)
                .normal(entry, dx, dy, dz);
    }
}
