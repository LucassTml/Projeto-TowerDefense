package io.github.lucasstml.towerrogue.core;

import java.util.Locale;

/**
 * Converts run seeds to short, shareable codes (Crockford base-32, e.g. {@code 7KQ2MX9TAB})
 * and back. Any other text typed by the player (e.g. "lucas") is hashed into a seed, so
 * every string is a valid seed.
 */
public final class SeedCodec {
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    /** Seeds are limited to 50 bits so codes are at most 10 characters long. */
    public static final long SEED_MASK = (1L << 50) - 1;

    private SeedCodec() {
    }

    public static long randomSeed() {
        return Rng.mix(System.nanoTime() ^ Rng.mix(System.currentTimeMillis())) & SEED_MASK;
    }

    public static String encode(long seed) {
        seed &= SEED_MASK;
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(ALPHABET.charAt((int) (seed & 31)));
            seed >>>= 5;
        } while (seed != 0);
        while (sb.length() < 10) sb.append('0');
        return sb.reverse().toString();
    }

    public static long decode(String text) {
        String t = text.trim().toUpperCase(Locale.ROOT);
        if (t.isEmpty()) return randomSeed();
        if (t.length() <= 10 && t.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0)) {
            long v = 0;
            for (int i = 0; i < t.length(); i++) v = (v << 5) | ALPHABET.indexOf(t.charAt(i));
            return v & SEED_MASK;
        }
        return Rng.mix(Rng.fnv1a(t)) & SEED_MASK;
    }
}
