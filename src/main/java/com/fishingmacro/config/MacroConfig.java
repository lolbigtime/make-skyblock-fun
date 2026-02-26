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

    // Hotbar slots (0-indexed)
    public static int weaponSlot = 0;
    public static int rodSlot = 1;

    // Combat
    public static boolean useHyperion = true;
    public static int killTimeoutMs = 10000;
    public static int hyperionRetryDelayMs = 500;
    public static int hyperionMaxAttempts = 3;
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

    // Sea creature detection
    public static double seaCreatureDetectionRadius = 10.0;

    // Rotation
    public static int rotationBaseTimeMs = 400;

    private static class ConfigData {
        int weaponSlot = MacroConfig.weaponSlot;
        int rodSlot = MacroConfig.rodSlot;
        boolean useHyperion = MacroConfig.useHyperion;
        int killTimeoutMs = MacroConfig.killTimeoutMs;
        int reelDelayMinMs = MacroConfig.reelDelayMinMs;
        int reelDelayMaxMs = MacroConfig.reelDelayMaxMs;
        int castDelayMinMs = MacroConfig.castDelayMinMs;
        int castDelayMaxMs = MacroConfig.castDelayMaxMs;
        int postReelDelayMinMs = MacroConfig.postReelDelayMinMs;
        int postReelDelayMaxMs = MacroConfig.postReelDelayMaxMs;
        int antiAfkMinIntervalMs = MacroConfig.antiAfkMinIntervalMs;
        int antiAfkMaxIntervalMs = MacroConfig.antiAfkMaxIntervalMs;
        double knockbackThreshold = MacroConfig.knockbackThreshold;
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
            weaponSlot = data.weaponSlot;
            rodSlot = data.rodSlot;
            useHyperion = data.useHyperion;
            killTimeoutMs = data.killTimeoutMs;
            reelDelayMinMs = data.reelDelayMinMs;
            reelDelayMaxMs = data.reelDelayMaxMs;
            castDelayMinMs = data.castDelayMinMs;
            castDelayMaxMs = data.castDelayMaxMs;
            postReelDelayMinMs = data.postReelDelayMinMs;
            postReelDelayMaxMs = data.postReelDelayMaxMs;
            antiAfkMinIntervalMs = data.antiAfkMinIntervalMs;
            antiAfkMaxIntervalMs = data.antiAfkMaxIntervalMs;
            knockbackThreshold = data.knockbackThreshold;
            seaCreatureDetectionRadius = data.seaCreatureDetectionRadius;
            rotationBaseTimeMs = data.rotationBaseTimeMs;
        } catch (IOException e) {
            System.err.println("[FishingMacro] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.weaponSlot = weaponSlot;
        data.rodSlot = rodSlot;
        data.useHyperion = useHyperion;
        data.killTimeoutMs = killTimeoutMs;
        data.reelDelayMinMs = reelDelayMinMs;
        data.reelDelayMaxMs = reelDelayMaxMs;
        data.castDelayMinMs = castDelayMinMs;
        data.castDelayMaxMs = castDelayMaxMs;
        data.postReelDelayMinMs = postReelDelayMinMs;
        data.postReelDelayMaxMs = postReelDelayMaxMs;
        data.antiAfkMinIntervalMs = antiAfkMinIntervalMs;
        data.antiAfkMaxIntervalMs = antiAfkMaxIntervalMs;
        data.knockbackThreshold = knockbackThreshold;
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
