package com.msf.gui.imgui;

import com.msf.config.MacroConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class ImGuiManager {
    private static ImGuiManager instance;

    private ImGuiImplGlfw implGlfw;
    private ImGuiImplGl3 implGl3;
    private boolean initialized = false;
    private boolean visible = false;
    private final MSFWindow window = new MSFWindow();

    public static ImGuiManager getInstance() {
        if (instance == null) instance = new ImGuiManager();
        return instance;
    }

    private void init() {
        if (initialized) return;

        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null); // Don't save imgui.ini

        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();

        implGlfw = new ImGuiImplGlfw();
        implGlfw.init(windowHandle, false); // No auto callbacks - we forward manually

        implGl3 = new ImGuiImplGl3();
        implGl3.init("#version 150"); // MC uses GL 3.2 core profile

        initialized = true;
    }

    public void render() {
        if (!visible) return;

        // Lazy init on first render (GL context needed)
        if (!initialized) {
            init();
        }

        // Save GL state
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int prevElementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int prevVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean prevScissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int prevBlendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int prevBlendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);

        // New frame
        implGlfw.newFrame();
        ImGui.newFrame();

        // Build UI
        window.render();

        // Render
        ImGui.render();
        implGl3.renderDrawData(ImGui.getDrawData());

        // Restore GL state
        GL20.glUseProgram(prevProgram);
        GL13.glActiveTexture(prevActiveTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, prevElementBuffer);
        GL30.glBindVertexArray(prevVertexArray);
        GL11.glBlendFunc(prevBlendSrc, prevBlendDst);
        if (prevBlend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        if (prevDepthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (prevCullFace) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
        if (prevScissorTest) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void toggle() {
        visible = !visible;
        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        if (visible) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        } else {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            MacroConfig.save(); // Auto-save on close
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
