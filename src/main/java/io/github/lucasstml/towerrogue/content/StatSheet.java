package io.github.lucasstml.towerrogue.content;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Final, computed tower stats. Immutable after {@link #compute}. */
public final class StatSheet {
    private static final Stat[] STATS = Stat.values();
    private final double[] values;

    private StatSheet(double[] values) {
        this.values = values;
    }

    public double get(Stat s) {
        return values[s.ordinal()];
    }

    public boolean flag(Stat s) {
        return values[s.ordinal()] > 0.5;
    }

    /** Applies modifiers on top of base values using the ADD → PCT → MUL (or SET) rule. */
    public static StatSheet compute(Map<Stat, Double> base, List<StatMod> mods) {
        int n = STATS.length;
        double[] add = new double[n];
        double[] pct = new double[n];
        double[] mul = new double[n];
        double[] set = new double[n];
        boolean[] hasSet = new boolean[n];
        Arrays.fill(mul, 1.0);
        for (StatMod m : mods) {
            int i = m.stat().ordinal();
            switch (m.op()) {
                case ADD -> add[i] += m.value();
                case PCT -> pct[i] += m.value();
                case MUL -> mul[i] *= m.value();
                case SET -> {
                    set[i] = m.value();
                    hasSet[i] = true;
                }
            }
        }
        double[] v = new double[n];
        for (Stat s : STATS) {
            int i = s.ordinal();
            double b = base.getOrDefault(s, s.defaultValue);
            v[i] = hasSet[i] ? set[i] : (b + add[i]) * Math.max(0, 1 + pct[i]) * mul[i];
        }
        v[Stat.ATTACK_SPEED.ordinal()] = Math.max(0.1, v[Stat.ATTACK_SPEED.ordinal()]);
        v[Stat.COOLDOWN.ordinal()] = Math.max(0.05, v[Stat.COOLDOWN.ordinal()]);
        v[Stat.RANGE.ordinal()] = Math.max(0.5, v[Stat.RANGE.ordinal()]);
        v[Stat.CRIT_CHANCE.ordinal()] = Math.min(1, Math.max(0, v[Stat.CRIT_CHANCE.ordinal()]));
        v[Stat.MULTISHOT.ordinal()] = Math.max(1, Math.floor(v[Stat.MULTISHOT.ordinal()] + 1e-9));
        v[Stat.CHAIN.ordinal()] = Math.max(0, Math.floor(v[Stat.CHAIN.ordinal()] + 1e-9));
        return new StatSheet(v);
    }

    /** Seconds between attacks after attack speed. */
    public double effectiveCooldown() {
        return get(Stat.COOLDOWN) / get(Stat.ATTACK_SPEED);
    }
}
