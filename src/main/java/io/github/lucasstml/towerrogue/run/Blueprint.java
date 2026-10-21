package io.github.lucasstml.towerrogue.run;

/** A tower type the player can build in this run, with its permanent tier (0 = base, max 2). */
public final class Blueprint {
    public static final int MAX_TIER = 2;

    public final String towerId;
    public int tier;

    public Blueprint(String towerId, int tier) {
        this.towerId = towerId;
        this.tier = tier;
    }

    public boolean canUpgrade() {
        return tier < MAX_TIER;
    }

    public static String tierName(int tier) {
        return switch (tier) {
            case 0 -> "";
            case 1 -> " Mk II";
            default -> " Mk III";
        };
    }
}
