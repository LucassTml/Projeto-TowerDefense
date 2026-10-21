package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.core.Rng;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete, serializable state of a run. Everything needed to resume exactly where the
 * player stopped is here, including the internal state of every random stream. The act
 * map is NOT stored: it is regenerated from the seed (determinism makes that safe).
 */
public final class RunState {
    public long seed;
    public int ascension;
    public int act = 1;
    public int currentNode = -1;
    public final List<Integer> visited = new ArrayList<>();
    public int hp;
    public int maxHp;
    public int gold;
    public final List<Blueprint> blueprints = new ArrayList<>();
    public final List<String> relics = new ArrayList<>();
    public RunPhase phase = RunPhase.MAP;

    /** Independent random streams, so e.g. shop rolls never shift event outcomes. */
    public final Rng rewardRng;
    public final Rng shopRng;
    public final Rng eventRng;

    public double rarePity;
    public int forgeUses;
    public int battleCounter;

    /** Snapshot of unlocked content and perks at run start (later unlocks don't affect a saved run). */
    public final List<String> towerPool = new ArrayList<>();
    public final List<String> relicPool = new ArrayList<>();
    public final Map<String, Integer> perks = new LinkedHashMap<>();
    public final List<String> seenEvents = new ArrayList<>();

    // ---- state of the node being resolved ----
    public EncounterKind battleKind = EncounterKind.NORMAL;
    public final List<RewardOption> rewards = new ArrayList<>();
    public int lastGoldReward;
    public ShopStock shop;
    public String eventId;
    public boolean eventResolved;
    public final List<String> eventLog = new ArrayList<>();
    public boolean eventBattlePending;

    // ---- statistics ----
    public int floorsCleared;
    public int kills;
    public int elitesKilled;
    public int bossesKilled;
    public int leaks;
    public long damage;

    public RunState(long seed) {
        this.seed = seed;
        this.rewardRng = Rng.derive(seed, "rewards");
        this.shopRng = Rng.derive(seed, "shop");
        this.eventRng = Rng.derive(seed, "events");
    }

    public Blueprint blueprint(String towerId) {
        for (Blueprint b : blueprints) if (b.towerId.equals(towerId)) return b;
        return null;
    }

    public boolean owns(String towerId) {
        return blueprint(towerId) != null;
    }

    public List<Blueprint> upgradeableBlueprints() {
        List<Blueprint> out = new ArrayList<>();
        for (Blueprint b : blueprints) if (b.canUpgrade()) out.add(b);
        return out;
    }

    public int perk(String id) {
        return perks.getOrDefault(id, 0);
    }
}
