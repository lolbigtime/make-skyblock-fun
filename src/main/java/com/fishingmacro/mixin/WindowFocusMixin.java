package com.fishingmacro.mixin;

import com.fishingmacro.macro.FishingMacro;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the pause screen from opening and FPS from being throttled when
 * the window loses focus while the macro is running.
 * Cancelling onWindowFocusChanged keeps windowFocused=true internally,
 * which also prevents the InactivityFpsLimiter from kicking in.
 */
@Mixin(MinecraftClient.class)
public class WindowFocusMixin {

    @Inject(method = "onWindowFocusChanged", at = @At("HEAD"), cancellable = true)
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        if (!focused && FishingMacro.getInstance().isRunning()) {
            ci.cancel();
        }
    }
}
