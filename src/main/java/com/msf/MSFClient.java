package com.msf;

import com.msf.config.MacroConfig;
import com.msf.feature.ChatSeaCreatureDetector;
import com.msf.feature.RotationTestMode;
import com.msf.feature.qol.DaggerSwapper;
import com.msf.feature.qol.GemstoneInfusion;
import com.msf.feature.qol.WandOfAtonement;
import com.msf.feature.script.FishingMacroFeature;
import com.msf.feature.system.FeatureManager;
import com.msf.gui.MSFScreen;
import com.msf.handler.RotationHandler;
import com.msf.macro.FishingMacro;
import com.msf.render.RenderHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MSFClient implements ClientModInitializer {
    public static final String MOD_ID = "msf";
    public static KeyBinding toggleKey;
    public static KeyBinding menuKey;

    private final ChatSeaCreatureDetector chatSeaCreatureDetector = new ChatSeaCreatureDetector();

    @Override
    public void onInitializeClient() {
        MacroConfig.load();

        var category = KeyBinding.Category.create(Identifier.of(MOD_ID, "category"));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.msf.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.msf.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        // Wire up chat-based sea creature detection
        FishingMacro.getInstance().setChatSeaCreatureDetector(chatSeaCreatureDetector);

        // Register features
        FeatureManager fm = FeatureManager.getInstance();
        fm.register(new FishingMacroFeature());
        DaggerSwapper daggerSwapper = new DaggerSwapper();
        fm.register(daggerSwapper);
        fm.register(new WandOfAtonement());
        GemstoneInfusion gemstoneInfusion = new GemstoneInfusion();
        fm.register(gemstoneInfusion);

        // Chat event handler
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String msg = message.getString();
            if (FishingMacro.getInstance().isRunning()) {
                chatSeaCreatureDetector.onChatMessage(msg);
            }
            if (daggerSwapper.isEnabled()) {
                daggerSwapper.onChatMessage(msg);
            }
            if (gemstoneInfusion.isEnabled()) {
                gemstoneInfusion.onChatMessage(msg);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        RenderHandler.getInstance().register();
    }

    private void onClientTick(MinecraftClient client) {
        if (toggleKey.wasPressed()) {
            FishingMacro macro = FishingMacro.getInstance();
            if (macro.isRunning()) {
                macro.stop();
                sendMessage(client, "Fishing macro disabled", Formatting.RED);
            } else {
                macro.start();
                sendMessage(client, "Fishing macro enabled", Formatting.GREEN);
            }
        }

        if (menuKey.wasPressed()) {
            if (client.currentScreen instanceof MSFScreen) {
                client.setScreen(null);
            } else {
                client.setScreen(new MSFScreen());
            }
        }

        // Stop macro on server disconnect
        if (FishingMacro.getInstance().isRunning() && client.getNetworkHandler() == null) {
            FishingMacro.getInstance().stop();
            sendMessage(client, "Disconnected - macro stopped", Formatting.RED);
        }

        FeatureManager.getInstance().tickAll();
        RotationHandler.getInstance().onTick();
        RotationTestMode.getInstance().onTick();
    }

    private void sendMessage(MinecraftClient client, String message, Formatting color) {
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[MSF] " + message).formatted(color),
                    true
            );
        }
    }
}
