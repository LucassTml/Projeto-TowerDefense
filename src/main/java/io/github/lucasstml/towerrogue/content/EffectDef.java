package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A data-driven behaviour reference: a {@code type} name plus free-form parameters.
 * Used for enemy abilities ({@code {"type":"summon","enemy":"spiderling","interval":4}})
 * and relic effects. Factories map the type name to a Java implementation.
 */
public record EffectDef(String type, JsonObject params) {

    public static EffectDef parse(JsonObject o) {
        return new EffectDef(o.getString("type"), o);
    }

    public static List<EffectDef> parseList(JsonArray arr) {
        List<EffectDef> out = new ArrayList<>();
        for (JsonObject o : arr.objects()) out.add(parse(o));
        return List.copyOf(out);
    }
}
