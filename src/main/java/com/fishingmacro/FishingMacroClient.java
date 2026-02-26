package com.fishingmacro;

import com.fishingmacro.config.MacroConfig;
import com.fishingmacro.handler.RotationHandler;
import com.fishingmacro.macro.FishingMacro;
import com.fishingmacro.render.RenderHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FishingMacroClient implements ClientModInitializer {
    public static final String MOD_ID = "fishingmacro";
    public static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        MacroConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fishingmacro.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "category"))
        ));

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

        FishingMacro.getInstance().onTick();
        RotationHandler.getInstance().onTick();
    }

    private void sendMessage(MinecraftClient client, String message, Formatting color) {
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[FishingMacro] " + message).formatted(color),
                    true
            );
        }
    }
}
