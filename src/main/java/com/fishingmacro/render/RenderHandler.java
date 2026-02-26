package com.fishingmacro.render;

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

import java.util.List;

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

    // --- HUD Overlay (top-right corner) ---

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        FishingMacro macro = FishingMacro.getInstance();
        if (!macro.isRunning()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        MacroState state = macro.getState();
        String statusText = state.getDisplayName();
        int color = getStateColor(state);

        int screenWidth = mc.getWindow().getScaledWidth();
        int textWidth = textRenderer.getWidth(statusText);

        int padding = 4;
        int x = screenWidth - textWidth - padding - 4;
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

            int distX = screenWidth - distWidth - padding - 4;
            int distY = y + textRenderer.fontHeight + padding + 2;

            context.fill(distX - padding, distY - padding,
                    distX + distWidth + padding, distY + textRenderer.fontHeight + padding,
                    0xAA000000);
            context.drawText(textRenderer, distText, distX, distY, 0xFFFF8800, true);
        }
    }

    private int getStateColor(MacroState state) {
        return switch (state) {
            case WAITING_FOR_BITE -> 0xFF55FF55;   // Green
            case CASTING, ROTATING, RE_CASTING, RESUMING -> 0xFFFFFF55; // Yellow
            case KILLING, SWAPPING_TO_WEAPON -> 0xFFFF5555; // Red
            case RETURNING_TO_SPOT -> 0xFFFF8800;  // Orange
            case SEA_CREATURE_DETECTED -> 0xFFFF5555; // Red
            default -> 0xFFFFFFFF; // White
        };
    }

    // --- World Rendering (Baritone path line) ---

    private void onWorldRender(WorldRenderContext context) {
        FishingMacro macro = FishingMacro.getInstance();
        if (!macro.isRunning()) return;

        MacroState state = macro.getState();
        if (state != MacroState.RETURNING_TO_SPOT) return;

        ReturnHandler returnHandler = macro.getReturnHandler();
        if (!returnHandler.isBaritoneAvailable()) return;

        List<Vec3d> pathNodes = returnHandler.getCurrentPathNodes();
        Vec3d savedPos = returnHandler.getSavedPos();

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

            // Draw path lines between consecutive nodes
            if (pathNodes.size() >= 2) {
                for (int i = 0; i < pathNodes.size() - 1; i++) {
                    Vec3d from = pathNodes.get(i);
                    Vec3d to = pathNodes.get(i + 1);
                    drawLine(lines, entry, from, to, 0, 220, 180, 220);
                }
            }

            // Draw destination marker (X) at saved position
            if (savedPos != null) {
                float size = 0.4f;
                Vec3d a1 = new Vec3d(savedPos.x - size, savedPos.y + 0.1, savedPos.z - size);
                Vec3d a2 = new Vec3d(savedPos.x + size, savedPos.y + 0.1, savedPos.z + size);
                Vec3d b1 = new Vec3d(savedPos.x - size, savedPos.y + 0.1, savedPos.z + size);
                Vec3d b2 = new Vec3d(savedPos.x + size, savedPos.y + 0.1, savedPos.z - size);
                drawLine(lines, entry, a1, a2, 255, 50, 50, 220);
                drawLine(lines, entry, b1, b2, 255, 50, 50, 220);
            }
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
