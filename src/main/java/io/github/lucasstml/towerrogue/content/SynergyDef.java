package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Board synergy, auto-battler style. Either counts towers with a {@code tag}
 * ("3 fire towers → burn +40%") or requires one of each tag in {@code requiresAll}
 * ("one tower of every element → +15% damage").
 */
public record SynergyDef(String id, String name, String tag, List<String> requiresAll, List<Tier> tiers) {

    /** @param scope "tag" (only towers with the synergy tag) or "all" */
    public record Tier(int count, String description, String scope, List<StatMod> mods) {
    }

    public static SynergyDef parse(JsonObject o) {
        List<Tier> tiers = new ArrayList<>();
        for (JsonObject t : o.getArray("tiers").objects()) {
            tiers.add(new Tier(t.getInt("count", 1), t.getString("description"),
                    t.getString("scope", "tag"), StatMod.parseList(t.getArray("mods"))));
        }
        return new SynergyDef(
                o.getString("id"),
                o.getString("name"),
                o.getString("tag", null),
                List.copyOf(o.optArray("requiresAll").strings()),
                List.copyOf(tiers));
    }
}
