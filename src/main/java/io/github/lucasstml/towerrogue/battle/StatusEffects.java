package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.DamageType;
import io.github.lucasstml.towerrogue.content.StatusType;

/**
 * Status effects on one enemy. Plain fields instead of a map: there are few types and each
 * has its own stacking rule, which reads more clearly as explicit code.
 *
 * <ul>
 *   <li>SLOW / VULNERABLE: strongest application wins, duration refreshes.</li>
 *   <li>BURN / POISON / SHRED: each application adds a stack (capped).</li>
 *   <li>FREEZE: stun followed by 1s of immunity, so stun-lock builds have a ceiling.</li>
 * </ul>
 * Damage-over-time is applied in 0.25s ticks so numbers on screen stay readable.
 */
public final class StatusEffects {
    private static final double DOT_TICK = 0.25;
    private static final double FREEZE_IMMUNITY = 1.0;

    double slowMag, slowTime;
    double freezeTime, freezeImmune;
    double burnDps, burnTime;
    int burnStacks;
    Tower burnSource;
    double poisonDps, poisonTime;
    int poisonStacks;
    Tower poisonSource;
    double shredPer, shredTime;
    int shredStacks;
    double vulnMag, vulnTime;
    private double dotTimer;

    public boolean has(StatusType t) {
        return switch (t) {
            case SLOW -> slowTime > 0;
            case FREEZE -> freezeTime > 0;
            case BURN -> burnTime > 0;
            case POISON -> poisonTime > 0;
            case SHRED -> shredTime > 0;
            case VULNERABLE -> vulnTime > 0;
        };
    }

    public boolean frozen() {
        return freezeTime > 0;
    }

    public int burnStacks() {
        return burnTime > 0 ? burnStacks : 0;
    }

    public int poisonStacks() {
        return poisonTime > 0 ? poisonStacks : 0;
    }

    public double slowMagnitude() {
        return slowTime > 0 ? slowMag : 0;
    }

    /** Multiplier applied to movement speed. */
    public double speedFactor() {
        if (freezeTime > 0) return 0;
        return 1 - (slowTime > 0 ? slowMag : 0);
    }

    public double armorReduction() {
        return shredTime > 0 ? shredPer * shredStacks : 0;
    }

    public double vulnerability() {
        return vulnTime > 0 ? vulnMag : 0;
    }

    void apply(StatusType type, double mag, double dur, Tower source, Caps caps) {
        switch (type) {
            case SLOW -> {
                mag = Math.min(0.8, mag);
                if (mag > slowMag || slowTime <= 0) {
                    slowMag = mag;
                    slowTime = dur;
                } else {
                    slowTime = Math.max(slowTime, dur);
                }
            }
            case FREEZE -> {
                if (freezeImmune <= 0) freezeTime = Math.max(freezeTime, dur);
            }
            case BURN -> {
                burnDps = burnTime > 0 ? Math.max(burnDps, mag) : mag;
                burnStacks = burnTime > 0 ? Math.min(caps.burn(), burnStacks + 1) : 1;
                burnTime = Math.max(burnTime, dur);
                burnSource = source;
            }
            case POISON -> {
                poisonDps = poisonTime > 0 ? Math.max(poisonDps, mag) : mag;
                poisonStacks = poisonTime > 0 ? Math.min(caps.poison(), poisonStacks + 1) : 1;
                poisonTime = Math.max(poisonTime, dur);
                poisonSource = source;
            }
            case SHRED -> {
                shredPer = shredTime > 0 ? Math.max(shredPer, mag) : mag;
                shredStacks = shredTime > 0 ? Math.min(caps.shred(), shredStacks + 1) : 1;
                shredTime = Math.max(shredTime, dur);
            }
            case VULNERABLE -> {
                if (mag > vulnMag || vulnTime <= 0) {
                    vulnMag = mag;
                    vulnTime = dur;
                } else {
                    vulnTime = Math.max(vulnTime, dur);
                }
            }
        }
    }

    void clearFreeze() {
        if (freezeTime > 0) {
            freezeTime = 0;
            freezeImmune = FREEZE_IMMUNITY;
        }
    }

    public void clearAll() {
        slowTime = freezeTime = burnTime = poisonTime = shredTime = vulnTime = 0;
        burnStacks = poisonStacks = shredStacks = 0;
    }

    void tick(Battle battle, Enemy e, double dt) {
        slowTime = Math.max(0, slowTime - dt);
        if (freezeTime > 0) {
            freezeTime -= dt;
            if (freezeTime <= 0) {
                freezeTime = 0;
                freezeImmune = FREEZE_IMMUNITY;
            }
        } else if (freezeImmune > 0) {
            freezeImmune -= dt;
        }
        shredTime = Math.max(0, shredTime - dt);
        vulnTime = Math.max(0, vulnTime - dt);

        boolean dots = burnTime > 0 || poisonTime > 0;
        if (!dots) {
            dotTimer = 0;
            return;
        }
        dotTimer += dt;
        while (dotTimer >= DOT_TICK && e.alive) {
            dotTimer -= DOT_TICK;
            if (burnTime > 0) {
                battle.damage(Hit.dot(burnSource, e, DamageType.FIRE, burnDps * burnStacks * DOT_TICK));
            }
            if (poisonTime > 0 && e.alive) {
                Hit h = Hit.dot(poisonSource, e, DamageType.POISON, poisonDps * poisonStacks * DOT_TICK);
                h.ignoreArmor = true;
                battle.damage(h);
            }
        }
        burnTime = Math.max(0, burnTime - dt);
        poisonTime = Math.max(0, poisonTime - dt);
    }

    /** Stack caps for this battle (base balance + relic bonuses). */
    record Caps(int burn, int poison, int shred) {
    }
}
