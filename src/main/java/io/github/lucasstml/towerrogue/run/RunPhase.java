package io.github.lucasstml.towerrogue.run;

/** Top-level state machine of a run. Each phase maps to one screen. */
public enum RunPhase {
    MAP, BATTLE, REWARD, SHOP, EVENT, REST, VICTORY, DEFEAT;

    public boolean finished() {
        return this == VICTORY || this == DEFEAT;
    }
}
