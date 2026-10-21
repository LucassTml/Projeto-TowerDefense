package io.github.lucasstml.towerrogue.battle.ability;

import io.github.lucasstml.towerrogue.battle.Battle;
import io.github.lucasstml.towerrogue.battle.Enemy;
import io.github.lucasstml.towerrogue.battle.Fx;
import io.github.lucasstml.towerrogue.battle.Tower;
import io.github.lucasstml.towerrogue.content.ContentException;
import io.github.lucasstml.towerrogue.content.EffectDef;
import io.github.lucasstml.towerrogue.content.StatusType;
import io.github.lucasstml.towerrogue.json.JsonArray;
import io.github.lucasstml.towerrogue.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory (registry of constructors keyed by the JSON {@code type}) plus the ability
 * implementations. Adding an ability = one class + one {@code register} line; content
 * files can then use it anywhere.
 */
public final class Abilities {
    private static final Map<String, Function<JsonObject, EnemyAbility>> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put("regen", p -> new Regen(p.getDouble("pct")));
        REGISTRY.put("shield", p -> new Shield(p.getDouble("pct"), p.getDouble("delay", 3), p.getDouble("rate", 0.25)));
        REGISTRY.put("summon", p -> new Summon(p.getString("enemy"), p.getInt("count", 1), p.getDouble("interval"),
                p.getDouble("initialDelay", p.getDouble("interval"))));
        REGISTRY.put("splitOnDeath", p -> new SplitOnDeath(p.getString("enemy"), p.getInt("count", 2)));
        REGISTRY.put("enrage", p -> new Enrage(p.getDouble("threshold"), p.getDouble("speedMult")));
        REGISTRY.put("healAura", p -> new HealAura(p.getDouble("radius"), p.getDouble("pct"), p.getDouble("interval")));
        REGISTRY.put("disableTowers", p -> new DisableTowers(p.getDouble("radius"), p.getDouble("duration"),
                p.getDouble("interval"), p.getDouble("initialDelay", p.getDouble("interval"))));
        REGISTRY.put("blink", p -> new Blink(p.getDouble("distance"), p.getDouble("interval")));
        REGISTRY.put("phases", p -> new Phases(p.getArray("thresholds"), p.getDouble("duration"),
                p.getString("summon", null), p.getInt("count", 0)));
        REGISTRY.put("rebirth", p -> new Rebirth(p.getDouble("pct")));
    }

    private Abilities() {
    }

    public static EnemyAbility create(EffectDef def) {
        Function<JsonObject, EnemyAbility> ctor = REGISTRY.get(def.type());
        if (ctor == null) throw new ContentException("unknown enemy ability type '" + def.type() + "'");
        return ctor.apply(def.params());
    }

    // ------------------------------------------------------------------ implementations

    /** Heals a % of max HP per second. Poison suppresses regeneration (a deliberate counter). */
    static final class Regen implements EnemyAbility {
        private final double pct;

        Regen(double pct) {
            this.pct = pct;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            if (e.hp < e.maxHp && !e.status.has(StatusType.POISON)) {
                e.hp = Math.min(e.maxHp, e.hp + e.maxHp * pct * dt);
            }
        }
    }

    /** Damage shield that recharges after {@code delay} seconds without being hit. */
    static final class Shield implements EnemyAbility {
        private final double pct, delay, rate;

        Shield(double pct, double delay, double rate) {
            this.pct = pct;
            this.delay = delay;
            this.rate = rate;
        }

        @Override
        public void onSpawn(Battle b, Enemy e) {
            e.maxShield += e.maxHp * pct;
            e.shield = e.maxShield;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            if (e.timeSinceHit >= delay && e.shield < e.maxShield) {
                e.shield = Math.min(e.maxShield, e.shield + e.maxShield * rate * dt);
            }
        }
    }

    static final class Summon implements EnemyAbility {
        private final String enemy;
        private final int count;
        private final double interval;
        private double timer;

        Summon(String enemy, int count, double interval, double initialDelay) {
            this.enemy = enemy;
            this.count = count;
            this.interval = interval;
            this.timer = initialDelay;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            if (e.invulnerable > 0) return;
            timer -= dt;
            if (timer <= 0) {
                timer += interval;
                b.summon(e, enemy, count);
            }
        }
    }

    static final class SplitOnDeath implements EnemyAbility {
        private final String enemy;
        private final int count;

        SplitOnDeath(String enemy, int count) {
            this.enemy = enemy;
            this.count = count;
        }

        @Override
        public void onDeath(Battle b, Enemy e) {
            b.summon(e, enemy, count);
        }
    }

    static final class Enrage implements EnemyAbility {
        private final double threshold, speedMult;
        private boolean triggered;

        Enrage(double threshold, double speedMult) {
            this.threshold = threshold;
            this.speedMult = speedMult;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            if (!triggered && e.hpFraction() <= threshold) {
                triggered = true;
                e.speedMult *= speedMult;
                b.emit(Fx.text(e.x, e.y - 0.6, "ENRAGED!", 0xE05561));
            }
        }
    }

    static final class HealAura implements EnemyAbility {
        private final double radius, pct, interval;
        private double timer;

        HealAura(double radius, double pct, double interval) {
            this.radius = radius;
            this.pct = pct;
            this.interval = interval;
            this.timer = interval;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            timer -= dt;
            if (timer > 0) return;
            timer += interval;
            boolean any = false;
            for (Enemy o : b.targeting().inRadius(e.x, e.y, radius, 2, b.enemies())) {
                if (o.hp < o.maxHp && !o.status.has(StatusType.POISON)) {
                    o.hp = Math.min(o.maxHp, o.hp + o.maxHp * pct);
                    any = true;
                }
            }
            if (any) b.emit(Fx.at(Fx.Type.HEAL, e.x, e.y, radius, 0x6AD182));
        }
    }

    /** Boss stomp/breath: stuns towers in a radius for a few seconds. */
    static final class DisableTowers implements EnemyAbility {
        private final double radius, duration, interval;
        private double timer;

        DisableTowers(double radius, double duration, double interval, double initialDelay) {
            this.radius = radius;
            this.duration = duration;
            this.interval = interval;
            this.timer = initialDelay;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            timer -= dt;
            if (timer > 0) return;
            timer += interval;
            b.emit(Fx.at(Fx.Type.SHOCKWAVE, e.x, e.y, radius, e.def.color()));
            for (Tower t : b.towers()) {
                double dx = t.x() - e.x, dy = t.y() - e.y;
                if (dx * dx + dy * dy <= radius * radius) b.disableTower(t, duration);
            }
        }
    }

    static final class Blink implements EnemyAbility {
        private final double distance, interval;
        private double timer = -1;

        Blink(double distance, double interval) {
            this.distance = distance;
            this.interval = interval;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            if (timer < 0) timer = interval * (0.5 + 0.5 * b.combatRng().nextDouble());
            timer -= dt;
            if (timer > 0 || e.status.frozen()) return;
            timer += interval;
            double ox = e.x, oy = e.y;
            e.setDist(e.dist() + distance);
            double[] p = new double[2];
            e.lane().positionAt(e.dist(), p);
            e.x = p[0];
            e.y = p[1];
            b.emit(Fx.line(Fx.Type.BLINK, ox, oy, e.x, e.y, e.def.color()));
        }
    }

    /**
     * Boss phases: HP is clamped at each threshold until the phase triggers, so burst damage
     * cannot skip a phase. The boss then becomes invulnerable briefly and calls reinforcements.
     */
    static final class Phases implements EnemyAbility {
        private final double[] thresholds;
        private final double duration;
        private final String summon;
        private final int count;
        private int next;

        Phases(JsonArray thresholds, double duration, String summon, int count) {
            this.thresholds = new double[thresholds.size()];
            for (int i = 0; i < thresholds.size(); i++) this.thresholds[i] = thresholds.getDouble(i);
            this.duration = duration;
            this.summon = summon;
            this.count = count;
        }

        @Override
        public void onSpawn(Battle b, Enemy e) {
            e.hpFloor = thresholds.length > 0 ? thresholds[0] * e.maxHp : 0;
        }

        @Override
        public void update(Battle b, Enemy e, double dt) {
            if (next >= thresholds.length || e.hp > e.hpFloor + 1e-6) return;
            next++;
            e.hpFloor = next < thresholds.length ? thresholds[next] * e.maxHp : 0;
            e.invulnerable = duration;
            e.status.clearAll();
            b.emit(Fx.at(Fx.Type.PHASE, e.x, e.y, 1.6, e.def.color()));
            b.emit(Fx.text(e.x, e.y - 0.8, "PHASE " + (next + 1), 0xF2C14E));
            if (summon != null && count > 0) b.summon(e, summon, count);
        }
    }

    static final class Rebirth implements EnemyAbility {
        private final double pct;
        private boolean used;

        Rebirth(double pct) {
            this.pct = pct;
        }

        @Override
        public boolean preventDeath(Battle b, Enemy e) {
            if (used) return false;
            used = true;
            e.hp = e.maxHp * pct;
            e.invulnerable = 1.0;
            e.status.clearAll();
            b.emit(Fx.at(Fx.Type.PHASE, e.x, e.y, 1.0, 0xFFB13D));
            b.emit(Fx.text(e.x, e.y - 0.6, "REBORN", 0xFFB13D));
            return true;
        }
    }
}
