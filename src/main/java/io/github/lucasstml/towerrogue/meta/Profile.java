package io.github.lucasstml.towerrogue.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persistent player profile: everything that survives between runs. */
public final class Profile {
    public static final int HISTORY_SIZE = 12;

    /** One finished run, shown on the history panel. */
    public record HistoryEntry(String seed, int ascension, boolean won, int act, int floors, int essence, String date) {
    }

    public int essence;
    public int totalEssence;
    public final Set<String> unlocked = new LinkedHashSet<>();
    public final Map<String, Integer> perks = new LinkedHashMap<>();
    /** Highest Ascension level the player may select. */
    public int maxAscension;
    public int runs;
    public int wins;
    public int bestFloor;
    public int totalKills;
    public int bossesKilled;
    public final List<HistoryEntry> history = new ArrayList<>();

    public int perkLevel(String id) {
        return perks.getOrDefault(id, 0);
    }
}
