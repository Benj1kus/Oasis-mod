package com.benji.oasiso.client.model;

import org.joml.Vector3f;


public final class EntropySpiderLegSolver {
    private EntropySpiderLegSolver() {
    }

    public static Vector3f[] solve(Vector3f hip, Vector3f requested, float[] lengths, Vector3f up) {
        int n = lengths.length;
        float total = 0;
        for (float length : lengths) total += length;
        Vector3f direction = new Vector3f(requested).sub(hip);
        float distance = direction.length();
        if (distance < .0001F) direction.set(1, 0, 0);
        else direction.div(distance);
        distance = Math.max(.10F, Math.min(total * .985F, distance));
        Vector3f target = new Vector3f(direction).mul(distance).add(hip);
        Vector3f pole = new Vector3f(up).sub(new Vector3f(direction).mul(up.dot(direction)));

        if (pole.lengthSquared() < .0001F) pole.set(0, 0, 1).cross(direction);
        if (pole.lengthSquared() < .0001F) pole.set(1, 0, 0).cross(direction);
        pole.normalize();
        int split = 2;
        float upper = 0, lower = 0;
        for (int i = 0; i < n; i++)
            if (i < split) upper += lengths[i];
            else lower += lengths[i];
        float along = (distance * distance + upper * upper - lower * lower) / (2 * distance);
        float height = (float) Math.sqrt(Math.max(.01F, upper * upper - along * along));

        Vector3f knee = new Vector3f(hip).add(new Vector3f(direction).mul(along)).add(new Vector3f(pole).mul(height));
        Vector3f[] joints = new Vector3f[n + 1];
        joints[0] = new Vector3f(hip);
        float traversed = 0;
        for (int i = 1; i <= n; i++) {
            traversed += lengths[i - 1];
            if (i <= split) joints[i] = new Vector3f(hip).lerp(knee, traversed / upper);
            else joints[i] = new Vector3f(knee).lerp(target, (traversed - upper) / lower);
            if (i < n) joints[i].add(new Vector3f(pole).mul(.06F * (float) Math.sin(Math.PI * i / n)));
        }
        for (int iteration = 0; iteration < 48; iteration++) {
            joints[n].set(target);
            for (int i = n - 1; i >= 0; i--) place(joints[i], joints[i + 1], lengths[i]);
            joints[0].set(hip);
            for (int i = 1; i <= n; i++) place(joints[i], joints[i - 1], lengths[i - 1]);
            if (joints[n].distanceSquared(target) < .00000001F) break;
        }
        return joints;
    }

    private static void place(Vector3f point, Vector3f fixed, float length) {
        Vector3f delta = new Vector3f(point).sub(fixed);
        if (delta.lengthSquared() < .00000001F) delta.set(0, 1, 0);
        point.set(fixed).add(delta.normalize(length));
    }
}
