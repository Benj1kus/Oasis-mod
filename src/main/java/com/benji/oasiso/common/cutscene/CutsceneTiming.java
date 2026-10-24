package com.benji.oasiso.common.cutscene;

import com.google.gson.JsonObject;


public final class CutsceneTiming {
    public static double half(JsonObject json) {
        return value(json, "transition_seconds", 1.2, .4, 4);
    }

    public static double hold(JsonObject json) {
        return value(json, "transition_black_hold", .12, .05, 1);
    }

    public static double total(JsonObject json) {
        return 2 * half(json) + hold(json);
    }

    public static int preparationTicks(JsonObject json) {
        return (int) Math.ceil(total(json) * 20) + 6;
    }

    public static double coverage(double time, double half, double hold) {
        if (time <= 0) return 0;
        if (time < half) return time / half;
        if (time <= half + hold) return 1;
        return Math.max(0, 1 - (time - half - hold) / half);
    }

    private static double value(JsonObject j, String key, double fallback, double min, double max) {
        double v = j.has(key) ? j.get(key).getAsDouble() : fallback;
        return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : fallback;
    }

    private CutsceneTiming() {
    }
}
