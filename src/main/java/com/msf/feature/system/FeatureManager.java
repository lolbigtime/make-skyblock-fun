package com.msf.feature.system;

import com.msf.config.MacroConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FeatureManager {
    private static FeatureManager instance;
    private final List<Feature> features = new ArrayList<>();

    public static FeatureManager getInstance() {
        if (instance == null) instance = new FeatureManager();
        return instance;
    }

    public void register(Feature feature) {
        features.add(feature);
        // Restore saved enabled state
        Boolean savedState = MacroConfig.featureStates.get(feature.getName());
        if (savedState != null && savedState) {
            feature.setEnabled(true);
            feature.onEnable();
        }
    }

    public void tickAll() {
        for (Feature feature : features) {
            if (feature.isEnabled()) {
                feature.onTick();
            }
        }
    }

    public List<Feature> getByCategory(FeatureCategory category) {
        List<Feature> result = new ArrayList<>();
        for (Feature feature : features) {
            if (feature.getCategory() == category) {
                result.add(feature);
            }
        }
        return result;
    }

    public Optional<Feature> getByName(String name) {
        for (Feature feature : features) {
            if (feature.getName().equals(name)) {
                return Optional.of(feature);
            }
        }
        return Optional.empty();
    }

    public List<Feature> getAll() {
        return features;
    }
}
