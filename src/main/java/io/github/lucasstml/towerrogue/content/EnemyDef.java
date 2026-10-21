package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static description of an enemy type, loaded from {@code enemies.json}.
 * Behaviour beyond walking (summoning, shields, phases...) is composed from
 * {@link EffectDef abilities} instead of subclasses, so a new enemy is pure data.
 */
public record EnemyDef(
        String id,
        String name,
        String description,
        double hp,
        double speed,
        double armor,
        Map<DamageType, Double> resist,
        Map<StatusType, Double> statusResist,
        boolean flying,
        double radius,
        int color,
        EnemyShape shape,
        int leak,
        int mana,
        boolean boss,
        List<EffectDef> abilities) {

    public double resistance(DamageType t) {
        return resist.getOrDefault(t, 0.0);
    }

    public double statusResistance(StatusType t) {
        return statusResist.getOrDefault(t, 0.0);
    }

    public static EnemyDef parse(JsonObject o) {
        Map<DamageType, Double> res = new EnumMap<>(DamageType.class);
        JsonObject r = o.optObject("resist");
        for (String k : r.keys()) res.put(DamageType.valueOf(k.toUpperCase(Locale.ROOT)), r.getDouble(k));
        Map<StatusType, Double> sres = new EnumMap<>(StatusType.class);
        JsonObject sr = o.optObject("statusResist");
        for (String k : sr.keys()) sres.put(StatusType.valueOf(k.toUpperCase(Locale.ROOT)), sr.getDouble(k));
        return new EnemyDef(
                o.getString("id"),
                o.getString("name"),
                o.getString("description", ""),
                o.getDouble("hp"),
                o.getDouble("speed"),
                o.getDouble("armor", 0),
                Map.copyOf(res),
                Map.copyOf(sres),
                o.getBool("flying", false),
                o.getDouble("radius", 0.28),
                ContentLoader.parseColor(o.getString("color", "#cc4444")),
                EnemyShape.valueOf(o.getString("shape", "circle").toUpperCase(Locale.ROOT)),
                o.getInt("leak", 1),
                o.getInt("mana", 3),
                o.getBool("boss", false),
                EffectDef.parseList(o.optArray("abilities")));
    }
}
