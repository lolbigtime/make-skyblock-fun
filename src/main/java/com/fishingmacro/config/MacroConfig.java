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

    // Fishing delays
    public static int reelDelayMinMs = 150;
    public static int reelDelayMaxMs = 400;
    public static int castDelayMinMs = 150;
    public static int castDelayMaxMs = 350;
    public static int postReelDelayMinMs = 300;
    public static int postReelDelayMaxMs = 600;

    // Anti-AFK
    public static int antiAfkMinIntervalMs = 15000;
    public static int antiAfkMaxIntervalMs = 30000;
    public static float antiAfkMaxYawDrift = 3.0f;
    public static float antiAfkMaxPitchDrift = 1.5f;

    // Knockback recovery
    public static double knockbackThreshold = 3.0;
    public static int knockbackReactionMinMs = 200;
    public static int knockbackReactionMaxMs = 600;

    // Return pathfinding
    public static int returnTimeoutMs = 15000;
    public static int returnStuckThresholdTicks = 15;
    public static int returnMaxStuckAttempts = 6;

    // Sea creature detection
    public static double seaCreatureDetectionRadius = 10.0;

    // Rotation
    public static int rotationBaseTimeMs = 400;

    private static class ConfigData {
        boolean useHyperion = MacroConfig.useHyperion;
        int killTimeoutMs = MacroConfig.killTimeoutMs;
        int hyperionRetryDelayMs = MacroConfig.hyperionRetryDelayMs;
        int hyperionMaxAttempts = MacroConfig.hyperionMaxAttempts;
        int reelDelayMinMs = MacroConfig.reelDelayMinMs;
        int reelDelayMaxMs = MacroConfig.reelDelayMaxMs;
        int castDelayMinMs = MacroConfig.castDelayMinMs;
        int castDelayMaxMs = MacroConfig.castDelayMaxMs;
        int postReelDelayMinMs = MacroConfig.postReelDelayMinMs;
        int postReelDelayMaxMs = MacroConfig.postReelDelayMaxMs;
        int antiAfkMinIntervalMs = MacroConfig.antiAfkMinIntervalMs;
        int antiAfkMaxIntervalMs = MacroConfig.antiAfkMaxIntervalMs;
        double knockbackThreshold = MacroConfig.knockbackThreshold;
        int returnTimeoutMs = MacroConfig.returnTimeoutMs;
        int returnStuckThresholdTicks = MacroConfig.returnStuckThresholdTicks;
        int returnMaxStuckAttempts = MacroConfig.returnMaxStuckAttempts;
        double seaCreatureDetectionRadius = MacroConfig.seaCreatureDetectionRadius;
        int rotationBaseTimeMs = MacroConfig.rotationBaseTimeMs;
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
            reelDelayMinMs = data.reelDelayMinMs;
            reelDelayMaxMs = data.reelDelayMaxMs;
            castDelayMinMs = data.castDelayMinMs;
            castDelayMaxMs = data.castDelayMaxMs;
            postReelDelayMinMs = data.postReelDelayMinMs;
            postReelDelayMaxMs = data.postReelDelayMaxMs;
            antiAfkMinIntervalMs = data.antiAfkMinIntervalMs;
            antiAfkMaxIntervalMs = data.antiAfkMaxIntervalMs;
            knockbackThreshold = data.knockbackThreshold;
            returnTimeoutMs = data.returnTimeoutMs;
            returnStuckThresholdTicks = data.returnStuckThresholdTicks;
            returnMaxStuckAttempts = data.returnMaxStuckAttempts;
            seaCreatureDetectionRadius = data.seaCreatureDetectionRadius;
            rotationBaseTimeMs = data.rotationBaseTimeMs;
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
        data.reelDelayMinMs = reelDelayMinMs;
        data.reelDelayMaxMs = reelDelayMaxMs;
        data.castDelayMinMs = castDelayMinMs;
        data.castDelayMaxMs = castDelayMaxMs;
        data.postReelDelayMinMs = postReelDelayMinMs;
        data.postReelDelayMaxMs = postReelDelayMaxMs;
        data.antiAfkMinIntervalMs = antiAfkMinIntervalMs;
        data.antiAfkMaxIntervalMs = antiAfkMaxIntervalMs;
        data.knockbackThreshold = knockbackThreshold;
        data.returnTimeoutMs = returnTimeoutMs;
        data.returnStuckThresholdTicks = returnStuckThresholdTicks;
        data.returnMaxStuckAttempts = returnMaxStuckAttempts;
        data.seaCreatureDetectionRadius = seaCreatureDetectionRadius;
        data.rotationBaseTimeMs = rotationBaseTimeMs;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[FishingMacro] Failed to save config: " + e.getMessage());
        }
    }
}
