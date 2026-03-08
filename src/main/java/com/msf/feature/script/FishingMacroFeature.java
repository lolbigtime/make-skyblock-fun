package com.msf.feature.script;

import com.msf.config.MacroConfig;
import com.msf.feature.system.Feature;
import com.msf.feature.system.FeatureCategory;
import com.msf.macro.FishingMacro;
import imgui.ImGui;
import imgui.type.ImBoolean;

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
    public void renderSettings() {
        ImBoolean killSea = new ImBoolean(MacroConfig.killSeaCreatures);
        if (ImGui.checkbox("Kill Sea Creatures", killSea)) {
            MacroConfig.killSeaCreatures = killSea.get();
        }

        if (FishingMacro.getInstance().isRunning()) {
            ImGui.separator();
            ImGui.text("State: " + FishingMacro.getInstance().getState().getDisplayName());
        }
    }
}
