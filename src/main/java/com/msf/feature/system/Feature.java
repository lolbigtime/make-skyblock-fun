package com.msf.feature.system;

import net.minecraft.client.gui.widget.ClickableWidget;
import java.util.function.Consumer;

public interface Feature {
    String getName();
    String getDescription();
    FeatureCategory getCategory();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    void onEnable();
    void onDisable();
    void onTick();

    default void addSettingsWidgets(Consumer<ClickableWidget> widgetAdder) {}
}
