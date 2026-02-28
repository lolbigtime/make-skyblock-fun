package com.fishingmacro.macro;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.feature.AntiAfk;
import com.fishingmacro.feature.BiteDetector;
import com.fishingmacro.feature.ChatSeaCreatureDetector;
import com.fishingmacro.feature.FailsafeHandler;
import com.fishingmacro.feature.SeaCreatureDetector;
import com.fishingmacro.gui.MainMenuScreen;
import com.fishingmacro.gui.SettingsScreen;
import com.fishingmacro.handler.KeySimulator;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.pathfinding.ReturnHandler;
import com.fishingmacro.util.Clock;
import com.fishingmacro.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Optional;

public class FishingMacro {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static FishingMacro instance;

    private MacroState state = MacroState.IDLE;
    private boolean running = false;

    private final BiteDetector biteDetector = new BiteDetector();
    private final SeaCreatureDetector seaCreatureDetector = new SeaCreatureDetector();
    private final AntiAfk antiAfk = new AntiAfk();
    private final ReturnHandler returnHandler = new ReturnHandler();
    private ChatSeaCreatureDetector chatSeaCreatureDetector;

    private final Clock stateTimer = new Clock();
    private LivingEntity targetCreature = null;
    private int hyperionAttempts = 0;
    private final Clock attackTimer = new Clock();
    private final Clock killTimeout = new Clock();
    private boolean rodSlotSelected = false;
    private boolean lookedDown = false;
    private boolean hyperionFallback = false;
    private boolean rotationRestored = false;
    private final Clock meleeAimCooldown = new Clock();
    private World savedWorld = null;
    private int detectedRodSlot = 1;
    private int detectedWeaponSlot = 0;

    public static FishingMacro getInstance() {
        if (instance == null) instance = new FishingMacro();
        return instance;
    }

    public void setChatSeaCreatureDetector(ChatSeaCreatureDetector detector) {
        this.chatSeaCreatureDetector = detector;
    }

    public void start() {
        if (mc.player == null || mc.world == null) return;
        running = true;
        savedWorld = mc.world;
        scanHotbarForItems();
        returnHandler.savePosition();
        antiAfk.start();
        biteDetector.reset();
        rodSlotSelected = false;
        if (chatSeaCreatureDetector != null) chatSeaCreatureDetector.reset();
        changeState(MacroState.CASTING);
    }

    public void stop() {
        running = false;
        KeySimulator.releaseAllKeys();
        RotationHandler.getInstance().stop();
        returnHandler.reset();
        antiAfk.reset();
        biteDetector.reset();
        FailsafeHandler.getInstance().reset();
        targetCreature = null;
        rodSlotSelected = false;
        if (chatSeaCreatureDetector != null) chatSeaCreatureDetector.reset();
        changeState(MacroState.IDLE);
    }

    public boolean isRunning() {
        return running;
    }

    public MacroState getState() {
        return state;
    }

    public ReturnHandler getReturnHandler() {
        return returnHandler;
    }

    public int getDetectedRodSlot() {
        return detectedRodSlot;
    }

    public int getDetectedWeaponSlot() {
        return detectedWeaponSlot;
    }

    public void onTick() {
        if (!running || mc.player == null || mc.world == null) return;

        // Stop if world changed (server switch/disconnect)
        if (mc.world != savedWorld) {
            stop();
            return;
        }

        // Close any open screen instead of pausing (but don't close our own screens)
        Screen currentScreen = mc.currentScreen;
        if (currentScreen != null
                && !(currentScreen instanceof MainMenuScreen)
                && !(currentScreen instanceof SettingsScreen)) {
            mc.setScreen(null);
            return; // give one tick for the screen to close
        }

        // Failsafe check before state dispatch
        FailsafeHandler.getInstance().onTick();
        if (!running) return; // failsafe may have stopped the macro

        switch (state) {
            case CASTING -> handleCasting();
            case WAITING_FOR_BITE -> handleWaitingForBite();
            case REELING -> handleReeling();
            case RE_CASTING -> handleReCasting();
            case SEA_CREATURE_DETECTED -> handleSeaCreatureDetected();
            case SWAPPING_TO_WEAPON -> handleSwappingToWeapon();
            case KILLING -> handleKilling();
            case SWAPPING_TO_ROD -> handleSwappingToRod();
            case RETURNING_TO_SPOT -> handleReturning();
            case RESUMING -> handleResuming();
            default -> {}
        }
    }

    // --- State Handlers ---

    private void handleCasting() {
        if (!stateTimer.isScheduled()) {
            // Make sure we're on the rod slot
            KeySimulator.pressHotbar(detectedRodSlot);
            stateTimer.schedule(MathUtil.randomBetween(100, 200));
            return;
        }
        if (!stateTimer.passed()) return;

        // Cast the rod
        KeySimulator.rightClick();
        changeState(MacroState.WAITING_FOR_BITE);
        // Small delay before we start scanning
        stateTimer.schedule(MathUtil.randomBetween(500, 800));
    }

    private void handleWaitingForBite() {
        // Don't scan during initial delay after casting
        if (stateTimer.isScheduled() && !stateTimer.passed()) return;

        // Check for sea creatures nearby
        Optional<LivingEntity> creature = seaCreatureDetector.detectSeaCreature();
        if (creature.isPresent()) {
            targetCreature = creature.get();
            long[] range = MacroConfig.humanize(MacroConfig.reelDelayMs);
            stateTimer.schedule(MathUtil.randomBetween(range[0], range[1]));
            changeState(MacroState.SEA_CREATURE_DETECTED);
            return;
        }

        // Check knockback (after sea creature check so we kill first, then return)
        if (returnHandler.isKnockedOff()) {
            // Human reaction delay before responding to knockback
            long[] kbRange = MacroConfig.humanize(MacroConfig.knockbackReactionMs);
            stateTimer.schedule(MathUtil.randomBetween(kbRange[0], kbRange[1]));
            changeState(MacroState.RETURNING_TO_SPOT);
            return;
        }

        // Check for fish bite
        if (biteDetector.hasBite()) {
            // Human reaction delay
            long[] reelRange = MacroConfig.humanize(MacroConfig.reelDelayMs);
            stateTimer.schedule(MathUtil.randomBetween(reelRange[0], reelRange[1]));
            rodSlotSelected = false;
            changeState(MacroState.REELING);
            return;
        }

        // Anti-AFK ticks during waiting
        antiAfk.onTick();
    }

    private void handleReeling() {
        if (!stateTimer.passed()) return;

        // Ensure rod is selected before reeling
        if (!rodSlotSelected) {
            KeySimulator.pressHotbar(detectedRodSlot);
            rodSlotSelected = true;
            stateTimer.schedule(MathUtil.randomBetween(50, 120));
            return;
        }

        // Right-click to reel in
        KeySimulator.rightClick();
        rodSlotSelected = false;

        // Post-reel delay to let the catch register
        long[] postReelRange = MacroConfig.humanize(MacroConfig.postReelDelayMs);
        stateTimer.schedule(MathUtil.randomBetween(postReelRange[0], postReelRange[1]));
        changeState(MacroState.RE_CASTING);
    }

    private void handleReCasting() {
        if (!stateTimer.passed()) return;

        // Check if a sea creature is nearby — skip recast and go straight to killing
        Optional<LivingEntity> creature = seaCreatureDetector.detectSeaCreature();
        if (creature.isPresent()) {
            targetCreature = creature.get();
            // Rod is already reeled in from REELING state, go straight to weapon swap
            KeySimulator.pressHotbar(detectedWeaponSlot);
            hyperionAttempts = 0;
            lookedDown = false;
            hyperionFallback = false;
            killTimeout.schedule(MacroConfig.killTimeoutMs);
            stateTimer.schedule(MathUtil.randomBetween(50, 120));
            changeState(MacroState.KILLING);
            return;
        }

        // Ensure rod is selected before casting
        if (!rodSlotSelected) {
            KeySimulator.pressHotbar(detectedRodSlot);
            rodSlotSelected = true;
            stateTimer.schedule(MathUtil.randomBetween(50, 120));
            return;
        }

        KeySimulator.rightClick();
        rodSlotSelected = false;
        changeState(MacroState.WAITING_FOR_BITE);
        stateTimer.schedule(MathUtil.randomBetween(500, 800));
    }

    private void handleSeaCreatureDetected() {
        if (!stateTimer.passed()) return;

        // Reel in the rod first (right-click to pull rod back)
        KeySimulator.rightClick();
        stateTimer.schedule(MathUtil.randomBetween(50, 150));
        changeState(MacroState.SWAPPING_TO_WEAPON);
    }

    private void handleSwappingToWeapon() {
        if (!stateTimer.passed()) return;

        KeySimulator.pressHotbar(detectedWeaponSlot);
        hyperionAttempts = 0;
        lookedDown = false;
        hyperionFallback = false;
        killTimeout.schedule(MacroConfig.killTimeoutMs);
        stateTimer.schedule(MathUtil.randomBetween(50, 120));
        changeState(MacroState.KILLING);
    }

    private void handleKilling() {
        if (targetCreature == null || targetCreature.isDead() || targetCreature.isRemoved()) {
            // Target is dead, swap back to rod
            stateTimer.schedule(MathUtil.randomBetween(100, 300));
            changeState(MacroState.SWAPPING_TO_ROD);
            return;
        }

        // Timeout - blacklist the entity so we don't re-target it
        if (killTimeout.passed()) {
            if (targetCreature != null) {
                seaCreatureDetector.blacklistEntity(targetCreature.getId());
            }
            stateTimer.schedule(MathUtil.randomBetween(100, 200));
            changeState(MacroState.SWAPPING_TO_ROD);
            return;
        }

        if (MacroConfig.useHyperion && !hyperionFallback) {
            handleHyperionKill();
        } else {
            handleMeleeKill();
        }
    }

    private void handleHyperionKill() {
        // Aim at the target entity for Wither Impact
        if (!RotationHandler.getInstance().isRotating()
                && (!meleeAimCooldown.isScheduled() || meleeAimCooldown.passed())) {
            RotationHandler.getInstance().easeToEntity(targetCreature);
            meleeAimCooldown.schedule(MathUtil.randomBetween(200, 400));
        }

        if (RotationHandler.getInstance().isRotating()) return;

        // Right-click to use Hyperion ability
        if (!attackTimer.isScheduled() || attackTimer.passed()) {
            KeySimulator.rightClick();
            hyperionAttempts++;
            attackTimer.schedule(MacroConfig.hyperionRetryDelayMs +
                    (long) MathUtil.randomFloat(-50, 100));
        }

        if (hyperionAttempts >= MacroConfig.hyperionMaxAttempts) {
            // Hyperion failed - fall back to melee for the remaining kill timeout
            hyperionFallback = true;
            attackTimer.reset();
        }
    }

    private void handleMeleeKill() {
        if (targetCreature == null || targetCreature.isDead() || targetCreature.isRemoved()) return;

        // Walk toward creature and attack with humanized aim
        double dist = mc.player.squaredDistanceTo(targetCreature);

        // Skip re-aiming when creature is very close (< 2 blocks = 4.0 sq dist)
        if (dist >= 4.0 && !RotationHandler.getInstance().isRotating()
                && (!meleeAimCooldown.isScheduled() || meleeAimCooldown.passed())) {
            RotationHandler.getInstance().easeToEntity(targetCreature);
            meleeAimCooldown.schedule(MathUtil.randomBetween(200, 400));
        }

        // Jump if target is above us (flying mobs like Banshees)
        if (targetCreature.getY() > mc.player.getY() + 1.5) {
            KeySimulator.pressKey(mc.options.jumpKey);
        } else {
            KeySimulator.releaseKey(mc.options.jumpKey);
        }

        // Walk toward if too far
        if (dist > 9.0) { // > 3 blocks
            KeySimulator.pressKey(mc.options.forwardKey);
            // Random sprint toggling
            if (Math.random() < 0.05) {
                com.fishingmacro.mixin.KeyBindingAccessor sprintAcc =
                        (com.fishingmacro.mixin.KeyBindingAccessor) mc.options.sprintKey;
                if (sprintAcc.getPressed()) {
                    KeySimulator.releaseKey(mc.options.sprintKey);
                } else {
                    KeySimulator.pressKey(mc.options.sprintKey);
                }
            }
        } else {
            KeySimulator.releaseKey(mc.options.forwardKey);
            KeySimulator.releaseKey(mc.options.sprintKey);
        }

        // Attack at randomized CPS (8-12)
        if (!attackTimer.isScheduled() || attackTimer.passed()) {
            if (dist <= 25.0) { // Within ~5 blocks, attack
                KeySimulator.leftClick();
            }
            int cpsDelay = 1000 / MathUtil.randomBetween(MacroConfig.meleeCpsMin, MacroConfig.meleeCpsMax);
            attackTimer.schedule(cpsDelay + (long) MathUtil.randomFloat(-15, 30));
        }
    }

    private void handleSwappingToRod() {
        if (!stateTimer.passed()) return;

        // Release combat keys
        KeySimulator.releaseKey(mc.options.forwardKey);
        KeySimulator.releaseKey(mc.options.sprintKey);
        KeySimulator.releaseKey(mc.options.jumpKey);
        KeySimulator.releaseKey(mc.options.attackKey);

        KeySimulator.pressHotbar(detectedRodSlot);
        targetCreature = null;
        attackTimer.reset();
        killTimeout.reset();
        meleeAimCooldown.reset();
        rodSlotSelected = false;

        // If knocked off during combat, return to spot before casting
        if (returnHandler.isKnockedOff()) {
            stateTimer.schedule(MathUtil.randomBetween(200, 400));
            changeState(MacroState.RETURNING_TO_SPOT);
        } else {
            stateTimer.schedule(MathUtil.randomBetween(200, 400));
            changeState(MacroState.CASTING);
        }
    }

    private void handleReturning() {
        if (!stateTimer.passed()) return;

        if (!returnHandler.isReturning()) {
            returnHandler.startReturn();
        }

        returnHandler.onTick();

        if (returnHandler.hasArrived() || returnHandler.hasTimedOut()) {
            KeySimulator.releaseKey(mc.options.forwardKey);
            KeySimulator.releaseKey(mc.options.sprintKey);
            KeySimulator.releaseKey(mc.options.jumpKey);
            KeySimulator.releaseKey(mc.options.leftKey);
            KeySimulator.releaseKey(mc.options.rightKey);
            returnHandler.stopReturn();
            rodSlotSelected = false;
            rotationRestored = false;
            stateTimer.schedule(MathUtil.randomBetween(100, 300));
            changeState(MacroState.RESUMING);
        }
    }

    private void handleResuming() {
        if (!stateTimer.passed()) return;

        if (!rotationRestored) {
            returnHandler.restoreRotation();
            rotationRestored = true;
            stateTimer.schedule(50);
            return;
        }

        // Wait for rotation to finish, then cast
        if (!RotationHandler.getInstance().isRotating()) {
            long[] castRange = MacroConfig.humanize(MacroConfig.castDelayMs);
            stateTimer.schedule(MathUtil.randomBetween(castRange[0], castRange[1]));
            changeState(MacroState.CASTING);
        }
    }

    private void scanHotbarForItems() {
        if (mc.player == null) return;

        // Defaults if items not found
        detectedRodSlot = 1;
        detectedWeaponSlot = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof FishingRodItem) {
                detectedRodSlot = i;
            }

            if (stack.getName().getString().toLowerCase().contains("hyperion")) {
                detectedWeaponSlot = i;
            }
        }

        System.out.println("[FishingMacro] Detected rod slot: " + detectedRodSlot
                + ", weapon slot: " + detectedWeaponSlot);
    }

    private void changeState(MacroState newState) {
        if (state != newState) {
            state = newState;
        }
    }
}
