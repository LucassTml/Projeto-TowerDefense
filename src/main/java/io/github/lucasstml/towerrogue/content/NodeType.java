package io.github.lucasstml.towerrogue.content;

/** Node kinds on the run map. */
public enum NodeType {
    COMBAT("Combat"),
    ELITE("Elite"),
    EVENT("Unknown"),
    SHOP("Merchant"),
    REST("Campfire"),
    TREASURE("Treasure"),
    BOSS("Boss");

    public final String label;

    NodeType(String label) {
        this.label = label;
    }
}
