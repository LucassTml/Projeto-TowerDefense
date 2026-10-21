package io.github.lucasstml.towerrogue.battle;

/** Summary handed back to the run layer when a battle ends. */
public record BattleResult(boolean won, int hp, int kills, int leaks, double damage, int elitesKilled,
                           boolean bossKilled, int wavesCleared, long ticks) {
}
