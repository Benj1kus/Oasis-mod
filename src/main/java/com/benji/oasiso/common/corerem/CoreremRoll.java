package com.benji.oasiso.common.corerem;


public final class CoreremRoll {
    public static final int HAPPY = 0, ANGER = 1, SAD = 2;
    public static final int USE_COOLDOWN = 20 * 10, SOUL_COOLDOWN = 20 * 30;

    public static int available(long now, long[] readyAt) {
        int mask = 0;
        for (int i = 0; i < 3; i++) if (readyAt[i] <= now) mask |= 1 << i;
        return mask;
    }

    public static int remaining(long now, long readyAt) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, readyAt - now));
    }

    public static int emotionAt(int mask, int step) {
        int count = Integer.bitCount(mask & 7);
        if (count == 0) return -1;
        int ordinal = Math.floorMod(step, count);
        for (int i = 0; i < 3; i++) {
            if ((mask & (1 << i)) != 0 && ordinal-- == 0) return i;
        }
        throw new AssertionError();
    }

    public static int steps(int mask, int winningOrdinal) {
        int count = Integer.bitCount(mask & 7);
        if (count == 0 || winningOrdinal < 0 || winningOrdinal >= count)
            throw new IllegalArgumentException("No available emotion / invalid winner");
        return (12 / count) * count + winningOrdinal;
    }

    public static int delay(int step, int steps) {
        double t = step / (double) Math.max(1, steps);
        return 2 + (int) Math.floor(8 * t * t);
    }

    public static int duration(int steps) {
        int ticks = 0;
        for (int i = 0; i <= steps; i++) ticks += delay(i, steps);
        return ticks;
    }

    public static int stepAt(int age, int steps) {
        for (int i = 0; i < steps; i++) {
            int delay = delay(i, steps);
            if (age < delay) return i;
            age -= delay;
        }
        return steps;
    }

    private CoreremRoll() {
    }
}
