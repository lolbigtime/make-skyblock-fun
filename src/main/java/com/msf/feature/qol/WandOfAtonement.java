package com.msf.feature.qol;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.gui.ConfigSliderWidget;
import com.msf.handler.KeySimulator;
import com.msf.util.Clock;
import com.msf.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class WandOfAtonement implements Feature {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String WAND_NAME = "Wand of Atonement";

    private enum State {
        IDLE, SWAPPING_TO_WAND, WAITING_SWAP, USING_WAND, WAITING_USE, SWAPPING_BACK, WAITING_SWAP_BACK, COOLDOWN
    }

    private boolean enabled = false;
    private State state = State.IDLE;
    private int previousSlot = -1;
    private int wandSlot = -1;
    private final Clock stateTimer = new Clock();
    private final Clock cooldownTimer = new Clock();

    @Override
    public String getName() {
        return "Auto Wand of Atonement";
    }

    @Override
    public String getDescription() {
        return "Auto-use Wand of Atonement when health drops";
    }

    @Override
    public FeatureCategory getCategory() {
        return FeatureCategory.QOL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            state = State.IDLE;
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        state = State.IDLE;
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case IDLE -> checkHealth();
            case SWAPPING_TO_WAND -> handleSwapToWand();
            case WAITING_SWAP -> handleWaitingSwap();
            case USING_WAND -> handleUsingWand();
            case WAITING_USE -> handleWaitingUse();
            case SWAPPING_BACK -> handleSwapBack();
            case WAITING_SWAP_BACK -> handleWaitingSwapBack();
            case COOLDOWN -> handleCooldown();
        }
    }

    private void checkHealth() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (cooldownTimer.isScheduled() && !cooldownTimer.passed()) return;

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        if (maxHealth <= 0) return;

        float healthPct = (health / maxHealth) * 100f;
        if (healthPct <= MacroConfig.wandHealthThreshold) {
            state = State.SWAPPING_TO_WAND;
        }
    }

    private void handleSwapToWand() {
        if (mc.player == null) {
            state = State.IDLE;
            return;
        }

        wandSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String name = Formatting.strip(stack.getName().getString());
            if (name != null && name.contains(WAND_NAME)) {
                wandSlot = i;
                break;
            }
        }

        if (wandSlot == -1) {
            System.out.println("[MSF] Could not find " + WAND_NAME + " in hotbar");
            long[] cd = MacroConfig.humanize(MacroConfig.wandCooldownMs);
            cooldownTimer.schedule(MathUtil.randomBetween(cd[0], cd[1]));
            state = State.IDLE;
            return;
        }

        previousSlot = mc.player.getInventory().getSelectedSlot();

        if (previousSlot == wandSlot) {
            // Already holding wand, skip swap
            state = State.USING_WAND;
            return;
        }

        KeySimulator.pressHotbar(wandSlot);
        long[] delay = MacroConfig.humanize(MacroConfig.wandSwapDelayMs);
        stateTimer.schedule(MathUtil.randomBetween(delay[0], delay[1]));
        state = State.WAITING_SWAP;
        System.out.println("[MSF] Swapping to Wand of Atonement (slot " + wandSlot + ")");
    }

    private void handleWaitingSwap() {
        if (!stateTimer.passed()) return;
        state = State.USING_WAND;
    }

    private void handleUsingWand() {
        KeySimulator.rightClick();
        long[] delay = MacroConfig.humanize(MacroConfig.wandUseDelayMs);
        stateTimer.schedule(MathUtil.randomBetween(delay[0], delay[1]));
        state = State.WAITING_USE;
        System.out.println("[MSF] Using Wand of Atonement");
    }

    private void handleWaitingUse() {
        if (!stateTimer.passed()) return;
        state = State.SWAPPING_BACK;
    }

    private void handleSwapBack() {
        if (mc.player == null || previousSlot == wandSlot) {
            // Was already holding wand, no need to swap back
            state = State.COOLDOWN;
            return;
        }

        KeySimulator.pressHotbar(previousSlot);
        long[] delay = MacroConfig.humanize(MacroConfig.wandSwapDelayMs);
        stateTimer.schedule(MathUtil.randomBetween(delay[0], delay[1]));
        state = State.WAITING_SWAP_BACK;
        System.out.println("[MSF] Swapping back to slot " + previousSlot);
    }

    private void handleWaitingSwapBack() {
        if (!stateTimer.passed()) return;
        state = State.COOLDOWN;
    }

    private void handleCooldown() {
        long[] cd = MacroConfig.humanize(MacroConfig.wandCooldownMs);
        cooldownTimer.schedule(MathUtil.randomBetween(cd[0], cd[1]));
        state = State.IDLE;
    }

    @Override
    public void addSettingsWidgets(Consumer<ClickableWidget> adder) {
        adder.accept(ConfigSliderWidget.forInt("Health Threshold (%)", 10, 90, MacroConfig.wandHealthThreshold,
                v -> MacroConfig.wandHealthThreshold = v));
        adder.accept(ConfigSliderWidget.forInt("Cooldown (ms)", 1000, 15000, MacroConfig.wandCooldownMs,
                v -> MacroConfig.wandCooldownMs = v));
        adder.accept(ConfigSliderWidget.forInt("Swap Delay (ms)", 50, 500, MacroConfig.wandSwapDelayMs,
                v -> MacroConfig.wandSwapDelayMs = v));
        adder.accept(ConfigSliderWidget.forInt("Use Delay (ms)", 50, 500, MacroConfig.wandUseDelayMs,
                v -> MacroConfig.wandUseDelayMs = v));
    }
}
