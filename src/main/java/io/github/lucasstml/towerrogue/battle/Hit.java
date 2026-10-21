package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.DamageType;

/**
 * One instance of damage about to be dealt. It is published on the event bus before
 * mitigation, so relics can scale {@link #multiplier} (e.g. "+60% vs low HP enemies")
 * without the damage pipeline knowing about them.
 */
public final class Hit {
    public final Tower source;
    public final Enemy target;
    public final DamageType type;
    public final double amount;
    public double multiplier = 1.0;
    public boolean crit;
    public boolean dot;
    public boolean ignoreArmor;
    public double armorPen;

    public Hit(Tower source, Enemy target, DamageType type, double amount) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.amount = amount;
    }

    static Hit dot(Tower source, Enemy target, DamageType type, double amount) {
        Hit h = new Hit(source, target, type, amount);
        h.dot = true;
        return h;
    }
}
