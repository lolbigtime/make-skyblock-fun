package com.fishingmacro.macro;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.feature.AntiAfk;
import com.fishingmacro.feature.BiteDetector;
import com.fishingmacro.feature.SeaCreatureDetector;
import com.fishingmacro.handler.KeySimulator;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.pathfinding.ReturnHandler;
import com.fishingmacro.util.Clock;
import com.fishingmacro.util.MathUtil;
import net.minecraft.client.MinecraftClient;
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

    private final Clock stateTimer = new Clock();
    private LivingEntity targetCreature = null;
    private int hyperionAttempts = 0;
    private final Clock attackTimer = new Clock();
    private final Clock killTimeout = new Clock();
    private boolean rodSlotSelected = false;
    private boolean lookedDown = false;
    private final Clock meleeAimCooldown = new Clock();
    private World savedWorld = null;
    private int detectedRodSlot = 1;
    private int detectedWeaponSlot = 0;

    public static FishingMacro getInstance() {
        if (instance == null) instance = new FishingMacro();
        return instance;
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
        changeState(MacroState.CASTING);
    }

    public void stop() {
        running = false;
        KeySimulator.releaseAllKeys();
        RotationHandler.getInstance().stop();
        returnHandler.reset();
        antiAfk.reset();
        biteDetector.reset();
        targetCreature = null;
        rodSlotSelected = false;
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

    public void onTick() {
        if (!running || mc.player == null || mc.world == null) return;

        // Stop if world changed (server switch/disconnect)
        if (mc.world != savedWorld) {
            stop();
            return;
        }

        // Close any open screen instead of pausing
        if (mc.currentScreen != null) {
            mc.setScreen(null);
            return; // give one tick for the screen to close
        }

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

        // Check for sea creatures first (kill before returning)
        Optional<LivingEntity> creature = seaCreatureDetector.detectSeaCreature();
        if (creature.isPresent()) {
            targetCreature = creature.get();
            // Human reaction delay
            stateTimer.schedule(MathUtil.randomBetween(
                    (long) MacroConfig.reelDelayMinMs,
                    (long) MacroConfig.reelDelayMaxMs
            ));
            changeState(MacroState.SEA_CREATURE_DETECTED);
            return;
        }

        // Check knockback (after sea creature check so we kill first, then return)
        if (returnHandler.isKnockedOff()) {
            // Human reaction delay before responding to knockback
            long reactionDelay = MathUtil.randomBetween(
                    (long) MacroConfig.knockbackReactionMinMs,
                    (long) MacroConfig.knockbackReactionMaxMs
            );
            stateTimer.schedule(reactionDelay);
            changeState(MacroState.RETURNING_TO_SPOT);
            return;
        }

        // Check for fish bite
        if (biteDetector.hasBite()) {
            // Human reaction delay
            stateTimer.schedule(MathUtil.randomBetween(
                    (long) MacroConfig.reelDelayMinMs,
                    (long) MacroConfig.reelDelayMaxMs
            ));
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
        stateTimer.schedule(MathUtil.randomBetween(
                (long) MacroConfig.postReelDelayMinMs,
                (long) MacroConfig.postReelDelayMaxMs
        ));
        changeState(MacroState.RE_CASTING);
    }

    private void handleReCasting() {
        if (!stateTimer.passed()) return;

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
        stateTimer.schedule(MathUtil.randomBetween(100, 250));
        changeState(MacroState.SWAPPING_TO_WEAPON);
    }

    private void handleSwappingToWeapon() {
        if (!stateTimer.passed()) return;

        KeySimulator.pressHotbar(detectedWeaponSlot);
        hyperionAttempts = 0;
        lookedDown = false;
        killTimeout.schedule(MacroConfig.killTimeoutMs);
        stateTimer.schedule(MathUtil.randomBetween(100, 200));
        changeState(MacroState.KILLING);
    }

    private void handleKilling() {
        if (targetCreature == null || targetCreature.isDead() || targetCreature.isRemoved()) {
            // Target is dead, swap back to rod
            stateTimer.schedule(MathUtil.randomBetween(100, 300));
            changeState(MacroState.SWAPPING_TO_ROD);
            return;
        }

        // Timeout
        if (killTimeout.passed()) {
            stateTimer.schedule(MathUtil.randomBetween(100, 200));
            changeState(MacroState.SWAPPING_TO_ROD);
            return;
        }

        if (MacroConfig.useHyperion) {
            handleHyperionKill();
        } else {
            handleMeleeKill();
        }
    }

    private void handleHyperionKill() {
        // Look down at floor for Wither Impact AoE
        if (!lookedDown) {
            RotationHandler.getInstance().lookDownAtFloor();
            lookedDown = true;
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
            // Max attempts reached, move on
            stateTimer.schedule(MathUtil.randomBetween(100, 300));
            changeState(MacroState.SWAPPING_TO_ROD);
        }
    }

    private void handleMeleeKill() {
        // Walk toward creature and attack with humanized aim
        double dist = mc.player.squaredDistanceTo(targetCreature);

        // Aim at creature with humanized rotation (cooldown between re-aims)
        if (!RotationHandler.getInstance().isRotating()
                && (!meleeAimCooldown.isScheduled() || meleeAimCooldown.passed())) {
            RotationHandler.getInstance().easeToEntity(targetCreature);
            meleeAimCooldown.schedule(MathUtil.randomBetween(200, 400));
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

        if (returnHandler.hasArrived()) {
            KeySimulator.releaseKey(mc.options.forwardKey);
            KeySimulator.releaseKey(mc.options.sprintKey);
            returnHandler.stopReturn();
            rodSlotSelected = false;
            stateTimer.schedule(MathUtil.randomBetween(100, 300));
            changeState(MacroState.RESUMING);
        }
    }

    private void handleResuming() {
        if (!stateTimer.passed()) return;

        if (!RotationHandler.getInstance().isRotating()) {
            // First tick: restore rotation
            returnHandler.restoreRotation();
            stateTimer.schedule(50);
            return;
        }

        // Wait for rotation to finish, then cast
        if (!RotationHandler.getInstance().isRotating()) {
            stateTimer.schedule(MathUtil.randomBetween(
                    (long) MacroConfig.castDelayMinMs,
                    (long) MacroConfig.castDelayMaxMs
            ));
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
