package com.msf.mixin;

import com.msf.gui.imgui.ImGuiManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(boolean tick, CallbackInfo ci) {
        ImGuiManager.getInstance().render();
    }
}
