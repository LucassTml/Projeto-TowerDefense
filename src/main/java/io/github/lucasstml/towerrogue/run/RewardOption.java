package io.github.lucasstml.towerrogue.run;

import io.github.lucasstml.towerrogue.content.Rarity;

/**
 * One card on the reward screen. Options sharing a {@code group} are mutually exclusive
 * ("pick 1 of 3"); taking one resolves the whole group.
 */
public final class RewardOption {
    public enum Kind { NEW_TOWER, UPGRADE_TOWER, RELIC, GOLD }

    public static final int GROUP_DRAFT = 0;
    public static final int GROUP_RELIC = 1;

    public final Kind kind;
    public final String id;
    public final int amount;
    public final int group;
    public final Rarity rarity;
    public boolean taken;

    public RewardOption(Kind kind, String id, int amount, int group, Rarity rarity) {
        this.kind = kind;
        this.id = id;
        this.amount = amount;
        this.group = group;
        this.rarity = rarity;
    }
}
