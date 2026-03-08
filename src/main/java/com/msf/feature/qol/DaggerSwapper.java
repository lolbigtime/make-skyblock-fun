package com.msf.feature.qol;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.gui.ConfigSliderWidget;
import com.msf.handler.KeySimulator;
import com.msf.util.Clock;
import com.msf.util.EntityScanner;
import com.msf.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaggerSwapper implements Feature {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Pattern ATTUNEMENT_PATTERN = Pattern.compile(
            "Strike using the (\\w+) attunement on your dagger"
    );
    private static final double NAMETAG_SCAN_RADIUS = 16.0;
    private static final int SCAN_INTERVAL_TICKS = 4;

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
    private Attunement lastCompletedAttunement = null;
    private int daggerSlot = -1;
    private int tickCounter = 0;
    private final Clock stateTimer = new Clock();
    private final Clock scanCooldown = new Clock();

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
            lastCompletedAttunement = null;
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        state = State.IDLE;
        targetAttunement = null;
        lastCompletedAttunement = null;
    }

    public void onChatMessage(String message) {
        if (!enabled || state != State.IDLE) return;

        Matcher matcher = ATTUNEMENT_PATTERN.matcher(message);
        if (matcher.find()) {
            String attunementName = matcher.group(1).toUpperCase();
            triggerAttunement(attunementName, "chat");
        }
    }

    private void triggerAttunement(String attunementName, String source) {
        try {
            Attunement att = Attunement.valueOf(attunementName);
            if (att == lastCompletedAttunement) return;
            targetAttunement = att;
            state = State.FINDING_DAGGER;
            System.out.println("[MSF] Dagger swap triggered (" + source + "): " + attunementName
                    + " -> " + targetAttunement.daggerName);
        } catch (IllegalArgumentException e) {
            System.out.println("[MSF] Unknown attunement: " + attunementName);
        }
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null) return;

        switch (state) {
            case IDLE -> scanNametags();
            case FINDING_DAGGER -> handleFindingDagger();
            case SWAPPED_TO_DAGGER -> handleSwappedToDagger();
            case CHECKING_MODE -> handleCheckingMode();
            case WAITING_AFTER_TOGGLE -> handleWaitingAfterToggle();
            case DONE -> {
                lastCompletedAttunement = targetAttunement;
                scanCooldown.schedule(1500);
                state = State.IDLE;
                targetAttunement = null;
            }
        }
    }

    private static final double BOSS_NAMETAG_CLUSTER_RADIUS_SQ = 4.0 * 4.0;

    private void scanNametags() {
        if (mc.player == null || mc.world == null) return;
        if (scanCooldown.isScheduled() && !scanCooldown.passed()) return;

        tickCounter++;
        if (tickCounter % SCAN_INTERVAL_TICKS != 0) return;

        String playerName = mc.player.getName().getString();

        List<ArmorStandEntity> stands = EntityScanner.getEntitiesWithinRadius(
                mc.player.getEntityPos(), NAMETAG_SCAN_RADIUS, ArmorStandEntity.class
        );

        // Find our boss by looking for "Spawned by: <player>" nametag
        Vec3d ourBossPos = null;
        for (ArmorStandEntity stand : stands) {
            Text customName = stand.getCustomName();
            if (customName == null) continue;
            String name = Formatting.strip(customName.getString());
            if (name == null) continue;
            if (name.contains("Spawned by") && name.contains(playerName)) {
                ourBossPos = stand.getEntityPos();
                break;
            }
        }

        if (ourBossPos == null) return;

        // Only check nametags near our boss
        boolean sawImmune = false;
        Attunement detectedAttunement = null;

        for (ArmorStandEntity stand : stands) {
            if (stand.squaredDistanceTo(ourBossPos) > BOSS_NAMETAG_CLUSTER_RADIUS_SQ) continue;

            Text customName = stand.getCustomName();
            if (customName == null) continue;
            String name = Formatting.strip(customName.getString());
            if (name == null) continue;
            String upper = name.toUpperCase();

            if (upper.contains("IMMUNE")) {
                sawImmune = true;
            }

            if (detectedAttunement == null) {
                for (Attunement att : Attunement.values()) {
                    if (upper.contains(att.name())) {
                        detectedAttunement = att;
                        break;
                    }
                }
            }
        }

        if (sawImmune) {
            lastCompletedAttunement = null;
        }

        if (detectedAttunement != null && detectedAttunement != lastCompletedAttunement) {
            triggerAttunement(detectedAttunement.name(), sawImmune ? "immune" : "nametag");
        }
    }

    private void handleFindingDagger() {
        if (mc.player == null || targetAttunement == null) {
            state = State.IDLE;
            return;
        }

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
            System.out.println("[MSF] Dagger already in correct mode");
            state = State.DONE;
        } else {
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
    public void addSettingsWidgets(Consumer<ClickableWidget> adder) {
        adder.accept(ConfigSliderWidget.forInt("Swap Delay (ms)", 50, 500, MacroConfig.daggerSwapDelayMs,
                v -> MacroConfig.daggerSwapDelayMs = v));
        adder.accept(ConfigSliderWidget.forInt("Toggle Delay (ms)", 50, 500, MacroConfig.daggerToggleDelayMs,
                v -> MacroConfig.daggerToggleDelayMs = v));
    }
}
