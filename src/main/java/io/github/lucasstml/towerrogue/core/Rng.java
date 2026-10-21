package io.github.lucasstml.towerrogue.core;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Deterministic pseudo-random generator based on SplitMix64.
 *
 * <p>Why not {@link java.util.Random}? Its algorithm is also deterministic, but its
 * 48-bit state is not exposed, so it cannot be saved and restored. Here the whole state
 * is one {@code long}: a run can be saved mid-way and every random stream resumes
 * exactly where it stopped, which is what makes "same seed = same run" hold across
 * save/load.
 *
 * <p>The game never shares one generator between unrelated systems. Each system gets
 * its own stream derived from the run seed ({@link #derive}), so an extra roll in the
 * shop never changes which events or enemies appear later.
 */
public final class Rng {
    private long state;

    public Rng(long seed) {
        this.state = seed;
    }

    /** Creates an independent stream for a named subsystem, e.g. {@code derive(seed, "map")}. */
    public static Rng derive(long seed, String label) {
        return new Rng(deriveSeed(seed, label));
    }

    public static long deriveSeed(long seed, String label) {
        return mix(seed ^ fnv1a(label));
    }

    /** SplitMix64 finalizer: a strong 64-bit bit mixer. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** 64-bit FNV-1a hash, stable across JVMs (unlike {@link String#hashCode} widened). */
    public static long fnv1a(String text) {
        long h = 0xcbf29ce484222325L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xff);
            h *= 0x100000001b3L;
        }
        return h;
    }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        return mix(state);
    }

    /** Uniform int in [0, bound). Uses rejection sampling to avoid modulo bias. */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive: " + bound);
        long limit = Long.MAX_VALUE - (Long.MAX_VALUE % bound);
        long r;
        do {
            r = nextLong() >>> 1;
        } while (r >= limit);
        return (int) (r % bound);
    }

    /** Uniform int in [min, max] (inclusive). */
    public int range(int min, int max) {
        return min + nextInt(max - min + 1);
    }

    /** Uniform double in [0, 1) with 53 bits of precision. */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public double range(double min, double max) {
        return min + (max - min) * nextDouble();
    }

    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0;
    }

    public boolean chance(double probability) {
        return nextDouble() < probability;
    }

    public <T> T pick(List<T> items) {
        return items.get(nextInt(items.size()));
    }

    /** Picks an element with probability proportional to its weight; null if all weights are 0. */
    public <T> T weighted(List<T> items, ToDoubleFunction<T> weight) {
        double total = 0;
        for (T t : items) total += Math.max(0, weight.applyAsDouble(t));
        if (total <= 0) return null;
        double roll = nextDouble() * total;
        for (T t : items) {
            roll -= Math.max(0, weight.applyAsDouble(t));
            if (roll < 0) return t;
        }
        return items.get(items.size() - 1);
    }

    /** Fisher–Yates shuffle. */
    public <T> void shuffle(List<T> items) {
        for (int i = items.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T tmp = items.get(i);
            items.set(i, items.get(j));
            items.set(j, tmp);
        }
    }

    public long getState() {
        return state;
    }

    public void setState(long state) {
        this.state = state;
    }
}
