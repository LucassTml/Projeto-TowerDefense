package io.github.lucasstml.towerrogue.content;

import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Global tuning numbers from {@code balance.json}. Kept in one file so balancing is one place. */
public final class Balance {
    public final int startHp;
    public final int startGold;
    public final List<String> starterTowers;
    public final int waveClearMana;
    public final int waveClearManaPerWave;
    public final int earlyCallMana;
    public final double sellRefund;
    public final int burnMaxStacks;
    public final int poisonMaxStacks;
    public final int shredMaxStacks;
    /** Modifiers granted by blueprint tier 1, 2... (cumulative). */
    public final List<List<StatMod>> blueprintTiers;
    public final List<StatMod> runeTileMods;
    public final double restHealPct;
    public final double actHealPct;
    public final Map<EncounterKind, int[]> goldReward;
    /** Weights for [COMMON, UNCOMMON, RARE] per encounter kind. */
    public final Map<EncounterKind, double[]> rarityWeights;
    public final double rarePityStep;
    public final int draftChoices;
    public final double upgradeOfferChance;
    public final Map<Rarity, Integer> towerPrice;
    public final Map<Rarity, Integer> relicPrice;
    public final int forgePriceBase;
    public final int forgePriceStep;
    public final int healPrice;
    public final double healServicePct;
    public final int essencePerFloor;
    public final int essencePerElite;
    public final int essencePerBoss;
    public final int essenceWin;
    public final double essenceAscensionBonus;
    public final List<AscensionLevel> ascension;

    public record AscensionLevel(int level, String text, JsonObject effects) {
    }

    public Balance(JsonObject o) {
        startHp = o.getInt("startHp");
        startGold = o.getInt("startGold");
        starterTowers = List.copyOf(o.getArray("starterTowers").strings());
        JsonObject battle = o.getObject("battle");
        waveClearMana = battle.getInt("waveClearMana");
        waveClearManaPerWave = battle.getInt("waveClearManaPerWave");
        earlyCallMana = battle.getInt("earlyCallMana");
        sellRefund = battle.getDouble("sellRefund");
        burnMaxStacks = battle.getInt("burnMaxStacks");
        poisonMaxStacks = battle.getInt("poisonMaxStacks");
        shredMaxStacks = battle.getInt("shredMaxStacks");
        runeTileMods = StatMod.parseList(battle.getArray("runeTileMods"));

        List<List<StatMod>> tiers = new ArrayList<>();
        for (Object t : o.getArray("blueprintTiers")) tiers.add(StatMod.parseList((JsonArray) t));
        blueprintTiers = List.copyOf(tiers);

        JsonObject run = o.getObject("run");
        restHealPct = run.getDouble("restHealPct");
        actHealPct = run.getDouble("actHealPct");

        JsonObject rewards = o.getObject("rewards");
        goldReward = new EnumMap<>(EncounterKind.class);
        rarityWeights = new EnumMap<>(EncounterKind.class);
        for (EncounterKind k : EncounterKind.values()) {
            String key = k.name().toLowerCase(Locale.ROOT);
            JsonArray g = rewards.getObject("gold").getArray(key);
            goldReward.put(k, new int[]{g.getInt(0), g.getInt(1)});
            JsonArray w = rewards.getObject("rarityWeights").getArray(key);
            rarityWeights.put(k, new double[]{w.getDouble(0), w.getDouble(1), w.getDouble(2)});
        }
        rarePityStep = rewards.getDouble("rarePityStep");
        draftChoices = rewards.getInt("draftChoices");
        upgradeOfferChance = rewards.getDouble("upgradeOfferChance");

        JsonObject shop = o.getObject("shop");
        towerPrice = rarityMap(shop.getObject("towerPrice"));
        relicPrice = rarityMap(shop.getObject("relicPrice"));
        forgePriceBase = shop.getInt("forgePriceBase");
        forgePriceStep = shop.getInt("forgePriceStep");
        healPrice = shop.getInt("healPrice");
        healServicePct = shop.getDouble("healServicePct");

        JsonObject ess = o.getObject("essence");
        essencePerFloor = ess.getInt("perFloor");
        essencePerElite = ess.getInt("perElite");
        essencePerBoss = ess.getInt("perBoss");
        essenceWin = ess.getInt("win");
        essenceAscensionBonus = ess.getDouble("ascensionBonus");

        List<AscensionLevel> asc = new ArrayList<>();
        for (JsonObject a : o.getArray("ascension").objects()) {
            asc.add(new AscensionLevel(a.getInt("level"), a.getString("text"), a.getObject("effects")));
        }
        ascension = List.copyOf(asc);
    }

    private static Map<Rarity, Integer> rarityMap(JsonObject o) {
        Map<Rarity, Integer> m = new EnumMap<>(Rarity.class);
        for (String k : o.keys()) m.put(Rarity.valueOf(k.toUpperCase(Locale.ROOT)), o.getInt(k));
        return m;
    }

    public int maxAscension() {
        return ascension.size();
    }
}
