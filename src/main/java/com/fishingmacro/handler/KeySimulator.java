package com.fishingmacro.handler;

import com.fishingmacro.mixin.KeyBindingAccessor;
import com.fishingmacro.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class KeySimulator {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void pressKey(KeyBinding key) {
        KeyBindingAccessor accessor = (KeyBindingAccessor) key;
        accessor.setPressed(true);
        accessor.setTimesPressed(accessor.getTimesPressed() + 1);
    }

    public static void releaseKey(KeyBinding key) {
        ((KeyBindingAccessor) key).setPressed(false);
    }

    public static void clickKey(KeyBinding key) {
        KeyBindingAccessor accessor = (KeyBindingAccessor) key;
        accessor.setTimesPressed(accessor.getTimesPressed() + 1);
    }

    public static void rightClick() {
        if (mc.player == null || mc.world == null) return;
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
    }

    public static void leftClick() {
        if (mc.player == null || mc.world == null) return;
        ((MinecraftClientAccessor) mc).invokeDoAttack();
    }

    public static void pressHotbar(int slot) {
        if (mc.player == null) return;
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().selectedSlot = slot;
    }

    public static void releaseAllKeys() {
        if (mc.options == null) return;
        releaseKey(mc.options.useKey);
        releaseKey(mc.options.attackKey);
        releaseKey(mc.options.forwardKey);
        releaseKey(mc.options.backKey);
        releaseKey(mc.options.leftKey);
        releaseKey(mc.options.rightKey);
        releaseKey(mc.options.jumpKey);
        releaseKey(mc.options.sneakKey);
        releaseKey(mc.options.sprintKey);
    }
}
