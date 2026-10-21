package io.github.lucasstml.towerrogue.tools;

import io.github.lucasstml.towerrogue.battle.Battle;
import io.github.lucasstml.towerrogue.battle.BattleCommand;
import io.github.lucasstml.towerrogue.battle.BattleMap;
import io.github.lucasstml.towerrogue.battle.Lane;
import io.github.lucasstml.towerrogue.battle.Tower;
import io.github.lucasstml.towerrogue.content.AttackKind;
import io.github.lucasstml.towerrogue.content.Stat;
import io.github.lucasstml.towerrogue.content.TowerDef;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple deterministic bot used for headless verification and balance reports. It builds
 * on the tiles that see the most path, rotates through its blueprints, upgrades when it
 * has spare mana and starts waves as soon as it is ready. It is intentionally "average":
 * if the bot can win a fight, a thoughtful player can too.
 */
public final class AutoPlayer {
    private final Battle battle;
    private final double[][] coverage = new double[BattleMap.COLS][BattleMap.ROWS];
    private final List<String> blueprints;
    private int rotation;
    private int branchToggle;

    public AutoPlayer(Battle battle) {
        this.battle = battle;
        this.blueprints = new ArrayList<>(battle.config().blueprints().keySet());
        computeCoverage();
    }

    /** Samples every lane (flight lines at lower weight) and counts, per tile, how much path lies within 2.6 tiles. */
    private void computeCoverage() {
        for (Lane lane : battle.map().groundLanes()) addCoverage(lane, 1.0);
        for (Lane lane : battle.map().airLanes()) addCoverage(lane, 0.6);
    }

    private void addCoverage(Lane lane, double weight) {
        double[] p = new double[2];
        double r2 = 2.6 * 2.6;
        for (double d = 0; d < lane.length(); d += 0.25) {
            lane.positionAt(d, p);
            for (int x = 0; x < BattleMap.COLS; x++) {
                for (int y = 0; y < BattleMap.ROWS; y++) {
                    double dx = x + 0.5 - p[0], dy = y + 0.5 - p[1];
                    if (dx * dx + dy * dy <= r2) coverage[x][y] += weight;
                }
            }
        }
    }

    /** Call once per tick before {@link Battle#step()}. */
    public void think() {
        if (battle.tick() % 20 != 0) return;
        Battle.State s = battle.state();
        if (s == Battle.State.WON || s == Battle.State.LOST) return;
        buildAndUpgrade();
        if (s == Battle.State.PREP && battle.canStartWave()) submit(new BattleCommand.StartWave());
    }

    private void buildAndUpgrade() {
        for (int guard = 0; guard < 12; guard++) {
            boolean acted = false;
            // Keep a reserve once a few towers exist, so upgrades also happen.
            if (battle.towers().size() < 4 + battle.wavesStarted() * 2) acted = tryBuild();
            if (!acted) acted = tryUpgrade();
            if (!acted) acted = tryBuild();
            if (!acted) return;
        }
    }

    /**
     * Picks the affordable blueprint with the best damage per mana, favouring anti-air when
     * the next wave has flyers and penalising types already built (keeps the board varied).
     */
    private boolean tryBuild() {
        boolean flyersNext = false;
        for (var g : battle.nextWavePreview()) if (battle.content().enemy(g.enemyId()).flying()) flyersNext = true;
        String bestId = null;
        double bestValue = -1;
        for (String id : blueprints) {
            TowerDef def = battle.content().tower(id);
            if (def.kind() == AttackKind.GENERATOR && battle.wavesStarted() > 2) continue;
            int cost = battle.buildCost(def);
            if (battle.mana() < cost) continue;
            var stats = battle.previewStats(def, false);
            double dps = stats.get(Stat.DAMAGE) / stats.effectiveCooldown()
                    * Math.max(1, stats.get(Stat.MULTISHOT) + stats.get(Stat.CHAIN) * 0.6 + stats.get(Stat.SPLASH) * 1.5);
            if (!def.onHit().isEmpty()) dps *= 1.4;
            if (def.kind() == AttackKind.SUPPORT || def.kind() == AttackKind.GENERATOR) dps = 12;
            if (flyersNext && stats.flag(Stat.HIT_AIR)) dps *= 1.6;
            int built = 0;
            for (Tower t : battle.towers()) if (t.def == def) built++;
            double value = dps / Math.max(1, cost) * Math.pow(0.8, built) * (1 + 0.01 * ((rotation + id.length()) % 7));
            if (value > bestValue) {
                bestValue = value;
                bestId = id;
            }
        }
        if (bestId == null) return false;
        TowerDef def = battle.content().tower(bestId);
        int[] tile = bestTile(def);
        if (tile == null) return false;
        rotation++;
        return submit(new BattleCommand.Place(bestId, tile[0], tile[1]));
    }

    private boolean tryUpgrade() {
        Tower best = null;
        double bestScore = -1;
        for (Tower t : battle.towers()) {
            if (t.isMaxLevel() && t.branch() != null) continue;
            double score = coverage[t.cx][t.cy] - t.level() * 5;
            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        if (best == null) return false;
        if (!best.isMaxLevel()) {
            if (battle.mana() < battle.upgradeCost(best)) return false;
            return submit(new BattleCommand.Upgrade(best.id));
        }
        if (best.def.branches().isEmpty()) return false;
        int idx = branchToggle++ % 2;
        if (battle.mana() < battle.branchCost(best, idx)) return false;
        return submit(new BattleCommand.ChooseBranch(best.id, idx));
    }

    private int[] bestTile(TowerDef def) {
        boolean aura = def.baseStat(Stat.SUPPORT_DAMAGE) > 0;
        int[] best = null;
        double bestScore = -1;
        for (int x = 0; x < BattleMap.COLS; x++) {
            for (int y = 0; y < BattleMap.ROWS; y++) {
                if (!battle.map().buildable(x, y) || battle.towerAt(x, y) != null) continue;
                double score = coverage[x][y];
                if (aura) score = neighbours(x, y) * 10 + coverage[x][y] * 0.1;
                if (battle.map().tile(x, y) == BattleMap.Tile.RUNE) score *= 1.2;
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[]{x, y};
                }
            }
        }
        return best;
    }

    private int neighbours(int x, int y) {
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (battle.towerAt(x + dx, y + dy) != null) n++;
        return n;
    }

    private boolean submit(BattleCommand c) {
        int before = battle.commandLog().size();
        battle.submit(c);
        battle.processCommands();
        return battle.commandLog().size() > before;
    }
}
