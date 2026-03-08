package com.msf.gui.imgui;

import com.msf.config.MacroConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class ImGuiManager {
    private static ImGuiManager instance;

    private ImGuiImplGl3 implGl3;
    private boolean initialized = false;
    private boolean visible = false;
    private final MSFWindow window = new MSFWindow();
    private double lastFrameTime = 0;

    public static ImGuiManager getInstance() {
        if (instance == null) instance = new ImGuiManager();
        return instance;
    }

    private void init() {
        if (initialized) return;

        try {
            ImGui.createContext();

            ImGuiIO io = ImGui.getIO();
            io.setIniFilename(null);

            // Map GLFW keys to imgui keys
            io.setKeyMap(imgui.flag.ImGuiKey.Tab, GLFW.GLFW_KEY_TAB);
            io.setKeyMap(imgui.flag.ImGuiKey.LeftArrow, GLFW.GLFW_KEY_LEFT);
            io.setKeyMap(imgui.flag.ImGuiKey.RightArrow, GLFW.GLFW_KEY_RIGHT);
            io.setKeyMap(imgui.flag.ImGuiKey.UpArrow, GLFW.GLFW_KEY_UP);
            io.setKeyMap(imgui.flag.ImGuiKey.DownArrow, GLFW.GLFW_KEY_DOWN);
            io.setKeyMap(imgui.flag.ImGuiKey.PageUp, GLFW.GLFW_KEY_PAGE_UP);
            io.setKeyMap(imgui.flag.ImGuiKey.PageDown, GLFW.GLFW_KEY_PAGE_DOWN);
            io.setKeyMap(imgui.flag.ImGuiKey.Home, GLFW.GLFW_KEY_HOME);
            io.setKeyMap(imgui.flag.ImGuiKey.End, GLFW.GLFW_KEY_END);
            io.setKeyMap(imgui.flag.ImGuiKey.Insert, GLFW.GLFW_KEY_INSERT);
            io.setKeyMap(imgui.flag.ImGuiKey.Delete, GLFW.GLFW_KEY_DELETE);
            io.setKeyMap(imgui.flag.ImGuiKey.Backspace, GLFW.GLFW_KEY_BACKSPACE);
            io.setKeyMap(imgui.flag.ImGuiKey.Space, GLFW.GLFW_KEY_SPACE);
            io.setKeyMap(imgui.flag.ImGuiKey.Enter, GLFW.GLFW_KEY_ENTER);
            io.setKeyMap(imgui.flag.ImGuiKey.Escape, GLFW.GLFW_KEY_ESCAPE);
            io.setKeyMap(imgui.flag.ImGuiKey.KeyPadEnter, GLFW.GLFW_KEY_KP_ENTER);
            io.setKeyMap(imgui.flag.ImGuiKey.A, GLFW.GLFW_KEY_A);
            io.setKeyMap(imgui.flag.ImGuiKey.C, GLFW.GLFW_KEY_C);
            io.setKeyMap(imgui.flag.ImGuiKey.V, GLFW.GLFW_KEY_V);
            io.setKeyMap(imgui.flag.ImGuiKey.X, GLFW.GLFW_KEY_X);
            io.setKeyMap(imgui.flag.ImGuiKey.Y, GLFW.GLFW_KEY_Y);
            io.setKeyMap(imgui.flag.ImGuiKey.Z, GLFW.GLFW_KEY_Z);

            // Build font atlas explicitly before GL3 init
            io.getFonts().build();

            implGl3 = new ImGuiImplGl3();
            implGl3.init("#version 150");

            // Ensure font texture is uploaded
            implGl3.updateFontsTexture();

            lastFrameTime = GLFW.glfwGetTime();
            initialized = true;
            System.out.println("[MSF] ImGui initialized successfully");
        } catch (Exception e) {
            System.err.println("[MSF] Failed to initialize ImGui: " + e.getMessage());
            e.printStackTrace();
            visible = false;
        }
    }

    public void render() {
        if (!visible) return;

        if (!initialized) {
            init();
            if (!initialized) return;
        }

        try {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            ImGuiIO io = ImGui.getIO();

            // Display size
            int[] winW = new int[1], winH = new int[1];
            GLFW.glfwGetWindowSize(handle, winW, winH);
            int[] fbW = new int[1], fbH = new int[1];
            GLFW.glfwGetFramebufferSize(handle, fbW, fbH);
            io.setDisplaySize(winW[0], winH[0]);
            if (winW[0] > 0 && winH[0] > 0) {
                io.setDisplayFramebufferScale((float) fbW[0] / winW[0], (float) fbH[0] / winH[0]);
            }

            // Delta time
            double currentTime = GLFW.glfwGetTime();
            io.setDeltaTime(lastFrameTime > 0 ? (float) (currentTime - lastFrameTime) : 1.0f / 60.0f);
            lastFrameTime = currentTime;

            // Mouse position
            double[] mx = new double[1], my = new double[1];
            GLFW.glfwGetCursorPos(handle, mx, my);
            io.setMousePos((float) mx[0], (float) my[0]);

            // Render to default framebuffer with correct viewport
            int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, fbW[0], fbH[0]);

            ImGui.newFrame();
            window.render();
            ImGui.render();

            // ImGuiImplGl3.renderDrawData handles its own GL state save/restore
            implGl3.renderDrawData(ImGui.getDrawData());

            // Restore framebuffer
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);
        } catch (Exception e) {
            System.err.println("[MSF] ImGui render error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void toggle() {
        visible = !visible;
        System.out.println("[MSF] ImGui toggle: visible=" + visible);
        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        if (visible) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        } else {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            MacroConfig.save();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
