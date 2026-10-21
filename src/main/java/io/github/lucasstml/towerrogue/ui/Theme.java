package io.github.lucasstml.towerrogue.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Palette and fonts. One place to restyle the whole game. */
public final class Theme {
    public static final Color BG = new Color(0x10131C);
    public static final Color BG_ALT = new Color(0x161A26);
    public static final Color PANEL = new Color(0x1C2130);
    public static final Color PANEL_LIGHT = new Color(0x262C3F);
    public static final Color BORDER = new Color(0x3A4260);
    public static final Color BORDER_LIGHT = new Color(0x56608A);
    public static final Color TEXT = new Color(0xE9E6DF);
    public static final Color TEXT_DIM = new Color(0x9AA1B8);
    public static final Color TEXT_FAINT = new Color(0x646B84);
    public static final Color GOLD = new Color(0xF2C14E);
    public static final Color MANA = new Color(0x5AB0FF);
    public static final Color HP = new Color(0xE05561);
    public static final Color GOOD = new Color(0x6AD182);
    public static final Color BAD = new Color(0xE05561);
    public static final Color ELITE = new Color(0xF2A93B);
    public static final Color BOSS = new Color(0xC56CF0);

    private static final String SANS = pick("Segoe UI", "Helvetica Neue", "Arial", "SansSerif");
    private static final String SERIF = pick("Georgia", "Palatino Linotype", "Times New Roman", "Serif");
    private static final Map<Integer, Font> CACHE = new HashMap<>();

    private Theme() {
    }

    private static String pick(String... families) {
        Set<String> available;
        try {
            available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (Throwable t) {
            return families[families.length - 1];
        }
        for (String f : families) if (available.contains(f)) return f;
        return families[families.length - 1];
    }

    public static Font font(int size) {
        return get(SANS, Font.PLAIN, size, 0);
    }

    public static Font bold(int size) {
        return get(SANS, Font.BOLD, size, 1);
    }

    public static Font title(int size) {
        return get(SERIF, Font.BOLD, size, 2);
    }

    private static Font get(String family, int style, int size, int slot) {
        return CACHE.computeIfAbsent(size * 4 + slot, k -> new Font(family, style, size));
    }

    public static Color rgb(int rgb) {
        return new Color(rgb);
    }

    public static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.max(0, Math.min(255, a * 255)));
    }

    public static Color alpha(int rgb, double a) {
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) Math.max(0, Math.min(255, a * 255)));
    }

    public static Color shade(Color c, double f) {
        if (f < 1) {
            return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f), c.getAlpha());
        }
        double t = f - 1;
        return new Color((int) (c.getRed() + (255 - c.getRed()) * t), (int) (c.getGreen() + (255 - c.getGreen()) * t),
                (int) (c.getBlue() + (255 - c.getBlue()) * t), c.getAlpha());
    }
}
