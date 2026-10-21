package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One act of a run (biome). Instead of hand-authoring every wave, an act declares an
 * enemy pool with threat costs and a budget curve; {@code battle.WaveDirector} spends the
 * budget procedurally. Designers tune difficulty by editing numbers, not code.
 */
public record ActDef(
        int act,
        String name,
        String subtitle,
        int floors,
        int wavesNormal,
        int wavesElite,
        int wavesBoss,
        double budgetBase,
        double budgetPerWave,
        double budgetPerFloor,
        double hpMult,
        double hpPerFloor,
        double manaMult,
        double twoLaneChance,
        int startMana,
        List<PoolEntry> pool,
        List<String> elites,
        String boss,
        List<String> bossEscorts,
        Theme theme) {

    /** An enemy that can appear in this act's waves. {@code cost} is its threat value. */
    public record PoolEntry(String enemy, double cost, double weight, int minWave, int minFloor) {
    }

    public record Theme(int grass, int grassAlt, int path, int pathEdge, int rock, int accent) {
    }

    public int waves(EncounterKind kind) {
        return switch (kind) {
            case NORMAL -> wavesNormal;
            case ELITE -> wavesElite;
            case BOSS -> wavesBoss;
        };
    }

    public static ActDef parse(JsonObject o) {
        List<PoolEntry> pool = new ArrayList<>();
        for (JsonObject p : o.getArray("pool").objects()) {
            pool.add(new PoolEntry(p.getString("enemy"), p.getDouble("cost"), p.getDouble("weight", 1),
                    p.getInt("minWave", 0), p.getInt("minFloor", 0)));
        }
        JsonObject w = o.getObject("waves");
        JsonObject b = o.getObject("budget");
        JsonObject t = o.getObject("theme");
        Theme theme = new Theme(
                ContentLoader.parseColor(t.getString("grass")),
                ContentLoader.parseColor(t.getString("grassAlt")),
                ContentLoader.parseColor(t.getString("path")),
                ContentLoader.parseColor(t.getString("pathEdge")),
                ContentLoader.parseColor(t.getString("rock")),
                ContentLoader.parseColor(t.getString("accent")));
        return new ActDef(
                o.getInt("act"),
                o.getString("name"),
                o.getString("subtitle", ""),
                o.getInt("floors"),
                w.getInt("normal"),
                w.getInt("elite"),
                w.getInt("boss"),
                b.getDouble("base"),
                b.getDouble("perWave"),
                b.getDouble("perFloor"),
                o.getDouble("hpMult", 1),
                o.getDouble("hpPerFloor", 0.05),
                o.getDouble("manaMult", 1),
                o.getDouble("twoLaneChance", 0),
                o.getInt("startMana"),
                List.copyOf(pool),
                List.copyOf(o.getArray("elites").strings()),
                o.getString("boss"),
                List.copyOf(o.optArray("bossEscorts").strings()),
                theme);
    }
}
