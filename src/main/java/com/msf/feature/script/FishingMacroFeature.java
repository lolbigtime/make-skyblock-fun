package com.msf.feature.script;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.macro.FishingMacro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class FishingMacroFeature implements Feature {

    @Override
    public String getName() {
        return "Fishing Macro";
    }

    @Override
    public String getDescription() {
        return "Automated fishing macro for SkyBlock";
    }

    @Override
    public FeatureCategory getCategory() {
        return FeatureCategory.SCRIPT;
    }

    @Override
    public boolean isEnabled() {
        return FishingMacro.getInstance().isRunning();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && !FishingMacro.getInstance().isRunning()) {
            FishingMacro.getInstance().start();
        } else if (!enabled && FishingMacro.getInstance().isRunning()) {
            FishingMacro.getInstance().stop();
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public void onTick() {
        FishingMacro.getInstance().onTick();
    }

    @Override
    public void addSettingsWidgets(Consumer<ClickableWidget> adder) {
        adder.accept(CheckboxWidget.builder(
                Text.literal("Kill Sea Creatures"),
                MinecraftClient.getInstance().textRenderer
        ).checked(MacroConfig.killSeaCreatures).callback((cb, checked) -> {
            MacroConfig.killSeaCreatures = checked;
        }).build());
    }
}
