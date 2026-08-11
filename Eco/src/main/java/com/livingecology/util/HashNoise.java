package com.livingecology.util;

public final class HashNoise {
    private HashNoise() {}

    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public static long combine(long seed, long a, long b) {
        return mix64(seed ^ mix64(a + 0x9e3779b97f4a7c15L) ^ Long.rotateLeft(mix64(b), 23));
    }

    public static double unit(long seed) {
        long bits = mix64(seed) >>> 11;
        return bits * 0x1.0p-53;
    }

    public static int bounded(long seed, int bound) {
        if (bound <= 1) return 0;
        return (int) Math.floor(unit(seed) * bound);
    }

    public static int signed(long seed, int magnitude) {
        if (magnitude <= 0) return 0;
        return bounded(seed, magnitude * 2 + 1) - magnitude;
    }
}
