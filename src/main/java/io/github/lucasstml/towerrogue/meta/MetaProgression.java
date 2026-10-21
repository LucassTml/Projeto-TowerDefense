package io.github.lucasstml.towerrogue.meta;

import io.github.lucasstml.towerrogue.content.Balance;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.PerkDef;
import io.github.lucasstml.towerrogue.content.RelicDef;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.core.SeedCodec;
import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunPhase;
import io.github.lucasstml.towerrogue.run.RunState;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Between-run progression: Essence earned per run, unlocks, perks and Ascension.
 * Only this class reads the profile when a run starts, and a run keeps a snapshot of what
 * it needs, so meta code never leaks into run logic.
 */
public final class MetaProgression {
    private MetaProgression() {
    }

    public static boolean isUnlocked(Profile p, TowerDef t) {
        return !t.locked() || p.unlocked.contains(t.id());
    }

    public static boolean isUnlocked(Profile p, RelicDef r) {
        return !r.locked() || p.unlocked.contains(r.id());
    }

    public static RunState startRun(Content content, Profile profile, long seed, int ascension) {
        List<String> towers = new ArrayList<>();
        for (TowerDef t : content.towers()) if (isUnlocked(profile, t)) towers.add(t.id());
        List<String> relics = new ArrayList<>();
        for (RelicDef r : content.relics()) if (isUnlocked(profile, r)) relics.add(r.id());
        int asc = Math.max(0, Math.min(ascension, profile.maxAscension));
        return RunController.create(content, seed, asc, towers, relics, profile.perks);
    }

    public static int essenceFor(Balance b, RunState run) {
        boolean won = run.phase == RunPhase.VICTORY;
        double base = run.floorsCleared * b.essencePerFloor
                + run.elitesKilled * b.essencePerElite
                + run.bossesKilled * b.essencePerBoss
                + (won ? b.essenceWin : 0);
        return (int) Math.round(base * (1 + b.essenceAscensionBonus * run.ascension));
    }

    /** Records a finished run into the profile and returns the essence earned. */
    public static int recordRun(Content content, Profile p, RunState run) {
        Balance b = content.balance();
        int essence = essenceFor(b, run);
        boolean won = run.phase == RunPhase.VICTORY;
        p.essence += essence;
        p.totalEssence += essence;
        p.runs++;
        if (won) p.wins++;
        p.bestFloor = Math.max(p.bestFloor, run.floorsCleared);
        p.totalKills += run.kills;
        p.bossesKilled += run.bossesKilled;
        if (won && run.ascension >= p.maxAscension && p.maxAscension < b.maxAscension()) p.maxAscension++;
        p.history.add(0, new Profile.HistoryEntry(SeedCodec.encode(run.seed), run.ascension, won, run.act,
                run.floorsCleared, essence, LocalDate.now().toString()));
        while (p.history.size() > Profile.HISTORY_SIZE) p.history.remove(p.history.size() - 1);
        return essence;
    }

    public static int perkCost(PerkDef perk, Profile p) {
        int lvl = p.perkLevel(perk.id());
        return lvl >= perk.maxLevel() ? -1 : perk.costs().get(lvl);
    }

    public static boolean buyPerk(PerkDef perk, Profile p) {
        int cost = perkCost(perk, p);
        if (cost < 0 || p.essence < cost) return false;
        p.essence -= cost;
        p.perks.put(perk.id(), p.perkLevel(perk.id()) + 1);
        return true;
    }

    public static boolean unlock(Profile p, String id, int cost) {
        if (p.unlocked.contains(id) || p.essence < cost) return false;
        p.essence -= cost;
        p.unlocked.add(id);
        return true;
    }
}
