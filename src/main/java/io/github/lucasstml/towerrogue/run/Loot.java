package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.Rarity;
import io.github.lucasstml.towerrogue.content.RelicDef;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.core.Rng;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Shared random-pick helpers for rewards, shops and events. */
final class Loot {
    private static final Rarity[] DRAFT_RARITIES = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE};

    private Loot() {
    }

    /** @param weights [common, uncommon, rare] */
    static Rarity rollRarity(Rng rng, double[] weights) {
        double total = 0;
        for (double w : weights) total += Math.max(0, w);
        double roll = rng.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= Math.max(0, weights[i]);
            if (roll < 0) return DRAFT_RARITIES[i];
        }
        return Rarity.COMMON;
    }

    /**
     * A tower from the run's pool not yet owned or excluded, preferring {@code rarity}
     * and falling back to the closest other rarity. Null if nothing is left.
     */
    static String pickTower(RunState run, Content content, Rarity rarity, Set<String> exclude, Rng rng) {
        for (Rarity r : fallbackOrder(rarity)) {
            List<String> c = new ArrayList<>();
            for (String id : run.towerPool) {
                TowerDef d = content.tower(id);
                if (d.rarity() == r && !run.owns(id) && !exclude.contains(id)) c.add(id);
            }
            if (!c.isEmpty()) return rng.pick(c);
        }
        return null;
    }

    static String pickRelic(RunState run, Content content, Rarity rarity, Set<String> exclude, Rng rng) {
        List<Rarity> order = rarity == Rarity.BOSS ? List.of(Rarity.BOSS, Rarity.RARE) : fallbackOrder(rarity);
        for (Rarity r : order) {
            List<String> c = new ArrayList<>();
            for (String id : run.relicPool) {
                RelicDef d = content.relic(id);
                if (d.rarity() == r && !run.relics.contains(id) && !exclude.contains(id)) c.add(id);
            }
            if (!c.isEmpty()) return rng.pick(c);
        }
        return null;
    }

    private static List<Rarity> fallbackOrder(Rarity r) {
        return switch (r) {
            case RARE -> List.of(Rarity.RARE, Rarity.UNCOMMON, Rarity.COMMON);
            case UNCOMMON -> List.of(Rarity.UNCOMMON, Rarity.RARE, Rarity.COMMON);
            default -> List.of(Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE);
        };
    }
}
