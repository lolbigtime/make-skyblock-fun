package com.fishingmacro.feature;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.util.EntityScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.Optional;

public class SeaCreatureDetector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public Optional<LivingEntity> detectSeaCreature() {
        if (mc.player == null || mc.world == null) return Optional.empty();

        List<LivingEntity> entities = EntityScanner.getEntitiesWithinRadius(
                mc.player.getPos(),
                MacroConfig.seaCreatureDetectionRadius,
                LivingEntity.class,
                this::isSeaCreature
        );

        // Return the nearest qualifying entity
        return entities.stream()
                .min((a, b) -> {
                    double distA = a.squaredDistanceTo(mc.player);
                    double distB = b.squaredDistanceTo(mc.player);
                    return Double.compare(distA, distB);
                });
    }

    private boolean isSeaCreature(LivingEntity entity) {
        // Exclude ourselves
        if (entity == mc.player) return false;

        // Exclude other players
        if (entity instanceof PlayerEntity) return false;

        // Exclude armor stands (used for nametags)
        if (entity instanceof ArmorStandEntity) return false;

        // Exclude squids
        if (entity.getType() == EntityType.SQUID || entity.getType() == EntityType.GLOW_SQUID) return false;

        // Must be alive
        if (entity.isDead()) return false;

        return true;
    }
}
