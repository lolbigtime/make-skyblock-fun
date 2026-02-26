package com.fishingmacro.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class EntityScanner {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static <T extends Entity> List<T> getEntitiesWithinRadius(
            Vec3d center, double radius, Class<T> type) {
        if (mc.world == null) return Collections.emptyList();

        Box box = new Box(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        return mc.world.getEntitiesByClass(type, box,
                entity -> entity.squaredDistanceTo(center) <= radius * radius);
    }

    public static <T extends Entity> List<T> getEntitiesWithinRadius(
            Vec3d center, double radius, Class<T> type, Predicate<T> filter) {
        if (mc.world == null) return Collections.emptyList();

        Box box = new Box(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        return mc.world.getEntitiesByClass(type, box,
                entity -> entity.squaredDistanceTo(center) <= radius * radius && filter.test(entity));
    }
}
