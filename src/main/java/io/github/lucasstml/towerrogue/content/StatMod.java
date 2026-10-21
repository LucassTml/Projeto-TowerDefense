package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One modification of a {@link Stat}. JSON forms:
 * {@code {"stat":"damage","add":4}}, {@code {"stat":"damage","pct":0.2}},
 * {@code {"stat":"damage","mul":1.5}}, {@code {"stat":"hit_air","set":1}}.
 *
 * <p>Stacking rule (see {@link StatSheet#compute}):
 * {@code final = (base + ΣADD) × (1 + ΣPCT) × ΠMUL}, unless a SET overrides it.
 * Summing percentages keeps many small bonuses predictable, while MUL is reserved for
 * rare, deliberately strong effects.
 */
public record StatMod(Stat stat, Op op, double value) {

    public enum Op { ADD, PCT, MUL, SET }

    public static StatMod parse(JsonObject o) {
        Stat stat = Stat.byKey(o.getString("stat"));
        if (o.has("add")) return new StatMod(stat, Op.ADD, o.getDouble("add"));
        if (o.has("pct")) return new StatMod(stat, Op.PCT, o.getDouble("pct"));
        if (o.has("mul")) return new StatMod(stat, Op.MUL, o.getDouble("mul"));
        if (o.has("set")) return new StatMod(stat, Op.SET, o.getDouble("set"));
        throw new IllegalArgumentException("stat mod for '" + stat.key + "' needs add/pct/mul/set");
    }

    public static List<StatMod> parseList(JsonArray arr) {
        List<StatMod> out = new ArrayList<>();
        for (JsonObject o : arr.objects()) out.add(parse(o));
        return List.copyOf(out);
    }

    /** Human readable form for tooltips, e.g. "+20% Damage". */
    public String describe() {
        return switch (op) {
            case ADD -> (value >= 0 ? "+" : "") + trim(value) + " " + stat.label;
            case PCT -> (value >= 0 ? "+" : "") + Math.round(value * 100) + "% " + stat.label;
            case MUL -> "x" + trim(value) + " " + stat.label;
            case SET -> stat == Stat.HIT_AIR ? (value > 0 ? "Can hit flying" : "Cannot hit flying")
                    : stat.label + " = " + trim(value);
        };
    }

    static String trim(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(java.util.Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
