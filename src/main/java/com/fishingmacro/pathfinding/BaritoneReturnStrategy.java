package com.fishingmacro.pathfinding;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Isolates all Baritone API calls behind reflection so no compile-time dependency is needed.
 * This class is only instantiated when Baritone is confirmed present at runtime.
 */
public class BaritoneReturnStrategy {
    private final Object baritone;
    private final Object settings;

    public BaritoneReturnStrategy() throws Exception {
        Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
        Object provider = apiClass.getMethod("getProvider").invoke(null);
        this.baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
        this.settings = apiClass.getMethod("getSettings").invoke(null);
    }

    private void setSetting(String name, Object value) {
        try {
            Field field = settings.getClass().getField(name);
            Object setting = field.get(settings);
            Field valueField = setting.getClass().getField("value");
            valueField.set(setting, value);
        } catch (Exception e) {
            System.err.println("[FishingMacro] Failed to set Baritone setting " + name + ": " + e.getMessage());
        }
    }

    private void configureSettings() {
        setSetting("allowSprint", true);
        setSetting("allowBreak", false);
        setSetting("allowPlace", false);
        setSetting("allowParkour", false);
    }

    public void startPathfinding(double x, double y, double z) {
        try {
            configureSettings();

            // Create GoalNear(int x, int y, int z, int range)
            Class<?> goalNearClass = Class.forName("baritone.api.pathing.goals.GoalNear");
            Constructor<?> goalCtor = goalNearClass.getConstructor(
                    int.class, int.class, int.class, int.class);
            Object goal = goalCtor.newInstance((int) x, (int) y, (int) z, 1);

            // Set goal and path via ICustomGoalProcess
            Object goalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            Class<?> goalInterface = Class.forName("baritone.api.pathing.goals.Goal");
            Method setGoalAndPath = goalProcess.getClass().getMethod("setGoalAndPath", goalInterface);
            setGoalAndPath.invoke(goalProcess, goal);

            System.out.println("[FishingMacro] Baritone pathfinding started to " +
                    (int) x + ", " + (int) y + ", " + (int) z);
        } catch (Exception e) {
            System.err.println("[FishingMacro] Baritone startPathfinding failed: " + e.getMessage());
            throw new RuntimeException("Baritone pathfinding failed", e);
        }
    }

    public void cancel() {
        try {
            Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
        } catch (Exception e) {
            System.err.println("[FishingMacro] Baritone cancel failed: " + e.getMessage());
        }
    }

    public boolean isPathing() {
        try {
            Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            return (boolean) pathingBehavior.getClass().getMethod("isPathing").invoke(pathingBehavior);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasFinished() {
        return !isPathing();
    }
}
