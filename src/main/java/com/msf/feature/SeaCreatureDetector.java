package com.msf.feature;

import com.msf.config.MacroConfig;
import com.msf.util.EntityScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SeaCreatureDetector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final long BLACKLIST_DURATION_MS = 30_000;
    private static final double BOBBER_PROXIMITY_RADIUS = 8.0;
    private static final double BOBBER_PROXIMITY_RADIUS_SQ = BOBBER_PROXIMITY_RADIUS * BOBBER_PROXIMITY_RADIUS;

    private final Map<Integer, Long> blacklistedEntities = new HashMap<>();

    public void blacklistEntity(int entityId) {
        blacklistedEntities.put(entityId, System.currentTimeMillis() + BLACKLIST_DURATION_MS);
    }

    public Optional<LivingEntity> detectSeaCreature(Vec3d bobberPos) {
        if (mc.player == null || mc.world == null) return Optional.empty();
        if (bobberPos == null) return Optional.empty();

        List<LivingEntity> entities = EntityScanner.getEntitiesWithinRadius(
                mc.player.getEntityPos(),
                MacroConfig.seaCreatureDetectionRadius,
                LivingEntity.class,
                e -> isSeaCreature(e) && e.squaredDistanceTo(bobberPos) <= BOBBER_PROXIMITY_RADIUS_SQ
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
        if (entity == mc.player) return false;
        if (entity instanceof PlayerEntity) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity.getType() == EntityType.SQUID || entity.getType() == EntityType.GLOW_SQUID) return false;
        if (entity.isRemoved()) return false;

        // Clean up expired blacklist entries
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
