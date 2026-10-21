package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.List;

/** Permanent meta-progression upgrade bought with essence between runs. */
public record PerkDef(String id, String name, String description, String effect, double perLevel,
                      List<Integer> costs) {

    public int maxLevel() {
        return costs.size();
    }

    public static PerkDef parse(JsonObject o) {
        return new PerkDef(
                o.getString("id"),
                o.getString("name"),
                o.getString("description"),
                o.getString("effect"),
                o.getDouble("perLevel"),
                List.copyOf(o.getArray("costs").ints()));
    }
}
