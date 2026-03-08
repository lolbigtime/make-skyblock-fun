package com.msf.feature.qol;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.handler.KeySimulator;
import com.msf.util.Clock;
import com.msf.util.MathUtil;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaggerSwapper implements Feature {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Pattern ATTUNEMENT_PATTERN = Pattern.compile(
            "Strike using (\\w+) attunement on your dagger"
    );

    private enum State {
        IDLE, FINDING_DAGGER, SWAPPED_TO_DAGGER, CHECKING_MODE, WAITING_AFTER_TOGGLE, DONE
    }

    private enum Attunement {
        ASHEN("Pyrochaos Dagger", Items.STONE_SWORD),
        AURIC("Pyrochaos Dagger", Items.GOLDEN_SWORD),
        CRYSTAL("Deathripper Dagger", Items.DIAMOND_SWORD),
        SPIRIT("Deathripper Dagger", Items.IRON_SWORD);

        final String daggerName;
        final net.minecraft.item.Item expectedItem;

        Attunement(String daggerName, net.minecraft.item.Item expectedItem) {
            this.daggerName = daggerName;
            this.expectedItem = expectedItem;
        }
    }

    private boolean enabled = false;
    private State state = State.IDLE;
    private Attunement targetAttunement = null;
    private int daggerSlot = -1;
    private final Clock stateTimer = new Clock();

    @Override
    public String getName() {
        return "Dagger Swapper";
    }

    @Override
    public String getDescription() {
        return "Auto-swap dagger for blaze slayer attunements";
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
            targetAttunement = null;
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        state = State.IDLE;
        targetAttunement = null;
    }

    public void onChatMessage(String message) {
        if (!enabled || state != State.IDLE) return;

        Matcher matcher = ATTUNEMENT_PATTERN.matcher(message);
        if (matcher.find()) {
            String attunementName = matcher.group(1).toUpperCase();
            try {
                targetAttunement = Attunement.valueOf(attunementName);
                state = State.FINDING_DAGGER;
                System.out.println("[MSF] Dagger swap triggered: " + attunementName
                        + " -> " + targetAttunement.daggerName);
            } catch (IllegalArgumentException e) {
                System.out.println("[MSF] Unknown attunement: " + attunementName);
            }
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case IDLE -> {}
            case FINDING_DAGGER -> handleFindingDagger();
            case SWAPPED_TO_DAGGER -> handleSwappedToDagger();
            case CHECKING_MODE -> handleCheckingMode();
            case WAITING_AFTER_TOGGLE -> handleWaitingAfterToggle();
            case DONE -> {
                state = State.IDLE;
                targetAttunement = null;
            }
        }
    }

    private void handleFindingDagger() {
        if (mc.player == null || targetAttunement == null) {
            state = State.IDLE;
            return;
        }

        // Scan hotbar for the target dagger
        daggerSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String name = Formatting.strip(stack.getName().getString());
            if (name != null && name.contains(targetAttunement.daggerName)) {
                daggerSlot = i;
                break;
            }
        }

        if (daggerSlot == -1) {
            System.out.println("[MSF] Could not find " + targetAttunement.daggerName + " in hotbar");
            state = State.IDLE;
            targetAttunement = null;
            return;
        }

        KeySimulator.pressHotbar(daggerSlot);
        long[] delay = MacroConfig.humanize(MacroConfig.daggerSwapDelayMs);
        stateTimer.schedule(MathUtil.randomBetween(delay[0], delay[1]));
        state = State.SWAPPED_TO_DAGGER;
    }

    private void handleSwappedToDagger() {
        if (!stateTimer.passed()) return;
        state = State.CHECKING_MODE;
    }

    private void handleCheckingMode() {
        if (mc.player == null || targetAttunement == null) {
            state = State.IDLE;
            return;
        }

        ItemStack held = mc.player.getInventory().getStack(daggerSlot);
        if (held.getItem() == targetAttunement.expectedItem) {
            // Correct mode already
            System.out.println("[MSF] Dagger already in correct mode");
            state = State.DONE;
        } else {
            // Wrong mode, right-click to toggle
            KeySimulator.rightClick();
            long[] delay = MacroConfig.humanize(MacroConfig.daggerToggleDelayMs);
            stateTimer.schedule(MathUtil.randomBetween(delay[0], delay[1]));
            state = State.WAITING_AFTER_TOGGLE;
        }
    }

    private void handleWaitingAfterToggle() {
        if (!stateTimer.passed()) return;
        System.out.println("[MSF] Dagger mode toggled");
        state = State.DONE;
    }

    @Override
    public void renderSettings() {
        int[] swapDelay = {MacroConfig.daggerSwapDelayMs};
        if (ImGui.sliderInt("Swap Delay (ms)##dagger", swapDelay, 50, 500)) {
            MacroConfig.daggerSwapDelayMs = swapDelay[0];
        }

        int[] toggleDelay = {MacroConfig.daggerToggleDelayMs};
        if (ImGui.sliderInt("Toggle Delay (ms)##dagger", toggleDelay, 50, 500)) {
            MacroConfig.daggerToggleDelayMs = toggleDelay[0];
        }

        ImGui.spacing();
        ImGui.text("Attunement Mapping:");
        ImGui.bulletText("ASHEN -> Pyrochaos Dagger (Stone Sword)");
        ImGui.bulletText("AURIC -> Pyrochaos Dagger (Golden Sword)");
        ImGui.bulletText("CRYSTAL -> Deathripper Dagger (Diamond Sword)");
        ImGui.bulletText("SPIRIT -> Deathripper Dagger (Iron Sword)");

        if (state != State.IDLE) {
            ImGui.spacing();
            ImGui.textColored(0.0f, 1.0f, 0.5f, 1.0f, "Active: " + state.name());
            if (targetAttunement != null) {
                ImGui.text("Target: " + targetAttunement.name() + " -> " + targetAttunement.daggerName);
            }
        }
    }
}
