package com.benji.oasiso.client.cutscene;

import com.google.gson.*;
import org.joml.Vector3f;

public final class CameraTrack {
    public static Vector3f sample(JsonArray keys, double t) {
        int n = keys.size();
        if (n == 0) return new Vector3f();
        int i = 0;
        while (i < n - 1 && keys.get(i + 1).getAsJsonArray().get(0).getAsDouble() <= t) i++;
        if (i == n - 1 || t <= time(keys, 0)) return point(keys, i);
        float a = (float) ((t - time(keys, i)) / (time(keys, i + 1) - time(keys, i)));
        a = Math.max(0, Math.min(1, a));
        Vector3f p0 = point(keys, Math.max(0, i - 1)), p1 = point(keys, i), p2 = point(keys, i + 1), p3 = point(keys, Math.min(n - 1, i + 2));
        Vector3f out = new Vector3f();
        for (int axis = 0; axis < 3; axis++) {
            float v0 = p0.get(axis), v1 = p1.get(axis), v2 = p2.get(axis), v3 = p3.get(axis);
            out.setComponent(axis, .5f * ((2 * v1) + (-v0 + v2) * a + (2 * v0 - 5 * v1 + 4 * v2 - v3) * a * a + (-v0 + 3 * v1 - 3 * v2 + v3) * a * a * a));
        }
        return out;
    }

    private static double time(JsonArray a, int i) {
        return a.get(i).getAsJsonArray().get(0).getAsDouble();
    }

    private static Vector3f point(JsonArray a, int i) {
        var row = a.get(i).getAsJsonArray();
        return new Vector3f(row.get(1).getAsFloat(), row.get(2).getAsFloat(), row.get(3).getAsFloat());
    }
}
