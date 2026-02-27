package com.fishingmacro.gui;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.gui.widget.ConfigSliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;
    private int contentHeight = 0;

    // Temp values for cancel support
    private boolean tmpUseHyperion;
    private boolean tmpFailsafeEnabled;

    public SettingsScreen(Screen parent) {
        super(Text.literal("MSF Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        tmpUseHyperion = MacroConfig.useHyperion;
        tmpFailsafeEnabled = MacroConfig.failsafeEnabled;

        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        int centerX = width / 2;
        int sliderWidth = 200;
        int left = centerX - sliderWidth / 2;
        int y = 30 - scrollOffset;
        int rowHeight = 24;

        // --- Combat ---
        y += rowHeight; // header space

        addDrawableChild(CyclingButtonWidget.onOffBuilder(tmpUseHyperion)
                .build(left, y, sliderWidth, 20, Text.literal("Use Hyperion"),
                        (button, value) -> tmpUseHyperion = value));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Kill Timeout", 1000, 30000, MacroConfig.killTimeoutMs,
                true, " ms", v -> MacroConfig.killTimeoutMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Hyperion Retry Delay", 50, 1000, MacroConfig.hyperionRetryDelayMs,
                true, " ms", v -> MacroConfig.hyperionRetryDelayMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Hyperion Max Attempts", 1, 20, MacroConfig.hyperionMaxAttempts,
                true, "", v -> MacroConfig.hyperionMaxAttempts = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Melee CPS Min", 1, 20, MacroConfig.meleeCpsMin,
                true, "", v -> MacroConfig.meleeCpsMin = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Melee CPS Max", 1, 20, MacroConfig.meleeCpsMax,
                true, "", v -> MacroConfig.meleeCpsMax = v.intValue()));
        y += rowHeight;

        // --- Fishing Delays ---
        y += rowHeight; // header space

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Reel Delay Min", 50, 1000, MacroConfig.reelDelayMinMs,
                true, " ms", v -> MacroConfig.reelDelayMinMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Reel Delay Max", 50, 2000, MacroConfig.reelDelayMaxMs,
                true, " ms", v -> MacroConfig.reelDelayMaxMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Cast Delay Min", 50, 1000, MacroConfig.castDelayMinMs,
                true, " ms", v -> MacroConfig.castDelayMinMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Cast Delay Max", 50, 2000, MacroConfig.castDelayMaxMs,
                true, " ms", v -> MacroConfig.castDelayMaxMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Post-Reel Delay Min", 50, 2000, MacroConfig.postReelDelayMinMs,
                true, " ms", v -> MacroConfig.postReelDelayMinMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Post-Reel Delay Max", 50, 3000, MacroConfig.postReelDelayMaxMs,
                true, " ms", v -> MacroConfig.postReelDelayMaxMs = v.intValue()));
        y += rowHeight;

        // --- Anti-AFK ---
        y += rowHeight; // header space

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Anti-AFK Interval Min", 5000, 60000, MacroConfig.antiAfkMinIntervalMs,
                true, " ms", v -> MacroConfig.antiAfkMinIntervalMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Anti-AFK Interval Max", 5000, 120000, MacroConfig.antiAfkMaxIntervalMs,
                true, " ms", v -> MacroConfig.antiAfkMaxIntervalMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Max Yaw Drift", 0.5, 15.0, MacroConfig.antiAfkMaxYawDrift,
                false, "", v -> MacroConfig.antiAfkMaxYawDrift = v.floatValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Max Pitch Drift", 0.5, 10.0, MacroConfig.antiAfkMaxPitchDrift,
                false, "", v -> MacroConfig.antiAfkMaxPitchDrift = v.floatValue()));
        y += rowHeight;

        // --- Knockback ---
        y += rowHeight; // header space

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Knockback Threshold", 1.0, 15.0, MacroConfig.knockbackThreshold,
                false, "", v -> MacroConfig.knockbackThreshold = v.doubleValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "KB Reaction Min", 50, 1000, MacroConfig.knockbackReactionMinMs,
                true, " ms", v -> MacroConfig.knockbackReactionMinMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "KB Reaction Max", 50, 2000, MacroConfig.knockbackReactionMaxMs,
                true, " ms", v -> MacroConfig.knockbackReactionMaxMs = v.intValue()));
        y += rowHeight;

        // --- Return ---
        y += rowHeight; // header space

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Return Timeout", 5000, 60000, MacroConfig.returnTimeoutMs,
                true, " ms", v -> MacroConfig.returnTimeoutMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Stuck Threshold Ticks", 5, 60, MacroConfig.returnStuckThresholdTicks,
                true, "", v -> MacroConfig.returnStuckThresholdTicks = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Max Stuck Attempts", 1, 20, MacroConfig.returnMaxStuckAttempts,
                true, "", v -> MacroConfig.returnMaxStuckAttempts = v.intValue()));
        y += rowHeight;

        // --- Detection ---
        y += rowHeight; // header space

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Detection Radius", 3.0, 30.0, MacroConfig.seaCreatureDetectionRadius,
                false, "", v -> MacroConfig.seaCreatureDetectionRadius = v.doubleValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Rotation Base Time", 100, 1500, MacroConfig.rotationBaseTimeMs,
                true, " ms", v -> MacroConfig.rotationBaseTimeMs = v.intValue()));
        y += rowHeight;

        // --- Failsafe ---
        y += rowHeight; // header space

        addDrawableChild(CyclingButtonWidget.onOffBuilder(tmpFailsafeEnabled)
                .build(left, y, sliderWidth, 20, Text.literal("Failsafe"),
                        (button, value) -> tmpFailsafeEnabled = value));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Teleport Threshold", 2.0, 20.0, MacroConfig.failsafeTeleportThreshold,
                false, "", v -> MacroConfig.failsafeTeleportThreshold = v.doubleValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Rotation Threshold", 5.0, 90.0, MacroConfig.failsafeRotationThreshold,
                false, "", v -> MacroConfig.failsafeRotationThreshold = v.floatValue()));
        y += rowHeight;

        // --- Buttons ---
        y += 10;

        int buttonWidth = 95;
        int gap = 10;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            MacroConfig.useHyperion = tmpUseHyperion;
            MacroConfig.failsafeEnabled = tmpFailsafeEnabled;
            MacroConfig.save();
            client.setScreen(parent);
        }).dimensions(centerX - buttonWidth - gap / 2, y, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            MacroConfig.load(); // discard changes by reloading
            client.setScreen(parent);
        }).dimensions(centerX + gap / 2, y, buttonWidth, 20).build());

        contentHeight = y + 20 + scrollOffset + 10;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, contentHeight - height);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 10));
        rebuildWidgets();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int y = 30 - scrollOffset;
        int rowHeight = 24;

        // Category headers
        drawCenteredHeader(context, "Combat", centerX, y);
        y += rowHeight * 7; // 6 widgets + header
        drawCenteredHeader(context, "Fishing Delays", centerX, y);
        y += rowHeight * 7;
        drawCenteredHeader(context, "Anti-AFK", centerX, y);
        y += rowHeight * 5;
        drawCenteredHeader(context, "Knockback", centerX, y);
        y += rowHeight * 4;
        drawCenteredHeader(context, "Return", centerX, y);
        y += rowHeight * 4;
        drawCenteredHeader(context, "Detection", centerX, y);
        y += rowHeight * 3;
        drawCenteredHeader(context, "Failsafe", centerX, y);
    }

    private void drawCenteredHeader(DrawContext context, String text, int centerX, int y) {
        int textWidth = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, centerX - textWidth / 2, y + 6, 0xFF55FFFF, true);
    }

    @Override
    public void close() {
        MacroConfig.load(); // discard on escape
        client.setScreen(parent);
    }
}
