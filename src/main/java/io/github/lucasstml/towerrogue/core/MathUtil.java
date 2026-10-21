package io.github.lucasstml.towerrogue.core;

/** Small numeric helpers shared by the simulation and the renderer. */
public final class MathUtil {
    private MathUtil() {
    }

    public static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double dist(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double dist2(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /** Exponential approach used for smooth UI animation, frame-rate independent. */
    public static double approach(double current, double target, double rate, double dt) {
        return target + (current - target) * Math.exp(-rate * dt);
    }
}
