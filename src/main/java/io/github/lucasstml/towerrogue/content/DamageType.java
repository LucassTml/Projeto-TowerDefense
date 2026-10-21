package io.github.lucasstml.towerrogue.content;

/** Damage channels. Physical is reduced by flat armor; the others by percentage resistances. */
public enum DamageType {
    PHYSICAL("Physical", 0xD9D4C7),
    FIRE("Fire", 0xFF8A3D),
    FROST("Frost", 0x7FD4FF),
    ARCANE("Arcane", 0xC08CFF),
    POISON("Poison", 0x8FE36A);

    public final String label;
    public final int color;

    DamageType(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
