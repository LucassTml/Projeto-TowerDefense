package io.github.lucasstml.towerrogue.relic;

import io.github.lucasstml.towerrogue.battle.BattleHook;
import io.github.lucasstml.towerrogue.rules.Modifiers;

/**
 * One behaviour of a relic. A relic can mix both kinds:
 * <ul>
 *   <li><b>static</b>: {@link #contribute} adds numbers to {@link Modifiers} (cheap, always on);</li>
 *   <li><b>reactive</b>: {@link #newHook} returns a fresh {@link BattleHook} per battle that
 *       subscribes to battle events (explosions, interest, shields...).</li>
 * </ul>
 */
public interface RelicEffect {

    default void contribute(Modifiers m) {
    }

    /** @return a new hook for one battle, or null if the effect is purely static */
    default BattleHook newHook() {
        return null;
    }

    /** Max HP granted (or removed) once, when the relic is obtained. */
    default int maxHpOnPickup() {
        return 0;
    }
}
