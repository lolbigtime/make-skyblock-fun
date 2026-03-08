package com.msf.mixin;

import com.msf.gui.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class ImGuiKeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput keyInput, CallbackInfo ci) {
        ImGuiManager imgui = ImGuiManager.getInstance();
        if (imgui.isVisible() && imgui.isInitialized()) {
            ImGuiIO io = ImGui.getIO();
            int key = keyInput.key();
            int mods = keyInput.modifiers();
            if (key >= 0 && key < 512) {
                io.setKeysDown(key, action != 0); // 0 = GLFW_RELEASE
            }
            io.setKeyCtrl((mods & 2) != 0);  // GLFW_MOD_CONTROL
            io.setKeyShift((mods & 1) != 0);  // GLFW_MOD_SHIFT
            io.setKeyAlt((mods & 4) != 0);    // GLFW_MOD_ALT
            io.setKeySuper((mods & 8) != 0);  // GLFW_MOD_SUPER
            if (io.getWantCaptureKeyboard()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, CharInput charInput, CallbackInfo ci) {
        ImGuiManager imgui = ImGuiManager.getInstance();
        if (imgui.isVisible() && imgui.isInitialized()) {
            ImGuiIO io = ImGui.getIO();
            io.addInputCharacter(charInput.codepoint());
            if (io.getWantCaptureKeyboard()) {
                ci.cancel();
            }
        }
    }
}
