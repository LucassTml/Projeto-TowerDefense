package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.ActDef;
import io.github.lucasstml.towerrogue.content.AffixDef;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.EnemyDef;
import io.github.lucasstml.towerrogue.core.MathUtil;
import io.github.lucasstml.towerrogue.core.Rng;
import io.github.lucasstml.towerrogue.rules.Modifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural wave composition ("AI director" style). Each wave gets a threat budget that
 * grows with wave index and floor; the director buys weighted groups from the act's pool
 * until the budget is spent. Elite fights inject affixed elites, boss fights end with the
 * boss surrounded by escorts. All waves are planned up front from the battle seed, so the
 * UI can preview the next wave and replays are exact.
 */
final class WaveDirector {
    private WaveDirector() {
    }

    static List<WavePlan> plan(Content content, ActDef act, EncounterKind kind, int floor, Modifiers mods,
                               Rng rng, int lanes) {
        int waves = act.waves(kind);
        double hpBase = act.hpMult() * (1 + act.hpPerFloor() * floor) * (1 + mods.enemyHpPct);
        List<WavePlan> plans = new ArrayList<>();
        for (int w = 0; w < waves; w++) {
            double budget = act.budgetBase() + act.budgetPerWave() * w + act.budgetPerFloor() * floor;
            if (kind == EncounterKind.ELITE) budget *= 1.1;
            double hpScale = hpBase * (1 + 0.035 * w);
            boolean last = w == waves - 1;
            List<WavePlan.Spawn> spawns = new ArrayList<>();
            double t = 0.5;

            if (kind == EncounterKind.BOSS && last) {
                List<ActDef.PoolEntry> escorts = new ArrayList<>();
                for (ActDef.PoolEntry p : act.pool()) if (act.bossEscorts().contains(p.enemy())) escorts.add(p);
                if (escorts.isEmpty()) escorts = act.pool();
                t = fill(content, spawns, budget * 0.45, t, escorts, 99, 99, lanes, hpScale, rng);
                spawns.add(new WavePlan.Spawn(t + 2, act.boss(), 0, List.of(), hpBase * (1 + mods.bossHpPct)));
                fill(content, spawns, budget * 0.4, t + 7, escorts, 99, 99, lanes, hpScale, rng);
            } else {
                int elites = 0;
                if (kind == EncounterKind.ELITE) {
                    if (last) elites = 1 + (floor >= 4 ? 1 : 0);
                    else if (w == waves / 2) elites = 1;
                }
                t = fill(content, spawns, budget * (elites > 0 ? 0.6 : 1.0), t, act.pool(), w, floor, lanes, hpScale, rng);
                for (int i = 0; i < elites; i++) {
                    List<AffixDef> pool = new ArrayList<>(content.affixes());
                    rng.shuffle(pool);
                    int n = Math.min(pool.size(), 1 + mods.eliteExtraAffixes);
                    List<String> affixes = new ArrayList<>();
                    for (int k = 0; k < n; k++) affixes.add(pool.get(k).id());
                    String enemy = rng.pick(act.elites());
                    double at = rng.range(2.0, Math.max(3.0, t - 1.0));
                    spawns.add(new WavePlan.Spawn(at, enemy, lanes > 1 ? rng.nextInt(lanes) : 0, affixes, hpScale));
                }
            }
            plans.add(new WavePlan(spawns));
        }
        return plans;
    }

    /** Buys enemy groups until the budget runs out. Returns the time after the last group. */
    private static double fill(Content content, List<WavePlan.Spawn> out, double budget, double t,
                               List<ActDef.PoolEntry> pool, int wave, int floor, int lanes, double hpScale, Rng rng) {
        while (true) {
            List<ActDef.PoolEntry> eligible = new ArrayList<>();
            for (ActDef.PoolEntry p : pool) {
                if (p.minWave() <= wave && p.minFloor() <= floor && p.cost() <= budget + 1e-9) eligible.add(p);
            }
            if (eligible.isEmpty()) return t;
            ActDef.PoolEntry pick = rng.weighted(eligible, ActDef.PoolEntry::weight);
            if (pick == null) return t;
            int max = (int) Math.floor(budget / pick.cost() + 1e-9);
            int size = pick.cost() >= 3 ? rng.range(1, 3) : rng.range(3, 7);
            size = Math.max(1, Math.min(size, max));
            EnemyDef def = content.enemy(pick.enemy());
            double interval = MathUtil.clamp(0.8 / def.speed(), 0.35, 1.5);
            int lane = lanes > 1 ? rng.nextInt(lanes) : 0;
            for (int i = 0; i < size; i++) {
                out.add(new WavePlan.Spawn(t + i * interval, pick.enemy(), lane, List.of(), hpScale));
            }
            t += size * interval + rng.range(0.6, 1.6);
            budget -= size * pick.cost();
        }
    }
}
