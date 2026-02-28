package com.fishingmacro.feature;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.util.EntityScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SeaCreatureDetector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final long BLACKLIST_DURATION_MS = 30_000;

    private final Map<Integer, Long> blacklistedEntities = new HashMap<>();

    public void blacklistEntity(int entityId) {
        blacklistedEntities.put(entityId, System.currentTimeMillis() + BLACKLIST_DURATION_MS);
    }

    public Optional<LivingEntity> detectSeaCreature() {
        if (mc.player == null || mc.world == null) return Optional.empty();

        List<LivingEntity> entities = EntityScanner.getEntitiesWithinRadius(
                mc.player.getEntityPos(),
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

        // Must exist in world (don't use isDead() - Hypixel mobs may have 0 client-side health)
        if (entity.isRemoved()) return false;

        // Skip blacklisted entities (failed kill attempts) and clean up expired entries
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = blacklistedEntities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            if (entry.getValue() < now) {
                it.remove();
            }
        }
        if (blacklistedEntities.containsKey(entity.getId())) return false;

        return true;
    }
}
