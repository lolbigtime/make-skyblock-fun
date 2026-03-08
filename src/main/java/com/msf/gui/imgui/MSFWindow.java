package com.msf.gui.imgui;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.feature.system.FeatureManager;
import com.msf.pathfinding.ReturnHandler;
import imgui.ImGui;
import imgui.type.ImBoolean;

public class MSFWindow {

    public void render() {
        if (!ImGui.begin("MSF")) {
            ImGui.end();
            return;
        }

        if (ImGui.beginTabBar("MSFTabs")) {
            if (ImGui.beginTabItem("Scripts")) {
                renderFeatureList(FeatureCategory.SCRIPT);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("QOL")) {
                renderFeatureList(FeatureCategory.QOL);
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Settings")) {
                renderSettings();
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }

        ImGui.end();
    }

    private void renderFeatureList(FeatureCategory category) {
        FeatureManager fm = FeatureManager.getInstance();
        for (Feature feature : fm.getByCategory(category)) {
            ImBoolean enabled = new ImBoolean(feature.isEnabled());
            if (ImGui.checkbox(feature.getName(), enabled)) {
                if (enabled.get()) {
                    feature.setEnabled(true);
                    feature.onEnable();
                    MacroConfig.featureStates.put(feature.getName(), true);
                } else {
                    feature.setEnabled(false);
                    feature.onDisable();
                    MacroConfig.featureStates.put(feature.getName(), false);
                }
            }
            ImGui.sameLine();
            ImGui.textDisabled(feature.getDescription());

            ImGui.indent();
            feature.renderSettings();
            ImGui.unindent();

            ImGui.spacing();
        }
    }

    private void renderSettings() {
        // Combat
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Combat");

        ImBoolean useHyperion = new ImBoolean(MacroConfig.useHyperion);
        if (ImGui.checkbox("Use Hyperion", useHyperion)) {
            MacroConfig.useHyperion = useHyperion.get();
        }

        int[] killTimeout = {MacroConfig.killTimeoutMs};
        if (ImGui.sliderInt("Kill Timeout (ms)", killTimeout, 1000, 30000)) {
            MacroConfig.killTimeoutMs = killTimeout[0];
        }

        int[] hyperionRetry = {MacroConfig.hyperionRetryDelayMs};
        if (ImGui.sliderInt("Hyperion Retry Delay (ms)", hyperionRetry, 50, 1000)) {
            MacroConfig.hyperionRetryDelayMs = hyperionRetry[0];
        }

        int[] hyperionAttempts = {MacroConfig.hyperionMaxAttempts};
        if (ImGui.sliderInt("Hyperion Max Attempts", hyperionAttempts, 1, 20)) {
            MacroConfig.hyperionMaxAttempts = hyperionAttempts[0];
        }

        int[] meleeCpsMin = {MacroConfig.meleeCpsMin};
        if (ImGui.sliderInt("Melee CPS Min", meleeCpsMin, 1, 20)) {
            MacroConfig.meleeCpsMin = meleeCpsMin[0];
        }

        int[] meleeCpsMax = {MacroConfig.meleeCpsMax};
        if (ImGui.sliderInt("Melee CPS Max", meleeCpsMax, 1, 20)) {
            MacroConfig.meleeCpsMax = meleeCpsMax[0];
        }

        // Fishing Delays
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Fishing Delays");

        int[] reelDelay = {MacroConfig.reelDelayMs};
        if (ImGui.sliderInt("Reel Delay (ms)", reelDelay, 50, 1000)) {
            MacroConfig.reelDelayMs = reelDelay[0];
        }

        int[] castDelay = {MacroConfig.castDelayMs};
        if (ImGui.sliderInt("Cast Delay (ms)", castDelay, 50, 1000)) {
            MacroConfig.castDelayMs = castDelay[0];
        }

        int[] postReelDelay = {MacroConfig.postReelDelayMs};
        if (ImGui.sliderInt("Post-Reel Delay (ms)", postReelDelay, 50, 2000)) {
            MacroConfig.postReelDelayMs = postReelDelay[0];
        }

        // Anti-AFK
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Anti-AFK");

        int[] antiAfkInterval = {MacroConfig.antiAfkIntervalMs};
        if (ImGui.sliderInt("Anti-AFK Interval (ms)", antiAfkInterval, 5000, 60000)) {
            MacroConfig.antiAfkIntervalMs = antiAfkInterval[0];
        }

        float[] maxYawDrift = {MacroConfig.antiAfkMaxYawDrift};
        if (ImGui.sliderFloat("Max Yaw Drift", maxYawDrift, 0.5f, 15.0f)) {
            MacroConfig.antiAfkMaxYawDrift = maxYawDrift[0];
        }

        float[] maxPitchDrift = {MacroConfig.antiAfkMaxPitchDrift};
        if (ImGui.sliderFloat("Max Pitch Drift", maxPitchDrift, 0.5f, 10.0f)) {
            MacroConfig.antiAfkMaxPitchDrift = maxPitchDrift[0];
        }

        // Knockback
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Knockback");

        float[] kbThreshold = {(float) MacroConfig.knockbackThreshold};
        if (ImGui.sliderFloat("Knockback Threshold", kbThreshold, 1.0f, 15.0f)) {
            MacroConfig.knockbackThreshold = kbThreshold[0];
        }

        int[] kbReaction = {MacroConfig.knockbackReactionMs};
        if (ImGui.sliderInt("KB Reaction (ms)", kbReaction, 50, 1000)) {
            MacroConfig.knockbackReactionMs = kbReaction[0];
        }

        // Return
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Return");

        boolean baritoneInstalled = ReturnHandler.isBaritoneAvailable();
        ImBoolean useBaritone = new ImBoolean(MacroConfig.useBaritone);
        if (baritoneInstalled) {
            if (ImGui.checkbox("Use Baritone", useBaritone)) {
                MacroConfig.useBaritone = useBaritone.get();
            }
        } else {
            ImGui.textDisabled("Use Baritone (not installed)");
        }

        int[] returnTimeout = {MacroConfig.returnTimeoutMs};
        if (ImGui.sliderInt("Return Timeout (ms)", returnTimeout, 5000, 60000)) {
            MacroConfig.returnTimeoutMs = returnTimeout[0];
        }

        int[] stuckThreshold = {MacroConfig.returnStuckThresholdTicks};
        if (ImGui.sliderInt("Stuck Threshold Ticks", stuckThreshold, 5, 60)) {
            MacroConfig.returnStuckThresholdTicks = stuckThreshold[0];
        }

        int[] maxStuck = {MacroConfig.returnMaxStuckAttempts};
        if (ImGui.sliderInt("Max Stuck Attempts", maxStuck, 1, 20)) {
            MacroConfig.returnMaxStuckAttempts = maxStuck[0];
        }

        // Detection
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Detection");

        float[] detectionRadius = {(float) MacroConfig.seaCreatureDetectionRadius};
        if (ImGui.sliderFloat("Detection Radius", detectionRadius, 3.0f, 30.0f)) {
            MacroConfig.seaCreatureDetectionRadius = detectionRadius[0];
        }

        int[] rotationBase = {MacroConfig.rotationBaseTimeMs};
        if (ImGui.sliderInt("Rotation Base Time (ms)", rotationBase, 100, 1500)) {
            MacroConfig.rotationBaseTimeMs = rotationBase[0];
        }

        // Failsafe
        ImGui.separator();
        ImGui.textColored(0.33f, 1.0f, 1.0f, 1.0f, "Failsafe");

        ImBoolean failsafe = new ImBoolean(MacroConfig.failsafeEnabled);
        if (ImGui.checkbox("Failsafe Enabled", failsafe)) {
            MacroConfig.failsafeEnabled = failsafe.get();
        }

        float[] tpThreshold = {(float) MacroConfig.failsafeTeleportThreshold};
        if (ImGui.sliderFloat("Teleport Threshold", tpThreshold, 2.0f, 20.0f)) {
            MacroConfig.failsafeTeleportThreshold = tpThreshold[0];
        }

        float[] rotThreshold = {MacroConfig.failsafeRotationThreshold};
        if (ImGui.sliderFloat("Rotation Threshold", rotThreshold, 5.0f, 90.0f)) {
            MacroConfig.failsafeRotationThreshold = rotThreshold[0];
        }

        // Save button
        ImGui.spacing();
        ImGui.separator();
        if (ImGui.button("Save Config")) {
            MacroConfig.save();
        }
    }
}
