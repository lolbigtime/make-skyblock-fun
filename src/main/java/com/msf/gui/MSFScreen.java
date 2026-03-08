package com.msf.gui;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.feature.system.FeatureManager;
import com.msf.macro.FishingMacro;
import com.msf.pathfinding.ReturnHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MSFScreen extends Screen {
    private int selectedTab = 0;
    private static final String[] TAB_NAMES = {"Scripts", "QOL", "Settings"};
    private final List<ClickableWidget> contentWidgets = new ArrayList<>();
    private int contentTop;

    public MSFScreen() {
        super(Text.literal("MSF"));
    }

    @Override
    protected void init() {
        // Tab buttons at top
        int tabWidth = 80;
        int tabSpacing = 4;
        int totalWidth = TAB_NAMES.length * tabWidth + (TAB_NAMES.length - 1) * tabSpacing;
        int startX = (width - totalWidth) / 2;
        int tabY = 10;

        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int tabIndex = i;
            ButtonWidget tabBtn = ButtonWidget.builder(
                    Text.literal(selectedTab == i ? "[" + TAB_NAMES[i] + "]" : TAB_NAMES[i]),
                    btn -> selectTab(tabIndex)
            ).dimensions(startX + i * (tabWidth + tabSpacing), tabY, tabWidth, 20).build();
            addDrawableChild(tabBtn);
        }

        contentTop = tabY + 28;

        // Footer
        int footerY = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save Config"), btn -> MacroConfig.save())
                .dimensions(width / 2 - 104, footerY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(width / 2 + 4, footerY, 100, 20).build());

        populateTab();
    }

    private void selectTab(int tab) {
        selectedTab = tab;
        clearAndInit();
    }

    private void populateTab() {
        // Remove old content widgets
        for (ClickableWidget w : contentWidgets) {
            remove(w);
        }
        contentWidgets.clear();

        switch (selectedTab) {
            case 0 -> populateFeatures(FeatureCategory.SCRIPT);
            case 1 -> populateFeatures(FeatureCategory.QOL);
            case 2 -> populateSettings();
        }
    }

    private void addContentWidget(ClickableWidget widget) {
        contentWidgets.add(widget);
        addDrawableChild(widget);
    }

    private int nextY() {
        return contentTop + contentWidgets.size() * 24;
    }

    private void populateFeatures(FeatureCategory category) {
        int x = width / 2 - 155;
        FeatureManager fm = FeatureManager.getInstance();

        for (Feature feature : fm.getByCategory(category)) {
            int y = nextY();
            CheckboxWidget cb = CheckboxWidget.builder(
                    Text.literal(feature.getName() + " - " + feature.getDescription()),
                    textRenderer
            ).pos(x, y).checked(feature.isEnabled()).callback((checkbox, checked) -> {
                if (checked) {
                    feature.setEnabled(true);
                    feature.onEnable();
                    MacroConfig.featureStates.put(feature.getName(), true);
                } else {
                    feature.setEnabled(false);
                    feature.onDisable();
                    MacroConfig.featureStates.put(feature.getName(), false);
                }
            }).build();
            addContentWidget(cb);

            // Feature-specific settings
            feature.addSettingsWidgets(w -> {
                int wy = nextY();
                w.setX(x + 20);
                w.setY(wy);
                if (w.getWidth() == 0) w.setWidth(290);
                addContentWidget(w);
            });
        }
    }

    private void populateSettings() {
        int x = width / 2 - 155;

        // Combat
        addSlider("Kill Timeout (ms)", 1000, 30000, MacroConfig.killTimeoutMs, v -> MacroConfig.killTimeoutMs = v);
        addCheckbox("Use Hyperion", MacroConfig.useHyperion, v -> MacroConfig.useHyperion = v);
        addSlider("Hyperion Retry Delay (ms)", 50, 1000, MacroConfig.hyperionRetryDelayMs, v -> MacroConfig.hyperionRetryDelayMs = v);
        addSlider("Hyperion Max Attempts", 1, 20, MacroConfig.hyperionMaxAttempts, v -> MacroConfig.hyperionMaxAttempts = v);
        addSlider("Melee CPS Min", 1, 20, MacroConfig.meleeCpsMin, v -> MacroConfig.meleeCpsMin = v);
        addSlider("Melee CPS Max", 1, 20, MacroConfig.meleeCpsMax, v -> MacroConfig.meleeCpsMax = v);

        // Fishing Delays
        addSlider("Reel Delay (ms)", 50, 1000, MacroConfig.reelDelayMs, v -> MacroConfig.reelDelayMs = v);
        addSlider("Cast Delay (ms)", 50, 1000, MacroConfig.castDelayMs, v -> MacroConfig.castDelayMs = v);
        addSlider("Post-Reel Delay (ms)", 50, 2000, MacroConfig.postReelDelayMs, v -> MacroConfig.postReelDelayMs = v);

        // Anti-AFK
        addSlider("Anti-AFK Interval (ms)", 5000, 60000, MacroConfig.antiAfkIntervalMs, v -> MacroConfig.antiAfkIntervalMs = v);
        addFloatSlider("Max Yaw Drift", 0.5f, 15.0f, MacroConfig.antiAfkMaxYawDrift, v -> MacroConfig.antiAfkMaxYawDrift = v);
        addFloatSlider("Max Pitch Drift", 0.5f, 10.0f, MacroConfig.antiAfkMaxPitchDrift, v -> MacroConfig.antiAfkMaxPitchDrift = v);

        // Knockback
        addFloatSlider("Knockback Threshold", 1.0f, 15.0f, (float) MacroConfig.knockbackThreshold, v -> MacroConfig.knockbackThreshold = v);
        addSlider("KB Reaction (ms)", 50, 1000, MacroConfig.knockbackReactionMs, v -> MacroConfig.knockbackReactionMs = v);

        // Return
        if (ReturnHandler.isBaritoneAvailable()) {
            addCheckbox("Use Baritone", MacroConfig.useBaritone, v -> MacroConfig.useBaritone = v);
        }
        addSlider("Return Timeout (ms)", 5000, 60000, MacroConfig.returnTimeoutMs, v -> MacroConfig.returnTimeoutMs = v);
        addSlider("Stuck Threshold Ticks", 5, 60, MacroConfig.returnStuckThresholdTicks, v -> MacroConfig.returnStuckThresholdTicks = v);
        addSlider("Max Stuck Attempts", 1, 20, MacroConfig.returnMaxStuckAttempts, v -> MacroConfig.returnMaxStuckAttempts = v);

        // Detection
        addFloatSlider("Detection Radius", 3.0f, 30.0f, (float) MacroConfig.seaCreatureDetectionRadius, v -> MacroConfig.seaCreatureDetectionRadius = v);
        addSlider("Rotation Base Time (ms)", 100, 1500, MacroConfig.rotationBaseTimeMs, v -> MacroConfig.rotationBaseTimeMs = v);

        // Failsafe
        addCheckbox("Failsafe Enabled", MacroConfig.failsafeEnabled, v -> MacroConfig.failsafeEnabled = v);
        addFloatSlider("Teleport Threshold", 2.0f, 20.0f, (float) MacroConfig.failsafeTeleportThreshold, v -> MacroConfig.failsafeTeleportThreshold = v);
        addFloatSlider("Rotation Threshold", 5.0f, 90.0f, MacroConfig.failsafeRotationThreshold, v -> MacroConfig.failsafeRotationThreshold = v);
    }

    private void addSlider(String label, int min, int max, int current, java.util.function.Consumer<Integer> setter) {
        ConfigSliderWidget slider = ConfigSliderWidget.forInt(label, min, max, current, setter::accept);
        slider.setX(width / 2 - 155);
        slider.setY(nextY());
        addContentWidget(slider);
    }

    private void addFloatSlider(String label, float min, float max, float current, java.util.function.Consumer<Float> setter) {
        ConfigSliderWidget slider = ConfigSliderWidget.forFloat(label, min, max, current, setter::accept);
        slider.setX(width / 2 - 155);
        slider.setY(nextY());
        addContentWidget(slider);
    }

    private void addCheckbox(String label, boolean current, java.util.function.Consumer<Boolean> setter) {
        CheckboxWidget cb = CheckboxWidget.builder(Text.literal(label), textRenderer)
                .pos(width / 2 - 155, nextY())
                .checked(current)
                .callback((checkbox, checked) -> setter.accept(checked))
                .build();
        addContentWidget(cb);
    }

    @Override
    public void close() {
        MacroConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
