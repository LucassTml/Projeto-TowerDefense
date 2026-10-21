package io.github.lucasstml.towerrogue.battle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Precomputed timeline of one wave: who spawns, when, on which lane, with which affixes. */
public final class WavePlan {

    /** @param hpScale final HP multiplier (act, floor, ascension, relics) */
    public record Spawn(double time, String enemyId, int lane, List<String> affixes, double hpScale) {
    }

    /** Aggregated view for the "next wave" preview. */
    public record Group(String enemyId, int count, boolean elite) {
    }

    private final List<Spawn> spawns;

    WavePlan(List<Spawn> spawns) {
        List<Spawn> sorted = new ArrayList<>(spawns);
        sorted.sort((a, b) -> Double.compare(a.time(), b.time()));
        this.spawns = List.copyOf(sorted);
    }

    public List<Spawn> spawns() {
        return spawns;
    }

    public List<Group> preview() {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (Spawn s : spawns) {
            String key = s.enemyId() + (s.affixes().isEmpty() ? "" : "*");
            counts.computeIfAbsent(key, k -> new int[1])[0]++;
        }
        List<Group> out = new ArrayList<>();
        counts.forEach((k, v) -> {
            boolean elite = k.endsWith("*");
            out.add(new Group(elite ? k.substring(0, k.length() - 1) : k, v[0], elite));
        });
        return out;
    }
}
