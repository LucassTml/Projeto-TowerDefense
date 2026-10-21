package io.github.lucasstml.towerrogue.content;

/** Status effects towers can apply. Magnitude meaning depends on the type (see StatusEffects). */
public enum StatusType {
    /** magnitude = fraction of speed removed (0.3 = 30% slower). Strongest wins. */
    SLOW("Slow", 0x7FD4FF),
    /** Stun: enemy stops. Followed by a short immunity window so it cannot be perma-frozen. */
    FREEZE("Freeze", 0xBFF0FF),
    /** magnitude = damage per second per stack. Stacks up to a cap. */
    BURN("Burn", 0xFF8A3D),
    /** magnitude = damage per second per stack; ignores armor. */
    POISON("Poison", 0x8FE36A),
    /** magnitude = armor removed per stack. */
    SHRED("Shred", 0xD9D4C7),
    /** magnitude = extra damage taken (0.2 = +20%). Strongest wins. */
    VULNERABLE("Vulnerable", 0xC08CFF);

    public final String label;
    public final int color;

    StatusType(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
