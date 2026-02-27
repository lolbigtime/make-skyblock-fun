package com.fishingmacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MacroConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("fishingmacro.json");

    // Combat
    public static boolean useHyperion = true;
    public static int killTimeoutMs = 10000;
    public static int hyperionRetryDelayMs = 300;
    public static int hyperionMaxAttempts = 7;
    public static int meleeCpsMin = 8;
    public static int meleeCpsMax = 12;

    // Fishing delays (single base values, ±30% applied via humanize())
    public static int reelDelayMs = 275;
    public static int castDelayMs = 250;
    public static int postReelDelayMs = 450;

    // Anti-AFK
    public static int antiAfkIntervalMs = 22500;
    public static float antiAfkMaxYawDrift = 3.0f;
    public static float antiAfkMaxPitchDrift = 1.5f;

    // Knockback recovery
    public static double knockbackThreshold = 3.0;
    public static int knockbackReactionMs = 400;

    // Return pathfinding
    public static boolean useBaritone = true;
    public static int returnTimeoutMs = 15000;
    public static int returnStuckThresholdTicks = 15;
    public static int returnMaxStuckAttempts = 6;

    // Sea creature detection
    public static double seaCreatureDetectionRadius = 10.0;

    // Rotation
    public static int rotationBaseTimeMs = 400;

    // Failsafe
    public static boolean failsafeEnabled = true;
    public static double failsafeTeleportThreshold = 4.0;
    public static float failsafeRotationThreshold = 15.0f;

    /**
     * Computes a ±30% humanized range from a base delay value.
     * Returns {min, max} as longs for use with MathUtil.randomBetween().
     */
    public static long[] humanize(int baseMs) {
        return new long[]{(long) (baseMs * 0.7), (long) (baseMs * 1.3)};
    }

    private static class ConfigData {
        boolean useHyperion = MacroConfig.useHyperion;
        int killTimeoutMs = MacroConfig.killTimeoutMs;
        int hyperionRetryDelayMs = MacroConfig.hyperionRetryDelayMs;
        int hyperionMaxAttempts = MacroConfig.hyperionMaxAttempts;
        int meleeCpsMin = MacroConfig.meleeCpsMin;
        int meleeCpsMax = MacroConfig.meleeCpsMax;

        // New single-value delay fields
        int reelDelayMs = MacroConfig.reelDelayMs;
        int castDelayMs = MacroConfig.castDelayMs;
        int postReelDelayMs = MacroConfig.postReelDelayMs;
        int knockbackReactionMs = MacroConfig.knockbackReactionMs;
        int antiAfkIntervalMs = MacroConfig.antiAfkIntervalMs;

        // Old min/max fields for backward compat (read-only, -1 = not present)
        int reelDelayMinMs = -1;
        int reelDelayMaxMs = -1;
        int castDelayMinMs = -1;
        int castDelayMaxMs = -1;
        int postReelDelayMinMs = -1;
        int postReelDelayMaxMs = -1;
        int knockbackReactionMinMs = -1;
        int knockbackReactionMaxMs = -1;
        int antiAfkMinIntervalMs = -1;
        int antiAfkMaxIntervalMs = -1;

        float antiAfkMaxYawDrift = MacroConfig.antiAfkMaxYawDrift;
        float antiAfkMaxPitchDrift = MacroConfig.antiAfkMaxPitchDrift;
        double knockbackThreshold = MacroConfig.knockbackThreshold;
        boolean useBaritone = MacroConfig.useBaritone;
        int returnTimeoutMs = MacroConfig.returnTimeoutMs;
        int returnStuckThresholdTicks = MacroConfig.returnStuckThresholdTicks;
        int returnMaxStuckAttempts = MacroConfig.returnMaxStuckAttempts;
        double seaCreatureDetectionRadius = MacroConfig.seaCreatureDetectionRadius;
        int rotationBaseTimeMs = MacroConfig.rotationBaseTimeMs;
        boolean failsafeEnabled = MacroConfig.failsafeEnabled;
        double failsafeTeleportThreshold = MacroConfig.failsafeTeleportThreshold;
        float failsafeRotationThreshold = MacroConfig.failsafeRotationThreshold;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            String json = Files.readString(CONFIG_PATH);
            ConfigData data = GSON.fromJson(json, ConfigData.class);
            if (data == null) return;
            useHyperion = data.useHyperion;
            killTimeoutMs = data.killTimeoutMs;
            hyperionRetryDelayMs = data.hyperionRetryDelayMs;
            hyperionMaxAttempts = data.hyperionMaxAttempts;
            meleeCpsMin = data.meleeCpsMin;
            meleeCpsMax = data.meleeCpsMax;

            // Backward compat: migrate old min/max pairs to single values
            if (data.reelDelayMinMs >= 0 && data.reelDelayMaxMs >= 0) {
                reelDelayMs = (data.reelDelayMinMs + data.reelDelayMaxMs) / 2;
            } else {
                reelDelayMs = data.reelDelayMs;
            }
            if (data.castDelayMinMs >= 0 && data.castDelayMaxMs >= 0) {
                castDelayMs = (data.castDelayMinMs + data.castDelayMaxMs) / 2;
            } else {
                castDelayMs = data.castDelayMs;
            }
            if (data.postReelDelayMinMs >= 0 && data.postReelDelayMaxMs >= 0) {
                postReelDelayMs = (data.postReelDelayMinMs + data.postReelDelayMaxMs) / 2;
            } else {
                postReelDelayMs = data.postReelDelayMs;
            }
            if (data.knockbackReactionMinMs >= 0 && data.knockbackReactionMaxMs >= 0) {
                knockbackReactionMs = (data.knockbackReactionMinMs + data.knockbackReactionMaxMs) / 2;
            } else {
                knockbackReactionMs = data.knockbackReactionMs;
            }
            if (data.antiAfkMinIntervalMs >= 0 && data.antiAfkMaxIntervalMs >= 0) {
                antiAfkIntervalMs = (data.antiAfkMinIntervalMs + data.antiAfkMaxIntervalMs) / 2;
            } else {
                antiAfkIntervalMs = data.antiAfkIntervalMs;
            }

            antiAfkMaxYawDrift = data.antiAfkMaxYawDrift;
            antiAfkMaxPitchDrift = data.antiAfkMaxPitchDrift;
            knockbackThreshold = data.knockbackThreshold;
            useBaritone = data.useBaritone;
            returnTimeoutMs = data.returnTimeoutMs;
            returnStuckThresholdTicks = data.returnStuckThresholdTicks;
            returnMaxStuckAttempts = data.returnMaxStuckAttempts;
            seaCreatureDetectionRadius = data.seaCreatureDetectionRadius;
            rotationBaseTimeMs = data.rotationBaseTimeMs;
            failsafeEnabled = data.failsafeEnabled;
            failsafeTeleportThreshold = data.failsafeTeleportThreshold;
            failsafeRotationThreshold = data.failsafeRotationThreshold;
        } catch (IOException e) {
            System.err.println("[FishingMacro] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.useHyperion = useHyperion;
        data.killTimeoutMs = killTimeoutMs;
        data.hyperionRetryDelayMs = hyperionRetryDelayMs;
        data.hyperionMaxAttempts = hyperionMaxAttempts;
        data.meleeCpsMin = meleeCpsMin;
        data.meleeCpsMax = meleeCpsMax;
        data.reelDelayMs = reelDelayMs;
        data.castDelayMs = castDelayMs;
        data.postReelDelayMs = postReelDelayMs;
        data.knockbackReactionMs = knockbackReactionMs;
        data.antiAfkIntervalMs = antiAfkIntervalMs;
        data.antiAfkMaxYawDrift = antiAfkMaxYawDrift;
        data.antiAfkMaxPitchDrift = antiAfkMaxPitchDrift;
        data.knockbackThreshold = knockbackThreshold;
        data.useBaritone = useBaritone;
        data.returnTimeoutMs = returnTimeoutMs;
        data.returnStuckThresholdTicks = returnStuckThresholdTicks;
        data.returnMaxStuckAttempts = returnMaxStuckAttempts;
        data.seaCreatureDetectionRadius = seaCreatureDetectionRadius;
        data.rotationBaseTimeMs = rotationBaseTimeMs;
        data.failsafeEnabled = failsafeEnabled;
        data.failsafeTeleportThreshold = failsafeTeleportThreshold;
        data.failsafeRotationThreshold = failsafeRotationThreshold;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[FishingMacro] Failed to save config: " + e.getMessage());
        }
    }
}
