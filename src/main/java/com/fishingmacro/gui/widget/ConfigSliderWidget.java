package com.fishingmacro.gui.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ConfigSliderWidget extends SliderWidget {
    private final String label;
    private final double min;
    private final double max;
    private final boolean intMode;
    private final String suffix;
    private final Consumer<Number> setter;

    public ConfigSliderWidget(int x, int y, int width, int height,
                              String label, double min, double max, double currentValue,
                              boolean intMode, String suffix, Consumer<Number> setter) {
        super(x, y, width, height, Text.empty(), toSliderValue(currentValue, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.intMode = intMode;
        this.suffix = suffix;
        this.setter = setter;
        updateMessage();
    }

    private static double toSliderValue(double value, double min, double max) {
        if (max <= min) return 0;
        return (value - min) / (max - min);
    }

    private double getActualValue() {
        return min + value * (max - min);
    }

    @Override
    protected void updateMessage() {
        if (intMode) {
            setMessage(Text.literal(label + ": " + (int) Math.round(getActualValue()) + suffix));
        } else {
            setMessage(Text.literal(label + ": " + String.format("%.1f", getActualValue()) + suffix));
        }
    }

    @Override
    protected void applyValue() {
        if (intMode) {
            setter.accept((int) Math.round(getActualValue()));
        } else {
            setter.accept(getActualValue());
        }
    }
}
