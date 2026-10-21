package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A status effect applied when an attack lands: {@code {"status":"burn","magnitude":4,"duration":3}}. */
public record OnHitDef(StatusType status, double magnitude, double duration, double chance) {

    public static OnHitDef parse(JsonObject o) {
        return new OnHitDef(
                StatusType.valueOf(o.getString("status").toUpperCase(Locale.ROOT)),
                o.getDouble("magnitude", 0),
                o.getDouble("duration", 0),
                o.getDouble("chance", 1.0));
    }

    public static List<OnHitDef> parseList(JsonArray arr) {
        List<OnHitDef> out = new ArrayList<>();
        for (JsonObject o : arr.objects()) out.add(parse(o));
        return List.copyOf(out);
    }

    public String describe() {
        String c = chance < 1 ? Math.round(chance * 100) + "% chance: " : "";
        return c + switch (status) {
            case SLOW -> "Slow " + Math.round(magnitude * 100) + "% for " + StatMod.trim(duration) + "s";
            case FREEZE -> "Freeze for " + StatMod.trim(duration) + "s";
            case BURN -> "Burn " + StatMod.trim(magnitude) + "/s per stack";
            case POISON -> "Poison " + StatMod.trim(magnitude) + "/s per stack, ignores armor";
            case SHRED -> "Shred " + StatMod.trim(magnitude) + " armor per stack";
            case VULNERABLE -> "Vulnerable: +" + Math.round(magnitude * 100) + "% damage taken";
        };
    }
}
