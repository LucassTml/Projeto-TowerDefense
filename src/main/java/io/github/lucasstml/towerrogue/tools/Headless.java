package io.github.lucasstml.towerrogue.tools;

import io.github.lucasstml.towerrogue.battle.Battle;
import io.github.lucasstml.towerrogue.battle.BattleCommand;
import io.github.lucasstml.towerrogue.battle.BattleConfig;
import io.github.lucasstml.towerrogue.battle.BattleResult;
import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.content.Rarity;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.core.Rng;
import io.github.lucasstml.towerrogue.core.SeedCodec;
import io.github.lucasstml.towerrogue.relic.RelicEffects;
import io.github.lucasstml.towerrogue.rules.Modifiers;
import io.github.lucasstml.towerrogue.run.RunMap;
import io.github.lucasstml.towerrogue.run.RunMapGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Command-line tools that run the simulation without a window:
 * <ul>
 *   <li>{@code --verify-determinism [seed]}: plays a battle with the bot, replays the recorded
 *       command log on a fresh battle and checks that both final state hashes match.</li>
 *   <li>{@code --balance-report [n]}: bot-plays n battles per act/encounter and prints win
 *       rates and HP lost, which is how the numbers in the JSON files were tuned.</li>
 * </ul>
 */
public final class Headless {
    private static final long MAX_TICKS = 60L * 60 * 30; // 30 simulated minutes

    private Headless() {
    }

    public static int verifyDeterminism(Content content, String seedText) {
        long seed = seedText == null ? 0x5EED_CAFEL : SeedCodec.decode(seedText);
        System.out.println("Determinism check, seed " + SeedCodec.encode(seed));
        boolean ok = true;

        RunMap m1 = RunMapGenerator.generate(Rng.derive(seed, "map:1"), 7);
        RunMap m2 = RunMapGenerator.generate(Rng.derive(seed, "map:1"), 7);
        boolean mapsEqual = describe(m1).equals(describe(m2));
        System.out.println("  run map generated twice:  " + (mapsEqual ? "identical" : "DIFFERENT"));
        ok &= mapsEqual;

        for (EncounterKind kind : EncounterKind.values()) {
            for (int act = 1; act <= content.acts().size(); act++) {
                BattleConfig cfg = sampleConfig(content, Rng.deriveSeed(seed, kind + ":" + act), act, kind, 3, act);
                Battle a = new Battle(content, cfg);
                AutoPlayer bot = new AutoPlayer(a);
                run(a, bot, null);
                Battle b = new Battle(content, sampleConfig(content, Rng.deriveSeed(seed, kind + ":" + act), act, kind, 3, act));
                run(b, null, a.commandLog());
                boolean same = a.stateHash() == b.stateHash() && a.tick() == b.tick();
                ok &= same;
                BattleResult r = a.result();
                System.out.printf(Locale.ROOT, "  act %d %-6s  ticks=%-6d commands=%-3d kills=%-4d %s  hash %016x vs %016x  %s%n",
                        act, kind, r.ticks(), a.commandLog().size(), r.kills(), r.won() ? "WON " : "LOST",
                        a.stateHash(), b.stateHash(), same ? "OK" : "MISMATCH");
            }
        }
        System.out.println(ok ? "RESULT: deterministic" : "RESULT: NOT deterministic");
        return ok ? 0 : 1;
    }

    public static int balanceReport(Content content, int perConfig) {
        System.out.println("Balance report: bot plays " + perConfig + " battles per row (floor = mid-act).");
        System.out.println("act kind    blueprints  win%   avgHpLost  avgLeaks  avgMinutes");
        for (int act = 1; act <= content.acts().size(); act++) {
            for (EncounterKind kind : EncounterKind.values()) {
                int floor = kind == EncounterKind.BOSS ? content.act(act).floors() : 3;
                int wins = 0;
                double hpLost = 0, leaks = 0, minutes = 0;
                for (int i = 0; i < perConfig; i++) {
                    long seed = Rng.deriveSeed(0xBA1A_CE00L + i, act + ":" + kind);
                    BattleConfig cfg = sampleConfig(content, seed, act, kind, floor, act + 1);
                    Battle b = new Battle(content, cfg);
                    run(b, new AutoPlayer(b), null);
                    BattleResult r = b.result();
                    if (r.won()) wins++;
                    hpLost += cfg.hp() - r.hp();
                    leaks += r.leaks();
                    minutes += r.ticks() * Battle.DT / 60.0;
                }
                System.out.printf(Locale.ROOT, "%-3d %-7s %-11d %5.0f%%  %9.1f  %8.1f  %10.1f%n", act, kind, 3 + act,
                        100.0 * wins / perConfig, hpLost / perConfig, leaks / perConfig, minutes / perConfig);
            }
        }
        return 0;
    }

    /**
     * A plausible mid-run loadout: the starters plus {@code extraTowers} random unlocked towers,
     * blueprint tiers rising with the act, and a couple of random common relics.
     */
    static BattleConfig sampleConfig(Content content, long seed, int act, EncounterKind kind, int floor, int extraTowers) {
        Rng rng = Rng.derive(seed, "loadout");
        Map<String, Integer> bp = new LinkedHashMap<>();
        for (String id : content.balance().starterTowers) bp.put(id, Math.min(2, act - 1));
        List<String> pool = new ArrayList<>();
        for (TowerDef t : content.towers()) if (!bp.containsKey(t.id()) && !t.locked()) pool.add(t.id());
        rng.shuffle(pool);
        for (int i = 0; i < Math.min(extraTowers, pool.size()); i++) bp.put(pool.get(i), Math.min(2, Math.max(0, act - 2)));
        List<String> relics = new ArrayList<>();
        List<String> commons = new ArrayList<>();
        content.relics().forEach(r -> {
            if (r.rarity() == Rarity.COMMON && !r.locked()) commons.add(r.id());
        });
        rng.shuffle(commons);
        for (int i = 0; i < Math.min(act, commons.size()); i++) relics.add(commons.get(i));
        Modifiers mods = new Modifiers();
        RelicEffects.contributeAll(content, relics, mods);
        return new BattleConfig(seed, act, kind, floor, bp, mods, RelicEffects.hooksFor(content, relics), 50, 50);
    }

    /** Runs to completion, either driven by the bot or by replaying a command log. */
    static void run(Battle b, AutoPlayer bot, List<BattleCommand.Logged> replay) {
        int next = 0;
        while (b.state() != Battle.State.WON && b.state() != Battle.State.LOST && b.tick() < MAX_TICKS) {
            if (bot != null) bot.think();
            if (replay != null) {
                while (next < replay.size() && replay.get(next).tick() == b.tick()) b.submit(replay.get(next++).command());
                b.processCommands();
            }
            b.step();
        }
    }

    private static String describe(RunMap m) {
        StringBuilder sb = new StringBuilder();
        for (RunMap.Node n : m.nodes()) sb.append(n.id).append(n.type).append(n.next).append(';');
        return sb.toString();
    }
}
