package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.battle.ability.EnemyAbility;
import io.github.lucasstml.towerrogue.content.AffixDef;
import io.github.lucasstml.towerrogue.content.DamageType;
import io.github.lucasstml.towerrogue.content.EnemyDef;
import io.github.lucasstml.towerrogue.content.StatusType;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime enemy. Movement is "distance along a lane"; x/y are derived from it each tick.
 * Fields are public-read for the renderer and abilities; only the battle package and
 * abilities mutate them.
 */
public final class Enemy {
    public final int id;
    public final EnemyDef def;
    public final List<AffixDef> affixes;
    public final int wave;
    /** HP scale of this enemy (act/floor/ascension); summons inherit it. */
    public final double scale;

    Lane lane;
    int laneIndex;
    double dist;
    public double x, y;
    public double maxHp, hp;
    public double maxShield, shield;
    public double armor;
    public double baseSpeed;
    public double speedMult = 1.0;
    public double currentSpeed;
    public double invulnerable;
    /** HP cannot drop below this until a phase ability clears it (boss phase gates). */
    public double hpFloor;
    public double timeSinceHit;
    public boolean alive = true;
    public boolean leaked;
    public final StatusEffects status = new StatusEffects();
    final List<EnemyAbility> abilities = new ArrayList<>();

    Enemy(int id, EnemyDef def, List<AffixDef> affixes, int wave, double scale) {
        this.id = id;
        this.def = def;
        this.affixes = List.copyOf(affixes);
        this.wave = wave;
        this.scale = scale;
    }

    public boolean elite() {
        return !affixes.isEmpty();
    }

    public boolean boss() {
        return def.boss();
    }

    public boolean flying() {
        return def.flying();
    }

    public double radius() {
        return def.radius() * (elite() ? 1.15 : 1.0);
    }

    public double resistance(DamageType t) {
        return def.resistance(t);
    }

    public double statusResistance(StatusType t) {
        double r = def.statusResistance(t);
        for (AffixDef a : affixes) r = Math.max(r, a.statusResist().getOrDefault(t, 0.0));
        return r;
    }

    /** Distance still to walk; smaller means closer to the base ("first" targeting). */
    public double remaining() {
        return lane.length() - dist;
    }

    public double progress() {
        return dist / lane.length();
    }

    public double hpFraction() {
        return maxHp <= 0 ? 0 : hp / maxHp;
    }

    public Lane lane() {
        return lane;
    }

    public double dist() {
        return dist;
    }

    public int laneIndex() {
        return laneIndex;
    }

    public void setDist(double d) {
        this.dist = Math.max(0, Math.min(lane.length() - 0.05, d));
    }

    /** Position after walking {@code extra} more tiles; used by ground-targeted shells. */
    public void predict(double extra, double[] out) {
        lane.positionAt(dist + extra, out);
    }

    void update(Battle battle, double dt) {
        if (!alive) return;
        if (invulnerable > 0) invulnerable = Math.max(0, invulnerable - dt);
        timeSinceHit += dt;
        for (int i = 0; i < abilities.size(); i++) abilities.get(i).update(battle, this, dt);
        status.tick(battle, this, dt);
        if (!alive) return;
        currentSpeed = baseSpeed * speedMult * status.speedFactor();
        dist += currentSpeed * dt;
        double[] p = battle.scratch;
        lane.positionAt(dist, p);
        x = p[0];
        y = p[1];
        if (dist >= lane.length()) battle.leak(this);
    }
}
