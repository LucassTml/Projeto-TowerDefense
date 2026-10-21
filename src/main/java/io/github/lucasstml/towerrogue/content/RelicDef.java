package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.List;
import java.util.Locale;

/**
 * A passive item. Its behaviour is a list of {@link EffectDef effects} that
 * {@code relic.RelicEffects} turns into code: static stat/rule modifiers and/or
 * subscriptions to battle events.
 */
public record RelicDef(String id, String name, String description, Rarity rarity, String glyph, int color,
                       List<EffectDef> effects, boolean locked, int unlockCost) {

    public static RelicDef parse(JsonObject o) {
        return new RelicDef(
                o.getString("id"),
                o.getString("name"),
                o.getString("description"),
                Rarity.valueOf(o.getString("rarity").toUpperCase(Locale.ROOT)),
                o.getString("glyph", o.getString("name").substring(0, 1)),
                ContentLoader.parseColor(o.getString("color", "#f2c14e")),
                EffectDef.parseList(o.getArray("effects")),
                o.getBool("locked", false),
                o.getInt("unlockCost", 0));
    }
}
