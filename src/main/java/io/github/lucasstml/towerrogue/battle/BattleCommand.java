package io.github.lucasstml.towerrogue.battle;

import io.github.lucasstml.towerrogue.content.TargetMode;

/**
 * Player intents (Command pattern). The UI never mutates the battle directly: it submits
 * commands, the battle executes them at a well-defined tick and records them in a log.
 * Seed + config + command log is therefore a complete replay of a battle.
 */
public sealed interface BattleCommand {

    record Place(String towerId, int x, int y) implements BattleCommand {
    }

    record Upgrade(int towerId) implements BattleCommand {
    }

    record ChooseBranch(int towerId, int branchIndex) implements BattleCommand {
    }

    record Sell(int towerId) implements BattleCommand {
    }

    record SetTarget(int towerId, TargetMode mode) implements BattleCommand {
    }

    record StartWave() implements BattleCommand {
    }

    /** A command stamped with the tick it was executed on. */
    record Logged(long tick, BattleCommand command) {
    }
}
