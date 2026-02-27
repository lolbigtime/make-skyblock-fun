package com.fishingmacro.gui;

import com.fishingmacro.feature.RotationTestMode;
import com.fishingmacro.macro.FishingMacro;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MainMenuScreen extends Screen {

    public MainMenuScreen() {
        super(Text.literal("MSF"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        FishingMacro macro = FishingMacro.getInstance();
        boolean running = macro.isRunning();

        // Fishing Macro card - large toggle button
        String fishingLabel = running ? "Fishing Macro [RUNNING]" : "Fishing Macro [STOPPED]";
        addDrawableChild(ButtonWidget.builder(Text.literal(fishingLabel), button -> {
            if (macro.isRunning()) {
                macro.stop();
            } else {
                macro.start();
            }
            close();
        }).dimensions(centerX - 100, centerY - 40, 200, 30).build());

        // Settings button
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), button -> {
            client.setScreen(new SettingsScreen(this));
        }).dimensions(centerX - 100, centerY + 5, 95, 20).build());

        // Test Rotation button
        addDrawableChild(ButtonWidget.builder(Text.literal("Test Rotation"), button -> {
            close();
            RotationTestMode.getInstance().start();
        }).dimensions(centerX + 5, centerY + 5, 95, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        int titleWidth = textRenderer.getWidth("MSF");
        context.drawText(textRenderer, "MSF", width / 2 - titleWidth / 2, height / 2 - 60, 0xFF55FFFF, true);

        // Status indicator
        FishingMacro macro = FishingMacro.getInstance();
        String status = macro.isRunning() ? "Status: Running" : "Status: Stopped";
        int statusColor = macro.isRunning() ? 0xFF55FF55 : 0xFFFF5555;
        int statusWidth = textRenderer.getWidth(status);
        context.drawText(textRenderer, status, width / 2 - statusWidth / 2, height / 2 - 48, statusColor, true);
    }

    @Override
    public void close() {
        client.setScreen(null);
    }
}
