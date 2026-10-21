package io.github.lucasstml.towerrogue.rules;

import io.github.lucasstml.towerrogue.content.Balance;
import io.github.lucasstml.towerrogue.content.StatMod;
import io.github.lucasstml.towerrogue.content.TowerDef;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated rule changes coming from relics, meta perks and Ascension.
 *
 * <p>Everything that "changes a number" funnels into this one plain-data object, which is
 * rebuilt from scratch whenever the run changes (relic gained, run loaded). Systems read
 * from it and never ask "do I own relic X?", so relics stay decoupled from game logic.
 * Effects that react to events (explosions, interest...) are {@code battle.BattleHook}s instead.
 */
public final class Modifiers {
    /** A stat modifier restricted to towers with {@code tag}, or all towers when tag is null. */
    public record ScopedMod(String tag, StatMod mod) {
    }

    // ---- battle ----
    public int startMana;
    public int killMana;
    public int waveMana;
    public int earlyCallBonus;
    public int freeTowers;
    public int leakDamage;
    public int burnStacks;
    public int poisonStacks;
    public int eliteExtraAffixes;
    public double towerCostPct;
    public double sellRefund = -1;
    public double enemyHpPct;
    public double enemySpeedPct;
    public double bossHpPct;
    public final List<ScopedMod> towerMods = new ArrayList<>();

    // ---- run ----
    public double goldPct;
    public double shopPricePct;
    public double restHealPct;
    public int draftChoices;
    public double rareChance;
    public int healAfterCombat;
    public int maxHpBonus;

    public double towerCostMult() {
        return Math.max(0.2, 1 + towerCostPct);
    }

    public double goldMult() {
        return Math.max(0, 1 + goldPct);
    }

    public double shopPriceMult() {
        return Math.max(0.2, 1 + shopPricePct);
    }

    public List<StatMod> towerModsFor(TowerDef def) {
        List<StatMod> out = new ArrayList<>();
        for (ScopedMod m : towerMods) {
            if (m.tag() == null || def.hasTag(m.tag())) out.add(m.mod());
        }
        return out;
    }

    /** Adds the cumulative effects of Ascension levels 1..level. */
    public void applyAscension(Balance balance, int level) {
        for (Balance.AscensionLevel a : balance.ascension) {
            if (a.level() > level) continue;
            JsonObject e = a.effects();
            enemyHpPct += e.getDouble("enemyHp", 0);
            maxHpBonus += e.getInt("maxHp", 0);
            eliteExtraAffixes += e.getInt("eliteAffixes", 0);
            goldPct += e.getDouble("goldMult", 0);
            restHealPct += e.getDouble("restHeal", 0);
            enemySpeedPct += e.getDouble("enemySpeed", 0);
            shopPricePct += e.getDouble("shopPrice", 0);
            startMana += e.getInt("startMana", 0);
            bossHpPct += e.getDouble("bossHp", 0);
            leakDamage += e.getInt("leakDamage", 0);
        }
    }
}
