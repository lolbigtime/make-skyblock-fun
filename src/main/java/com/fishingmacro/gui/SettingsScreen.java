package com.fishingmacro.gui;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.gui.widget.ConfigSliderWidget;
import com.fishingmacro.pathfinding.ReturnHandler;
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
    private boolean tmpUseBaritone;

    public SettingsScreen(Screen parent) {
        super(Text.literal("MSF Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        tmpUseHyperion = MacroConfig.useHyperion;
        tmpFailsafeEnabled = MacroConfig.failsafeEnabled;
        tmpUseBaritone = MacroConfig.useBaritone;

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
                "Reel Delay", 50, 1000, MacroConfig.reelDelayMs,
                true, " ms", v -> MacroConfig.reelDelayMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Cast Delay", 50, 1000, MacroConfig.castDelayMs,
                true, " ms", v -> MacroConfig.castDelayMs = v.intValue()));
        y += rowHeight;

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Post-Reel Delay", 50, 2000, MacroConfig.postReelDelayMs,
                true, " ms", v -> MacroConfig.postReelDelayMs = v.intValue()));
        y += rowHeight;

        // --- Anti-AFK ---
        y += rowHeight; // header space

        addDrawableChild(new ConfigSliderWidget(left, y, sliderWidth, 20,
                "Anti-AFK Interval", 5000, 60000, MacroConfig.antiAfkIntervalMs,
                true, " ms", v -> MacroConfig.antiAfkIntervalMs = v.intValue()));
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
                "KB Reaction", 50, 1000, MacroConfig.knockbackReactionMs,
                true, " ms", v -> MacroConfig.knockbackReactionMs = v.intValue()));
        y += rowHeight;

        // --- Return ---
        y += rowHeight; // header space

        boolean baritoneInstalled = ReturnHandler.isBaritoneAvailable();
        String baritoneLabel = baritoneInstalled ? "Use Baritone" : "Use Baritone (not installed)";
        CyclingButtonWidget<Boolean> baritoneButton = CyclingButtonWidget.onOffBuilder(tmpUseBaritone)
                .build(left, y, sliderWidth, 20, Text.literal(baritoneLabel),
                        (button, value) -> tmpUseBaritone = value);
        if (!baritoneInstalled) {
            baritoneButton.active = false;
        }
        addDrawableChild(baritoneButton);
        y += rowHeight;

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
            MacroConfig.useBaritone = tmpUseBaritone;
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
        y += rowHeight * 7; // header + 6 widgets
        drawCenteredHeader(context, "Fishing Delays", centerX, y);
        y += rowHeight * 4; // header + 3 widgets
        drawCenteredHeader(context, "Anti-AFK", centerX, y);
        y += rowHeight * 4; // header + 3 widgets
        drawCenteredHeader(context, "Knockback", centerX, y);
        y += rowHeight * 3; // header + 2 widgets
        drawCenteredHeader(context, "Return", centerX, y);
        y += rowHeight * 5; // header + 4 widgets
        drawCenteredHeader(context, "Detection", centerX, y);
        y += rowHeight * 3; // header + 2 widgets
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
