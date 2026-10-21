package io.github.lucasstml.towerrogue.battle;

/**
 * Facts published on the battle {@link io.github.lucasstml.towerrogue.core.EventBus}.
 * {@link Hit} (damage about to be dealt) is also published and is mutable on purpose.
 */
public final class BattleEvents {
    private BattleEvents() {
    }

    public record EnemySpawned(Enemy enemy) {
    }

    public record EnemyKilled(Enemy enemy, Tower killer) {
    }

    /** Mutable: hooks may reduce {@link #damage} (e.g. a shield relic sets it to 0). */
    public static final class EnemyLeaked {
        public final Enemy enemy;
        public int damage;

        EnemyLeaked(Enemy enemy, int damage) {
            this.enemy = enemy;
            this.damage = damage;
        }
    }

    public record WaveStarted(int index, boolean early) {
    }

    public record WaveCleared(int index) {
    }

    public record TowerBuilt(Tower tower) {
    }
}
