package com.fishingmacro.feature;

import com.fishingmacro.util.Clock;
import com.fishingmacro.util.EntityScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BiteDetector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Clock cooldown = new Clock();

    public boolean hasBite() {
        if (mc.player == null || mc.world == null) return false;
        if (cooldown.isScheduled() && !cooldown.passed()) return false;

        List<ArmorStandEntity> armorStands = EntityScanner.getEntitiesWithinRadius(
                mc.player.getEntityPos(), 5.0, ArmorStandEntity.class
        );

        for (ArmorStandEntity stand : armorStands) {
            Text customName = stand.getCustomName();
            if (customName == null) continue;

            String name = Formatting.strip(customName.getString());
            if (name != null && name.trim().equals("!!!")) {
                cooldown.schedule(500);
                return true;
            }
        }

        return false;
    }

    public void reset() {
        cooldown.reset();
    }
}
