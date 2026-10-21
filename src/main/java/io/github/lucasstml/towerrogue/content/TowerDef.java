package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static description of a tower type, loaded from {@code towers.json}.
 * Runtime state (position, level, cooldown) lives in {@code battle.Tower}.
 *
 * @param upgrades in-battle level ups: index 0 takes the tower from level 1 to 2, and so on
 * @param branches mutually exclusive specializations unlocked at max level
 */
public record TowerDef(
        String id,
        String name,
        String description,
        Rarity rarity,
        int cost,
        List<String> tags,
        AttackKind kind,
        DamageType damageType,
        String art,
        int color,
        boolean homing,
        TargetMode defaultTarget,
        Map<Stat, Double> base,
        List<OnHitDef> onHit,
        List<Upgrade> upgrades,
        List<Branch> branches,
        boolean locked,
        int unlockCost) {

    public record Upgrade(int cost, List<StatMod> mods) {
    }

    public record Branch(String id, String name, String description, int cost,
                         List<StatMod> mods, List<OnHitDef> onHit) {
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public int maxLevel() {
        return 1 + upgrades.size();
    }

    public double baseStat(Stat s) {
        return base.getOrDefault(s, s.defaultValue);
    }

    public static TowerDef parse(JsonObject o) {
        Map<Stat, Double> base = new EnumMap<>(Stat.class);
        JsonObject stats = o.getObject("stats");
        for (String key : stats.keys()) base.put(Stat.byKey(key), stats.getDouble(key));

        List<Upgrade> ups = new ArrayList<>();
        for (JsonObject u : o.optArray("upgrades").objects()) {
            ups.add(new Upgrade(u.getInt("cost"), StatMod.parseList(u.getArray("mods"))));
        }
        List<Branch> branches = new ArrayList<>();
        for (JsonObject b : o.optArray("branches").objects()) {
            branches.add(new Branch(b.getString("id"), b.getString("name"), b.getString("description"),
                    b.getInt("cost"), StatMod.parseList(b.optArray("mods")), OnHitDef.parseList(b.optArray("onHit"))));
        }
        return new TowerDef(
                o.getString("id"),
                o.getString("name"),
                o.getString("description"),
                Rarity.valueOf(o.getString("rarity").toUpperCase(Locale.ROOT)),
                o.getInt("cost"),
                List.copyOf(o.optArray("tags").strings()),
                AttackKind.valueOf(o.getString("kind").toUpperCase(Locale.ROOT)),
                DamageType.valueOf(o.getString("damageType", "physical").toUpperCase(Locale.ROOT)),
                o.getString("art", o.getString("id")),
                ContentLoader.parseColor(o.getString("color", "#cccccc")),
                o.getBool("homing", true),
                TargetMode.valueOf(o.getString("target", "first").toUpperCase(Locale.ROOT)),
                Map.copyOf(base),
                OnHitDef.parseList(o.optArray("onHit")),
                List.copyOf(ups),
                List.copyOf(branches),
                o.getBool("locked", false),
                o.getInt("unlockCost", 0));
    }
}
