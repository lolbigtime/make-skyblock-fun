package com.msf.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ConfigSliderWidget extends SliderWidget {
    private final String label;
    private final double min;
    private final double max;
    private final boolean isInt;
    private final Consumer<Number> setter;

    private ConfigSliderWidget(int width, String label, double min, double max, double current, boolean isInt, Consumer<Number> setter) {
        super(0, 0, width, 20, Text.empty(), (current - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.isInt = isInt;
        this.setter = setter;
        updateMessage();
    }

    public static ConfigSliderWidget forInt(String label, int min, int max, int current, Consumer<Integer> setter) {
        return new ConfigSliderWidget(310, label, min, max, current, true, n -> setter.accept(n.intValue()));
    }

    public static ConfigSliderWidget forFloat(String label, float min, float max, float current, Consumer<Float> setter) {
        return new ConfigSliderWidget(310, label, min, max, current, false, n -> setter.accept(n.floatValue()));
    }

    private double getMappedValue() {
        return min + value * (max - min);
    }

    @Override
    protected void updateMessage() {
        if (isInt) {
            setMessage(Text.literal(label + ": " + (int) Math.round(getMappedValue())));
        } else {
            setMessage(Text.literal(label + ": " + String.format("%.1f", getMappedValue())));
        }
    }

    @Override
    protected void applyValue() {
        if (isInt) {
            setter.accept((int) Math.round(getMappedValue()));
        } else {
            setter.accept((float) getMappedValue());
        }
    }
}
