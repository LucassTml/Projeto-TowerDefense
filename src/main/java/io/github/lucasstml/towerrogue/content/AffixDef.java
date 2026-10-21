package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Elite modifier ("Armored", "Swift"...) stacked on top of a normal enemy. */
public record AffixDef(
        String id,
        String name,
        String description,
        int color,
        double hpMult,
        double speedMult,
        double armorAdd,
        Map<StatusType, Double> statusResist,
        List<EffectDef> abilities) {

    public static AffixDef parse(JsonObject o) {
        Map<StatusType, Double> sres = new EnumMap<>(StatusType.class);
        JsonObject sr = o.optObject("statusResist");
        for (String k : sr.keys()) sres.put(StatusType.valueOf(k.toUpperCase(Locale.ROOT)), sr.getDouble(k));
        return new AffixDef(
                o.getString("id"),
                o.getString("name"),
                o.getString("description", ""),
                ContentLoader.parseColor(o.getString("color", "#ffd34d")),
                o.getDouble("hpMult", 1.0),
                o.getDouble("speedMult", 1.0),
                o.getDouble("armorAdd", 0),
                Map.copyOf(sres),
                EffectDef.parseList(o.optArray("abilities")));
    }
}
