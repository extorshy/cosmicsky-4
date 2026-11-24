package com.example.skystudio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Sky Studio settings screen. Everything is clickable: tabs, toggles, sliders, RGB colour
 * sliders with a live swatch. Does not pause the game, so you see the sky change live.
 * Opens with the key bound in Options > Controls.
 */
public class SkyScreen extends Screen {
    private enum Tab { TIME, SUN_SKY, CLOUDS, STARS, GALAXIES, SOLAR }
    private static Tab tab = Tab.TIME;
    private int galaxyIdx = 0, planetIdx = 0;
    private int left, top;
    /** swatches drawn in extract: {x, y, colourSupplier} */
    private final java.util.List<Object[]> swatches = new java.util.ArrayList<>();

    public SkyScreen() { super(Component.literal("Sky Studio")); }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { SkyConfig.save(); super.onClose(); }

    @Override
    protected void init() {
        clearWidgets();
        swatches.clear();
        SkyConfig c = SkyConfig.INSTANCE;
        left = this.width / 2 - 150;
        top = 40;

        // tab bar
        Tab[] tabs = Tab.values();
        String[] names = {"Time", "Sun & Sky", "Clouds", "Stars", "Galaxies", "Planets"};
        int tw = 300 / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            final Tab t = tabs[i];
            Button b = Button.builder(Component.literal(names[i]), x -> { tab = t; init(); })
                    .bounds(left + i * tw, 12, tw - 2, 20).build();
            b.active = tab != t;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), x -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());

        int y = top;
        switch (tab) {
            case TIME -> {
                addToggle(y, "Follow real time", () -> c.realTime, v -> { c.realTime = v; if (v) c.useSetTime = false; init(); }); y += 24;
                addToggle(y, "Use custom time", () -> c.useSetTime, v -> { c.useSetTime = v; if (v) c.realTime = false; init(); }); y += 24;
                addSlider(y, 300, "Time of day", 0, 23999, () -> c.setTimeTicks,
                        v -> { c.setTimeTicks = (int) v; c.useSetTime = true; c.realTime = false; },
                        v -> SkyConfig.describeTicks((int) v)); y += 24;
                addRow(y, "Morning", () -> preset(0), "Noon", () -> preset(6000),
                        "Sunset", () -> preset(12000), "Midnight", () -> preset(18000));
            }
            case SUN_SKY -> {
                y = colorRow(y, "Sun", () -> c.sunColor, v -> c.sunColor = v);
                y = colorRow(y, "Moon", () -> c.moonColor, v -> c.moonColor = v);
                y = colorRow(y, "Sky", () -> c.skyColor, v -> c.skyColor = v);
                y = colorRow(y, "Horizon", () -> c.horizonColor, v -> c.horizonColor = v);
                addToggle(y, "Sun colour tints surface blocks", () -> c.tintSurface, v -> c.tintSurface = v); y += 24;
                addSlider(y, 300, "Tint strength", 0, 1, () -> c.tintStrength, v -> c.tintStrength = (float) v,
                        v -> String.format("%.0f%%", v * 100));
            }
            case CLOUDS -> {
                addToggle(y, "Realistic clouds", () -> c.realisticClouds, v -> c.realisticClouds = v); y += 24;
                y = colorRow(y, "Clouds", () -> c.cloudColor, v -> c.cloudColor = v);
                addSlider(y, 300, "Coverage", 0, 1, () -> c.cloudCoverage, v -> c.cloudCoverage = (float) v,
                        v -> String.format("%.0f%%", v * 100)); y += 24;
                addSlider(y, 300, "Height", 100, 320, () -> c.cloudHeight, v -> c.cloudHeight = (float) v,
                        v -> String.format("%.0f", v));
            }
            case STARS -> {
                addToggle(y, "Stars", () -> c.stars, v -> c.stars = v); y += 24;
                y = colorRow(y, "Stars", () -> c.starColor, v -> c.starColor = v);
                addSlider(y, 300, "Star count", 200, 6000, () -> c.starCount, v -> c.starCount = (int) v,
                        v -> String.format("%.0f", v)); y += 28;
                addToggle(y, "Shooting stars", () -> c.shootingStars, v -> c.shootingStars = v); y += 24;
                y = colorRow(y, "Shooting", () -> c.shootingStarColor, v -> c.shootingStarColor = v);
                addSlider(y, 300, "Per minute", 0, 30, () -> c.shootingStarsPerMinute,
                        v -> c.shootingStarsPerMinute = (float) v, v -> String.format("%.0f", v));
            }
            case GALAXIES -> {
                addRow(y, "< Prev", () -> { galaxyIdx = Math.max(0, galaxyIdx - 1); init(); },
                        "Next >", () -> { galaxyIdx = Math.min(c.galaxies.size() - 1, galaxyIdx + 1); init(); },
                        "Add", () -> { c.galaxies.add(new SkyConfig.Galaxy("Galaxy " + (c.galaxies.size() + 1),
                                0xFFFFFF, 0.5f, 90f, 30f, 10f)); galaxyIdx = c.galaxies.size() - 1; init(); },
                        "Delete", () -> { if (!c.galaxies.isEmpty()) { c.galaxies.remove(galaxyIdx);
                                galaxyIdx = Math.max(0, Math.min(galaxyIdx, c.galaxies.size() - 1)); init(); } });
                y += 24;
                if (c.galaxies.isEmpty()) break;
                SkyConfig.Galaxy g = c.galaxies.get(Math.min(galaxyIdx, c.galaxies.size() - 1));
                y += 12; // room for name label
                y = colorRow(y, "Colour", () -> g.color, v -> g.color = v);
                addSlider(y, 300, "Brightness", 0, 1, () -> g.brightness, v -> g.brightness = (float) v, v -> String.format("%.0f%%", v * 100)); y += 22;
                addSlider(y, 300, "Azimuth", 0, 360, () -> g.azimuth, v -> g.azimuth = (float) v, v -> String.format("%.0f°", v)); y += 22;
                addSlider(y, 300, "Elevation", -90, 90, () -> g.elevation, v -> g.elevation = (float) v, v -> String.format("%.0f°", v)); y += 22;
                addSlider(y, 300, "Size", 1, 90, () -> g.size, v -> g.size = (float) v, v -> String.format("%.0f°", v));
            }
            case SOLAR -> {
                addToggle(y, "Show solar system", () -> c.solarSystem, v -> c.solarSystem = v); y += 24;
                addSlider(y, 300, "Scale", 0.25, 3, () -> c.solarSystemScale, v -> c.solarSystemScale = (float) v,
                        v -> String.format("%.2fx", v)); y += 24;
                addRow(y, "< Prev", () -> { planetIdx = Math.max(0, planetIdx - 1); init(); },
                        "Next >", () -> { planetIdx = Math.min(c.planets.size() - 1, planetIdx + 1); init(); });
                y += 24;
                SkyConfig.Planet p = c.planets.get(Math.min(planetIdx, c.planets.size() - 1));
                y += 12;
                y = colorRow(y, "Colour", () -> p.color, v -> p.color = v);
                addSlider(y, 300, "Size", 0.2, 3, () -> p.size, v -> p.size = (float) v, v -> String.format("%.1f", v));
            }
        }
    }

    private static void preset(int ticks) {
        SkyConfig c = SkyConfig.INSTANCE;
        c.setTimeTicks = ticks; c.useSetTime = true; c.realTime = false;
    }

    // ---------------- widget helpers ----------------

    private void addToggle(int y, String label, java.util.function.BooleanSupplier get,
                           java.util.function.Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(toggleText(label, get.getAsBoolean()), b -> {
            boolean v = !get.getAsBoolean();
            set.accept(v);
            b.setMessage(toggleText(label, v));
        }).bounds(left, y, 300, 20).build());
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    private void addRow(int y, Object... labelsAndActions) {
        int n = labelsAndActions.length / 2, w = 300 / n;
        for (int i = 0; i < n; i++) {
            final Runnable r = (Runnable) labelsAndActions[i * 2 + 1];
            addRenderableWidget(Button.builder(Component.literal((String) labelsAndActions[i * 2]),
                    b -> { r.run(); init(); })
                    .bounds(left + i * w, y, w - 2, 20).build());
        }
    }

    private void addSlider(int y, int w, String label, double min, double max, DoubleSupplier get,
                           DoubleConsumer set, java.util.function.DoubleFunction<String> fmt) {
        addRenderableWidget(new ValueSlider(left, y, w, 20, label, min, max, get.getAsDouble(), set, fmt));
    }

    /** One colour = three R/G/B sliders + a live swatch. Returns the next free y. */
    private int colorRow(int y, String label, IntSupplier get, IntConsumer set) {
        int[] rgb = {(get.getAsInt() >> 16) & 255, (get.getAsInt() >> 8) & 255, get.getAsInt() & 255};
        String[] ch = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            addRenderableWidget(new ValueSlider(left + 50 + i * 80, y, 78, 20, ch[i], 0, 255, rgb[i], v -> {
                rgb[idx] = (int) v;
                set.accept((rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
            }, v -> String.format("%.0f", v)));
        }
        swatches.add(new Object[]{left + 292, y, label, get});
        return y + 24;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 2, 0xFFFFFFFF);
        for (Object[] s : swatches) {
            int x = (int) s[0], y = (int) s[1];
            g.text(this.font, (String) s[2], left, y + 6, 0xFFFFFFFF);
            // swatch to the right of the sliders
            g.fill(left + 292 - 6, y + 2, left + 292 + 10, y + 18, 0xFF000000 | ((IntSupplier) s[3]).getAsInt());
        }
        SkyConfig c = SkyConfig.INSTANCE;
        if (tab == Tab.GALAXIES && !c.galaxies.isEmpty())
            g.text(this.font, c.galaxies.get(Math.min(galaxyIdx, c.galaxies.size() - 1)).name + "  (" + (galaxyIdx + 1) + "/" + c.galaxies.size() + ")", left, top + 28, 0xFFFFFF55);
        if (tab == Tab.SOLAR)
            g.text(this.font, c.planets.get(Math.min(planetIdx, c.planets.size() - 1)).name, left, top + 76, 0xFFFFFF55);
    }

    /** Slider that shows "Label: value" and writes straight into the config. */
    private static class ValueSlider extends AbstractSliderButton {
        private final String label; private final double min, max;
        private final DoubleConsumer set; private final java.util.function.DoubleFunction<String> fmt;

        ValueSlider(int x, int y, int w, int h, String label, double min, double max, double value,
                    DoubleConsumer set, java.util.function.DoubleFunction<String> fmt) {
            super(x, y, w, h, Component.empty(), (value - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.set = set; this.fmt = fmt;
            updateMessage();
        }
        private double real() { return min + this.value * (max - min); }
        @Override protected void updateMessage() { setMessage(Component.literal(label + ": " + fmt.apply(real()))); }
        @Override protected void applyValue() { set.accept(real()); }
    }
}
