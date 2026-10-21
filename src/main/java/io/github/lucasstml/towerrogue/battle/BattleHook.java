package io.github.lucasstml.towerrogue.battle;

/**
 * Extension point for code that reacts to battle events (relic effects). A fresh hook
 * instance is created per battle, so per-battle state (e.g. "3 shield charges") resets
 * naturally. Hooks subscribe to {@link Battle#bus()} in {@link #attach}.
 */
@FunctionalInterface
public interface BattleHook {
    void attach(Battle battle);
}
