package io.github.lucasstml.towerrogue.content;

public enum Rarity {
    STARTER("Starter", 0x9A9A9A),
    COMMON("Common", 0xD0D0D0),
    UNCOMMON("Uncommon", 0x5AB0FF),
    RARE("Rare", 0xF2C14E),
    BOSS("Boss", 0xC56CF0);

    public final String label;
    public final int color;

    Rarity(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
