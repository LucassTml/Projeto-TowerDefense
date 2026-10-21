package io.github.lucasstml.towerrogue.content;

/** How a tower turns its stats into an effect on the battlefield. */
public enum AttackKind {
    /** Fires projectiles (homing or ground-targeted), optionally with splash and multishot. */
    PROJECTILE,
    /** Instant hit that jumps between nearby enemies. */
    CHAIN,
    /** Instant area pulse that hits every enemy in range. */
    PULSE,
    /** No attack; buffs nearby towers. */
    SUPPORT,
    /** No attack; generates mana while a wave is running. */
    GENERATOR
}
