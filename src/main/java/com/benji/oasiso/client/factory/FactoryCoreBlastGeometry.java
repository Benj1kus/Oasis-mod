package com.benji.oasiso.client.factory;

public final class FactoryCoreBlastGeometry {
    public interface Sink {
        void vertex(double x, double y, double z, float phase, float kind, float heat, float life, float u, float v);
    }

    private record P(double x, double y, double z) {
        P add(P b) {
            return new P(x + b.x, y + b.y, z + b.z);
        }

        P sub(P b) {
            return new P(x - b.x, y - b.y, z - b.z);
        }

        P mul(double v) {
            return new P(x * v, y * v, z * v);
        }

        double dot(P b) {
            return x * b.x + y * b.y + z * b.z;
        }

        P cross(P b) {
            return new P(y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y * b.x);
        }

        P unit() {
            return mul(1 / Math.max(1e-9, Math.sqrt(dot(this))));
        }
    }

    private interface Path {
        P at(double t);
    }

    private static final double TAU = Math.PI * 2;

    public static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    private static double smooth(double x) {
        x = clamp(x);
        return x * x * (3 - 2 * x);
    }

    private static double hash(int n) {
        double x = Math.sin(n * 127.1 + 31.7) * 43758.5453;
        return x - Math.floor(x);
    }

    public static void charge(Sink out, double seconds, long seed, double cameraX, double cameraY, double cameraZ, boolean reduced) {
        if (seconds < 0 || seconds >= 8) return;
        P camera = new P(cameraX, cameraY, cameraZ);
        double strength = smooth(seconds / .65), q = seconds / 8;
        int count = reduced ? 18 : 30;
        for (int i = 0; i < count; i++) {
            int id = i + (int) (seed & 1023);
            double offset = hash(id) * 2.1;
            double age = (seconds + offset) % 2.1;
            double tail = Math.min(age, .42);
            double life = strength * smooth(age / .12) * smooth((2.1 - age) / .25);
            double birth = seconds - age;
            Path path = u -> orbit(birth + u, u, id);
            ribbon(out, path, Math.max(0, age - tail), age, .15 + .15 * hash(id + 81), camera, (float) hash(id), (float) life, 14);
        }
        for (int i = 0; i < 4; i++) {
            final int j = i;
            double rotation = seconds * .85 + seconds * seconds * seconds * .036 + i * TAU / 4;
            Path path = t -> {
                double a = rotation + t * TAU * 1.1;
                double r = 2.3 + .55 * Math.sin(t * Math.PI);
                double y = .25 + t * 4.6 + .35 * Math.sin(a * 2 + j);
                return new P(Math.cos(a) * r, y, Math.sin(a) * r);
            };
            ribbon(out, path, 0, 1, .075 + .10 * q, camera, (float) hash(i + 6), (float) (strength * .92), 48);
        }
    }

    private static P orbit(double t, double age, int id) {
        double phase = hash(id) * TAU;
        double a = phase + t * (.8 + hash(id + 9) * .8) + .038 * t * t * t;
        double r = 2.1 + hash(id + 7) * 1.5 + .22 * Math.sin(a * 1.3);
        P target = new P(Math.cos(a) * r, 2.35 + 2.1 * Math.sin(a * .65 + phase), Math.sin(a) * r);
        double emerge = smooth(age / .35);
        return new P(0, 3.8, 0).mul(1 - emerge).add(target.mul(emerge));
    }

    private static void ribbon(Sink out, Path path, double start, double end, double width, P camera, float phase, float life, int segments) {
        if (life < .002 || end - start < .0001) return;
        P previousL = null, previousR = null, previousSide = null;
        for (int i = 0; i <= segments; i++) {
            double f = i / (double) segments, t = start + (end - start) * f;
            P p = path.at(t), tangent = path.at(t + .001).sub(path.at(t - .001)).unit();
            P side = tangent.cross(p.sub(camera));
            if (side.dot(side) < 1e-8) side = tangent.cross(Math.abs(tangent.y) < .9 ? new P(0, 1, 0) : new P(1, 0, 0));
            side = side.unit();
            if (previousSide != null && side.dot(previousSide) < 0) side = side.mul(-1);
            P left = p.sub(side.mul(width)), right = p.add(side.mul(width));
            if (i > 0) {
                put(out, previousL, phase, .5F, 1, life, 0, (i - 1F) / segments);
                put(out, previousR, phase, .5F, 1, life, 1, (i - 1F) / segments);
                put(out, right, phase, .5F, 1, life, 1, (float) f);
                put(out, left, phase, .5F, 1, life, 0, (float) f);
            }
            previousL = left;
            previousR = right;
            previousSide = side;
        }
    }

    public static void blast(Sink out, double age, long seed, double cameraX, double cameraY, double cameraZ, boolean reduced) {
        if (age < 0 || age >= 10) return;
        int salt = (int) (seed & 1023);
        double grow = 1 - Math.exp(-age * 2.5);
        double rise = 1 - Math.exp(-age * 1.9);
        double body = 1 - smooth((age - 3.2) / 4.8);
        double drift = Math.max(0, age - 3) * .3;
        float life = (float) (smooth(age / .055) * body);
        int slices = reduced ? 16 : 24, stacks = reduced ? 8 : 12;
        if (age < .65)
            blob(out, 0, 2, 0, 5 * grow, 5 * grow, 5 * grow, (float) hash(salt), 1, (float) (1 - age / .65), slices, stacks, age);
        if (life > .002) {
            for (int i = 0; i < 9; i++) {
                double h = (i + .45) / 9.0;
                double y = h * 28 * rise;
                double w = (2.8 - 1.6 * h + .3 * Math.sin(i * 1.7 - age * 3)) * grow;
                blob(out, .35 * Math.sin(i * .7 + age * 2), y, .35 * Math.cos(i * .8 + age * 1.7), w, 3.6 * rise, w, (float) hash(i + salt), .78F, life, slices, stacks, age);
            }
            blob(out, 0, 29.5 * rise + drift, 0, 8.2 * grow, 5.8 * grow, 8.2 * grow, (float) hash(salt + 18), .94F, life, slices, stacks, age);
            for (int i = 0; i < 10; i++) {
                double a = i * TAU / 10 + age * .1;
                double r = 6.45 * grow;
                double y = (28.4 + Math.sin(i * 2.3) * 1.2) * rise + drift;
                double size = (3.1 + .45 * hash(i + salt)) * grow;
                blob(out, Math.cos(a) * r, y, Math.sin(a) * r, size, 4.0 * grow, size, (float) hash(i + salt + 33), .82F, life, slices, stacks, age);
            }
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8 + age * .12, r = (4.4 + .25 * age) * grow;
                blob(out, Math.cos(a) * r, 1.5 * grow, Math.sin(a) * r, 2.4 * grow, 2.5 * grow, 2.4 * grow, (float) hash(i + salt + 71), .35F, life * .85F, slices, stacks, age);
            }
            ring(out, Math.min(11.5, age * 23), 26 * rise, .22 + .25 * grow, age * .7, (float) (body * smooth(age / .07)), salt);
            if (age < 2.5)
                ring(out, Math.min(9, age * 15), 7.5 * rise, .15, -age, (float) (1 - smooth((age - 1.1) / 1.4)), salt + 5);
        }
        P camera = new P(cameraX, cameraY, cameraZ);
        int shards = reduced ? 18 : 32;
        for (int i = 0; i < shards; i++) {
            int id = i + salt;
            double birth = .15 + hash(id + 17) * 1.35;
            double t = age - birth;
            if (t < 0) continue;
            double end = 5.5 + hash(id + 8) * 3.4;
            float opacity = (float) (smooth(t / .2) * (1 - smooth((age - end) / 1.5)) * (1 - smooth(age - 9)));
            double a = hash(id + 5) * TAU;
            Path path = u -> {
                double r = 2 + u * (1.7 + hash(id + 6) * 1.1);
                double angle = a + .2 * u + .12 * Math.sin(u * 1.6 + id);
                double y = (15 + hash(id + 2) * 13) * (1 - Math.exp(-birth * 1.9)) + u * (2 + hash(id + 3) * 2) - .32 * u * u;
                return new P(Math.cos(angle) * r, y, Math.sin(angle) * r);
            };
            ribbon(out, path, Math.max(0, t - .75), t, .22 + .28 * hash(id + 1), camera, (float) hash(id), opacity, 12);
        }
    }

    private static void blob(Sink out, double x, double y, double z, double rx, double ry, double rz, float phase, float heat, float life, int slices, int stacks, double age) {
        if (rx < .005 || ry < .005 || life < .002) return;
        for (int j = 0; j < stacks; j++)
            for (int i = 0; i < slices; i++) {
                ballVertex(out, x, y, z, rx, ry, rz, i / (double) slices, j / (double) stacks, phase, heat, life, age);
                ballVertex(out, x, y, z, rx, ry, rz, (i + 1.0) / slices, j / (double) stacks, phase, heat, life, age);
                ballVertex(out, x, y, z, rx, ry, rz, (i + 1.0) / slices, (j + 1.0) / stacks, phase, heat, life, age);
                ballVertex(out, x, y, z, rx, ry, rz, i / (double) slices, (j + 1.0) / stacks, phase, heat, life, age);
            }
    }

    private static void ballVertex(Sink out, double x, double y, double z, double rx, double ry, double rz, double u, double v, float phase, float heat, float life, double age) {
        double a = u * TAU, b = v * Math.PI;
        double bump = 1 + .065 * Math.sin(a * 5 + phase * TAU + age * .8) * Math.sin(b * 3) + .035 * Math.sin(a * 9 - b * 4 - age * 1.3) * Math.sin(b);
        out.vertex(x + Math.cos(a) * Math.sin(b) * rx * bump, y + Math.cos(b) * ry * bump, z + Math.sin(a) * Math.sin(b) * rz * bump, phase, 0, heat, life, (float) u, (float) v);
    }

    private static void ring(Sink out, double radius, double y, double width, double rotation, float life, int seed) {
        if (radius < .01 || life < .002) return;
        for (int i = 0; i < 160; i++) {
            double a = i * TAU / 160 + rotation, b = (i + 1) * TAU / 160 + rotation;
            double ra = radius * (1 + .022 * Math.sin(a * 9 + rotation)), rb = radius * (1 + .022 * Math.sin(b * 9 + rotation));
            float phase = (float) hash(seed);
            put(out, new P(Math.cos(a) * (ra - width), y, Math.sin(a) * (ra - width)), phase, 1, 1, life, 0, i / 160F);
            put(out, new P(Math.cos(a) * (ra + width), y, Math.sin(a) * (ra + width)), phase, 1, 1, life, 1, i / 160F);
            put(out, new P(Math.cos(b) * (rb + width), y, Math.sin(b) * (rb + width)), phase, 1, 1, life, 1, (i + 1) / 160F);
            put(out, new P(Math.cos(b) * (rb - width), y, Math.sin(b) * (rb - width)), phase, 1, 1, life, 0, (i + 1) / 160F);
        }
    }

    private static void put(Sink out, P p, float phase, float kind, float heat, float life, float u, float v) {
        out.vertex(p.x, p.y, p.z, phase, kind, heat, life, u, v);
    }

    private FactoryCoreBlastGeometry() {
    }
}
