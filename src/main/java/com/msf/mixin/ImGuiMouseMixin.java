package com.msf.mixin;

import com.msf.gui.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class ImGuiMouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput mouseInput, int action, CallbackInfo ci) {
        ImGuiManager imgui = ImGuiManager.getInstance();
        if (imgui.isVisible() && imgui.isInitialized()) {
            ImGuiIO io = ImGui.getIO();
            int button = mouseInput.button();
            if (button >= 0 && button < 5) {
                io.setMouseDown(button, action == GLFW.GLFW_PRESS);
            }
            if (io.getWantCaptureMouse()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        ImGuiManager imgui = ImGuiManager.getInstance();
        if (imgui.isVisible() && imgui.isInitialized()) {
            ImGuiIO io = ImGui.getIO();
            io.setMouseWheel(io.getMouseWheel() + (float) vertical);
            io.setMouseWheelH(io.getMouseWheelH() + (float) horizontal);
            if (io.getWantCaptureMouse()) {
                ci.cancel();
            }
        }
    }
}
