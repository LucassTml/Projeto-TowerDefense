package io.github.lucasstml.towerrogue.battle.ability;

import io.github.lucasstml.towerrogue.battle.Battle;
import io.github.lucasstml.towerrogue.battle.Enemy;

/**
 * A behaviour component attached to an enemy (composition over inheritance): bosses are
 * ordinary enemies with several abilities, and elite affixes simply add more abilities.
 * All hooks default to no-ops so each ability overrides only what it needs.
 */
public interface EnemyAbility {

    default void onSpawn(Battle battle, Enemy self) {
    }

    default void update(Battle battle, Enemy self, double dt) {
    }

    /** Called when HP reaches 0. Returning true cancels the death (e.g. rebirth). */
    default boolean preventDeath(Battle battle, Enemy self) {
        return false;
    }

    default void onDeath(Battle battle, Enemy self) {
    }
}
