package io.github.lucasstml.towerrogue.content;

import java.util.HashMap;
import java.util.Map;

/**
 * Every tower number that upgrades, relics or synergies can modify.
 * The JSON key is used in content files, e.g. {@code {"stat": "range", "pct": 0.15}}.
 */
public enum Stat {
    DAMAGE("damage", "Damage", 0),
    /** Base seconds between attacks. Effective cooldown = cooldown / attack_speed. */
    COOLDOWN("cooldown", "Cooldown", 1),
    ATTACK_SPEED("attack_speed", "Attack speed", 1),
    /** In tiles. */
    RANGE("range", "Range", 2.5),
    SPLASH("splash", "Splash radius", 0),
    CHAIN("chain", "Chain jumps", 0),
    CHAIN_RANGE("chain_range", "Chain range", 1.8),
    MULTISHOT("multishot", "Targets", 1),
    CRIT_CHANCE("crit_chance", "Crit chance", 0),
    CRIT_MULT("crit_mult", "Crit damage", 2),
    PROJECTILE_SPEED("projectile_speed", "Projectile speed", 9),
    ARMOR_PEN("armor_pen", "Armor pierce", 0),
    STATUS_POWER("status_power", "Effect power", 1),
    STATUS_DURATION("status_duration", "Effect duration", 1),
    STATUS_CHANCE("status_chance", "Effect chance", 0),
    SUPPORT_DAMAGE("support_damage", "Aura damage", 0),
    SUPPORT_SPEED("support_speed", "Aura speed", 0),
    INCOME("income", "Mana per cycle", 0),
    /** 0 or 1. */
    HIT_AIR("hit_air", "Hits flying", 0);

    public final String key;
    public final String label;
    public final double defaultValue;

    private static final Map<String, Stat> BY_KEY = new HashMap<>();

    static {
        for (Stat s : values()) BY_KEY.put(s.key, s);
    }

    Stat(String key, String label, double defaultValue) {
        this.key = key;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public static Stat byKey(String key) {
        Stat s = BY_KEY.get(key);
        if (s == null) throw new IllegalArgumentException("unknown stat '" + key + "'");
        return s;
    }
}
