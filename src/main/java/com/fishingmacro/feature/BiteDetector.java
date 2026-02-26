package com.fishingmacro.feature;

import com.fishingmacro.util.Clock;
import com.fishingmacro.util.EntityScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class BiteDetector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final double SCAN_RADIUS = 10.0;
    private final Clock cooldown = new Clock();

    public boolean hasBite() {
        if (mc.player == null || mc.world == null) return false;
        if (cooldown.isScheduled() && !cooldown.passed()) return false;

        // Determine scan center: prefer fish hook position, fall back to player
        Vec3d scanCenter = mc.player.getEntityPos();
        if (mc.player.fishHook != null) {
            scanCenter = mc.player.fishHook.getEntityPos();
        }

        // Check armor stand entities (original detection method)
        if (checkArmorStands(scanCenter)) {
            cooldown.schedule(500);
            return true;
        }

        // Check text display entities (1.20+ Hypixel may use these)
        if (checkTextDisplays(scanCenter)) {
            cooldown.schedule(500);
            return true;
        }

        // Also scan around player position if hook is far away
        if (mc.player.fishHook != null) {
            Vec3d playerPos = mc.player.getEntityPos();
            if (playerPos.squaredDistanceTo(scanCenter) > 4.0) {
                if (checkArmorStands(playerPos) || checkTextDisplays(playerPos)) {
                    cooldown.schedule(500);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkArmorStands(Vec3d center) {
        List<ArmorStandEntity> armorStands = EntityScanner.getEntitiesWithinRadius(
                center, SCAN_RADIUS, ArmorStandEntity.class
        );

        for (ArmorStandEntity stand : armorStands) {
            Text customName = stand.getCustomName();
            if (customName == null) continue;

            String name = Formatting.strip(customName.getString());
            if (name != null && name.trim().equals("!!!")) {
                return true;
            }
        }
        return false;
    }

    private boolean checkTextDisplays(Vec3d center) {
        List<DisplayEntity.TextDisplayEntity> textDisplays = EntityScanner.getEntitiesWithinRadius(
                center, SCAN_RADIUS, DisplayEntity.TextDisplayEntity.class
        );

        for (DisplayEntity.TextDisplayEntity display : textDisplays) {
            // Check custom name
            Text customName = display.getCustomName();
            if (customName != null) {
                String name = Formatting.strip(customName.getString());
                if (name != null && name.trim().equals("!!!")) {
                    return true;
                }
            }

            // Check display name (entity name)
            String displayName = Formatting.strip(display.getName().getString());
            if (displayName != null && displayName.trim().equals("!!!")) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        cooldown.reset();
    }
}
