package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.AttackKind;
import io.github.lucasstml.towerrogue.content.OnHitDef;
import io.github.lucasstml.towerrogue.content.Stat;
import io.github.lucasstml.towerrogue.content.StatSheet;
import io.github.lucasstml.towerrogue.content.TargetMode;
import io.github.lucasstml.towerrogue.content.TowerDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime tower. Its {@link StatSheet} is recomputed by the battle whenever something that
 * could change it happens (placement, upgrade, synergy change), never per frame. Aura
 * buffs from support towers are the only per-tick values.
 */
public final class Tower {
    public final int id;
    public final TowerDef def;
    public final int cx, cy;
    public final boolean onRune;

    int level = 1;
    TowerDef.Branch branch;
    int invested;
    TargetMode targetMode;
    StatSheet stats;
    List<OnHitDef> onHit = List.of();
    double cooldown;
    double disabled;
    /** Facing angle in radians (cosmetic, but cheap to keep in the simulation). */
    public double aim;
    double auraDamage;
    double auraSpeed;
    int kills;
    double damageDealt;

    Tower(int id, TowerDef def, int cx, int cy, boolean onRune, int invested) {
        this.id = id;
        this.def = def;
        this.cx = cx;
        this.cy = cy;
        this.onRune = onRune;
        this.invested = invested;
        this.targetMode = def.defaultTarget();
        this.aim = -Math.PI / 2;
    }

    public double x() { return cx + 0.5; }
    public double y() { return cy + 0.5; }
    public int level() { return level; }
    public TowerDef.Branch branch() { return branch; }
    public int invested() { return invested; }
    public TargetMode targetMode() { return targetMode; }
    public StatSheet stats() { return stats; }
    public List<OnHitDef> onHit() { return onHit; }
    public double disabled() { return disabled; }
    public int kills() { return kills; }
    public double damageDealt() { return damageDealt; }
    public double auraDamage() { return auraDamage; }
    public double auraSpeed() { return auraSpeed; }
    public double range() { return stats.get(Stat.RANGE); }
    public boolean hitsAir() { return stats.flag(Stat.HIT_AIR); }

    public boolean isMaxLevel() {
        return level >= def.maxLevel();
    }

    public boolean providesAura() {
        return stats.get(Stat.SUPPORT_DAMAGE) > 0 || stats.get(Stat.SUPPORT_SPEED) > 0;
    }

    /** Damage per hit including aura buffs. */
    public double effectiveDamage() {
        return stats.get(Stat.DAMAGE) * (1 + auraDamage);
    }

    public double effectiveCooldown() {
        return stats.get(Stat.COOLDOWN) / (stats.get(Stat.ATTACK_SPEED) * (1 + auraSpeed));
    }

    /** Rough damage per second for the UI (ignores splash/chains/effects). */
    public double estimatedDps() {
        if (def.kind() == AttackKind.SUPPORT || def.kind() == AttackKind.GENERATOR) return 0;
        double critFactor = 1 + stats.get(Stat.CRIT_CHANCE) * (stats.get(Stat.CRIT_MULT) - 1);
        double targets = def.kind() == AttackKind.PROJECTILE ? stats.get(Stat.MULTISHOT) : 1;
        return effectiveDamage() * critFactor * targets / effectiveCooldown();
    }

    void update(Battle b, double dt) {
        if (disabled > 0) {
            disabled = Math.max(0, disabled - dt);
            return;
        }
        switch (def.kind()) {
            case SUPPORT -> { /* passive: auras are applied by the battle */ }
            case GENERATOR -> {
                if (b.state() != Battle.State.WAVE) return;
                cooldown -= dt;
                if (cooldown <= 0) {
                    cooldown += effectiveCooldown();
                    int amount = (int) Math.round(stats.get(Stat.INCOME));
                    b.addMana(amount);
                    b.emit(Fx.text(x(), y() - 0.4, "+" + amount, 0x5AB0FF));
                }
            }
            default -> attack(b, dt);
        }
    }

    private void attack(Battle b, double dt) {
        if (cooldown > 0) cooldown -= dt;
        if (cooldown > 0) return;
        List<Enemy> targets = b.targeting().select(this, b.enemies(), def.kind() == AttackKind.PROJECTILE
                ? (int) stats.get(Stat.MULTISHOT) : 1);
        if (targets.isEmpty()) {
            cooldown = 0; // stay ready, fire the moment something enters range
            return;
        }
        Enemy primary = targets.get(0);
        aim = Math.atan2(primary.y - y(), primary.x - x());
        cooldown += effectiveCooldown();
        switch (def.kind()) {
            case PROJECTILE -> {
                for (Enemy t : targets) b.fireProjectile(this, t);
                b.emit(Fx.at(Fx.Type.MUZZLE, x() + Math.cos(aim) * 0.35, y() + Math.sin(aim) * 0.35, 0, def.color()));
            }
            case CHAIN -> chain(b, primary);
            case PULSE -> pulse(b);
            default -> { }
        }
    }

    private void chain(Battle b, Enemy first) {
        List<Enemy> hit = new ArrayList<>();
        double dmg = effectiveDamage();
        double fromX = x(), fromY = y() - 0.3;
        Enemy current = first;
        int jumps = (int) stats.get(Stat.CHAIN);
        double chainRange = stats.get(Stat.CHAIN_RANGE);
        for (int j = 0; j <= jumps && current != null; j++) {
            b.emit(Fx.line(Fx.Type.CHAIN, fromX, fromY, current.x, current.y, def.color()));
            fromX = current.x;
            fromY = current.y;
            hit.add(current);
            b.strike(this, current, dmg);
            dmg *= 0.85;
            current = b.targeting().nearestExcept(fromX, fromY, chainRange, hitsAir(), hit, b.enemies());
        }
    }

    private void pulse(Battle b) {
        double r = range();
        b.emit(Fx.at(Fx.Type.PULSE, x(), y(), r, def.color()));
        List<Enemy> inRange = b.targeting().allInRange(this, b.enemies());
        double dmg = effectiveDamage();
        for (Enemy e : inRange) b.strike(this, e, dmg);
    }
}
