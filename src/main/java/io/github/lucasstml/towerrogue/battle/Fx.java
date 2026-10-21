package io.github.lucasstml.towerrogue.battle;

/**
 * A purely cosmetic notification from the simulation to the renderer ("something exploded
 * here"). The simulation never reads these back, so visuals can change freely without
 * affecting determinism, and headless runs simply discard them.
 *
 * <p>Positions are in tiles. {@code x2/y2} is a second point (chain arcs, blinks), {@code value}
 * a magnitude (damage, radius).
 */
public record Fx(Type type, double x, double y, double x2, double y2, double value, int color, String text) {

    public enum Type {
        HIT, CRIT, DEATH, TEXT, CHAIN, PULSE, EXPLOSION, LEAK, MANA, SHOCKWAVE, PHASE, SHATTER, HEAL, BLINK, SPAWN, MUZZLE
    }

    public static Fx at(Type type, double x, double y, double value, int color) {
        return new Fx(type, x, y, x, y, value, color, null);
    }

    public static Fx text(double x, double y, String text, int color) {
        return new Fx(Type.TEXT, x, y, x, y, 0, color, text);
    }

    public static Fx line(Type type, double x, double y, double x2, double y2, int color) {
        return new Fx(type, x, y, x2, y2, 0, color, null);
    }
}
