package com.msf.feature.system;

public interface Feature {
    String getName();
    String getDescription();
    FeatureCategory getCategory();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    void onEnable();
    void onDisable();
    void onTick();

    default void renderSettings() {}
}
