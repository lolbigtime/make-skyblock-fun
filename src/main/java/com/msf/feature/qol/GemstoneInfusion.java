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

import java.util.function.Consumer;

public class GemstoneInfusion implements Feature {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String TRIGGER = "Gemstone Infusion is now available!";

    private enum State {
        IDLE, PENDING_USE, COOLDOWN
    }

    private boolean enabled = false;
    private State state = State.IDLE;
    private final Clock reactionTimer = new Clock();
    private final Clock cooldownTimer = new Clock();

    @Override
    public String getName() {
        return "Auto Gemstone Infusion";
    }

    @Override
    public String getDescription() {
        return "Auto-use Gemstone Infusion when it becomes available";
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

    public void onChatMessage(String message) {
        if (!enabled) return;
        if (state != State.IDLE) return;
        if (cooldownTimer.isScheduled() && !cooldownTimer.passed()) return;
        if (!message.contains(TRIGGER)) return;

        long[] delay = MacroConfig.humanize(MacroConfig.gemstoneInfusionReactionMs);
        reactionTimer.schedule(MathUtil.randomBetween(delay[0], delay[1]));
        state = State.PENDING_USE;
        System.out.println("[MSF] Gemstone Infusion available - queued use");
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case PENDING_USE -> handlePendingUse();
            case COOLDOWN -> handleCooldown();
            default -> {}
        }
    }

    private void handlePendingUse() {
        if (!reactionTimer.passed()) return;
        if (mc.currentScreen != null) {
            // Wait until UI closes before firing
            return;
        }
        KeySimulator.rightClick();
        System.out.println("[MSF] Used Gemstone Infusion");
        long[] cd = MacroConfig.humanize(MacroConfig.gemstoneInfusionCooldownMs);
        cooldownTimer.schedule(MathUtil.randomBetween(cd[0], cd[1]));
        state = State.COOLDOWN;
    }

    private void handleCooldown() {
        if (cooldownTimer.isScheduled() && !cooldownTimer.passed()) return;
        state = State.IDLE;
    }

    @Override
    public void addSettingsWidgets(Consumer<ClickableWidget> adder) {
        adder.accept(ConfigSliderWidget.forInt("Reaction Delay (ms)", 50, 1000,
                MacroConfig.gemstoneInfusionReactionMs,
                v -> MacroConfig.gemstoneInfusionReactionMs = v));
        adder.accept(ConfigSliderWidget.forInt("Cooldown (ms)", 500, 10000,
                MacroConfig.gemstoneInfusionCooldownMs,
                v -> MacroConfig.gemstoneInfusionCooldownMs = v));
    }
}
