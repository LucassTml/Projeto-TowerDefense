package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.EncounterKind;
import io.github.lucasstml.towerrogue.rules.Modifiers;

import java.util.List;
import java.util.Map;

/**
 * Everything a battle needs from the outside world. Together with the command log this is
 * a complete, reproducible description of a battle.
 *
 * @param blueprints owned tower ids mapped to their blueprint tier (0 = base), in display order
 */
public record BattleConfig(long seed, int act, EncounterKind kind, int floor, Map<String, Integer> blueprints,
                           Modifiers mods, List<BattleHook> hooks, int hp, int maxHp) {
}
