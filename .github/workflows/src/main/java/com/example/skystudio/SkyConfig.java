package com.example.skystudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All user settings. Colours are 0xRRGGBB ints. Saved to config/skystudio.json. */
public class SkyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skystudio.json");
    public static SkyConfig INSTANCE = new SkyConfig();

    // ---- time ----
    public boolean realTime = false;     // follow your computer clock
    public boolean useSetTime = false;   // freeze at setTimeTicks
    public int setTimeTicks = 6000;      // 0=sunrise, 6000=noon, 12000=sunset, 18000=midnight

    // ---- sun / moon ----
    public int sunColor = 0xFFF2C0;
    public int moonColor = 0xDDE6FF;
    public boolean tintSurface = true;   // sun colour tints surface-exposed blocks
    public float tintStrength = 0.6f;

    // ---- sky ----
    public int skyColor = 0x78A7FF;
    public int horizonColor = 0xC0D8FF;
    public int starColor = 0xFFFFFF;
    public boolean stars = true;
    public int starCount = 1500;

    public boolean shootingStars = true;
    public int shootingStarColor = 0xFFFFFF;
    public float shootingStarsPerMinute = 6f;

    // ---- clouds ----
    public boolean realisticClouds = true;
    public int cloudColor = 0xFFFFFF;
    public float cloudCoverage = 0.5f;   // 0..1
    public float cloudHeight = 192f;

    // ---- solar system ----
    public boolean solarSystem = false;
    public float solarSystemScale = 1f;
    public List<Planet> planets = defaultPlanets();

    // ---- galaxies ----
    public List<Galaxy> galaxies = new ArrayList<>(List.of(
            new Galaxy("Andromeda", 0xB8A8FF, 0.5f, 40f, 25f, 12f),
            new Galaxy("Spiral", 0xFFB8D8, 0.4f, 200f, 50f, 9f)));

    public static class Galaxy {
        public String name; public int color; public float brightness;
        public float azimuth, elevation, size; // degrees
        public Galaxy() {}
        public Galaxy(String n, int c, float b, float az, float el, float s) {
            name = n; color = c; brightness = b; azimuth = az; elevation = el; size = s;
        }
    }

    public static class Planet {
        public String name; public int color; public float size, orbitDays;
        public Planet() {}
        public Planet(String n, int c, float s, float o) { name = n; color = c; size = s; orbitDays = o; }
    }

    private static List<Planet> defaultPlanets() {
        return new ArrayList<>(List.of(
                new Planet("Mercury", 0xB5B5B5, 0.4f, 88f),
                new Planet("Venus",   0xE8CDA2, 0.7f, 225f),
                new Planet("Earth",   0x4F8FE8, 0.7f, 365f),
                new Planet("Mars",    0xD1603D, 0.5f, 687f),
                new Planet("Jupiter", 0xD9B38C, 1.6f, 4333f),
                new Planet("Saturn",  0xEAD6A6, 1.4f, 10759f),
                new Planet("Uranus",  0x9FE3E8, 1.0f, 30687f),
                new Planet("Neptune", 0x4B70DD, 1.0f, 60190f)));
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) INSTANCE = GSON.fromJson(Files.readString(FILE), SkyConfig.class);
            if (INSTANCE == null) INSTANCE = new SkyConfig();
        } catch (Exception e) {
            SkyStudioClient.LOGGER.warn("Could not read config, using defaults", e);
            INSTANCE = new SkyConfig();
        }
        save();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(INSTANCE)); }
        catch (Exception e) { SkyStudioClient.LOGGER.warn("Could not save config", e); }
    }


    /** Tick-of-day to show on this client, or null to use the world's real time. */
    public static Long currentOverrideTicks() {
        SkyConfig c = INSTANCE;
        if (c.realTime) {
            java.time.LocalTime t = java.time.LocalTime.now();
            double h = t.getHour() + t.getMinute() / 60.0 + t.getSecond() / 3600.0;
            return Math.floorMod(Math.round((h - 6.0) * 1000.0), 24000L);
        }
        if (c.useSetTime) return (long) Math.floorMod(c.setTimeTicks, 24000);
        return null;
    }

    public static String describeTicks(int ticks) {
        int mins = (int) Math.floorMod((ticks / 1000.0 + 6.0) * 60.0, 1440.0);
        String phase = ticks < 1000 ? "Sunrise" : ticks < 5000 ? "Morning" : ticks < 7000 ? "Noon"
                : ticks < 11000 ? "Afternoon" : ticks < 13000 ? "Sunset" : ticks < 17000 ? "Night"
                : ticks < 19000 ? "Midnight" : ticks < 23000 ? "Late night" : "Dawn";
        return String.format("%02d:%02d  %s", mins / 60, mins % 60, phase);
    }

    /** Parses "#RRGGBB", "RRGGBB" or "0xRRGGBB". Returns -1 on failure. */
    public static int parseColor(String s) {
        try {
            s = s.trim().replace("#", "").replace("0x", "");
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (Exception e) { return -1; }
    }
}
