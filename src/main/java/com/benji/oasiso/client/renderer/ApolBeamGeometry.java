package com.benji.oasiso.client.renderer;

import java.util.ArrayList;
import java.util.List;

public final class ApolBeamGeometry {
    private static final double TAU = Math.PI * 2;

    public record Point(double x, double y, double z, float u, float v) {
    }

    public record Face(Point a, Point b, Point c, Point d, int kind, float tint, float alpha) {
        public Point[] points() {
            return new Point[]{a, b, c, d};
        }
        public double centerX() {
            return (a.x + b.x + c.x + d.x) * .25;
        }
        public double centerY() {
            return (a.y + b.y + c.y + d.y) * .25;
        }
        public double centerZ() {
            return (a.z + b.z + c.z + d.z) * .25;
        }
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }

    private static Point p(double x, double y, double z, double u, double v) {
        return new Point(x, y, z, (float) u, (float) v);
    }

    private static void quad(List<Face> out, Point a, Point b, Point c, Point d, int kind, double tint, double alpha) {
        out.add(new Face(a, b, c, d, kind, (float) tint, (float) alpha));
    }

    private static Point tube(double angle, double r, double y, double u, double v) {
        return p(Math.cos(angle) * r, y, Math.sin(angle) * r, u, v);
    }

    private static double profile(double y) {
        return .42 + .58 * Math.sin(Math.PI * (.12 + y * .8));
    }

    public static List<Face> build(double age, int seed) {
        List<Face> out = new ArrayList<>(2400);
        if (age <= 0 || age >= 6) return out;
        double growth = smooth(age / 1.15), height = 10 * smooth(age / .85);
        double opening = .06 + .94 * growth;
        double speed = 2.0;
        double clock = Math.floor(age * 24 * speed) / 24.0, phase = seed * .073;
        double[] radii = {.36, .65, 1.05};
        for (int layer = 0; layer < radii.length; layer++) {
            for (int i = 0; i < 48; i++)
                for (int j = 0; j < 12; j++) {
                    double v0 = j / 12.0, v1 = (j + 1) / 12.0;
                    double a0 = TAU * i / 48, a1 = TAU * (i + 1) / 48;
                    double r0 = radii[layer] * opening * profile(v0), r1 = radii[layer] * opening * profile(v1);

                    double layerAlpha = layer == 0 ? 0.95 : layer == 1 ? 0.65 : 0.35;

                    quad(out, tube(a0, r0, height * v0, i / 48.0, v0), tube(a1, r0, height * v0, (i + 1) / 48.0, v0), tube(a1, r1, height * v1, (i + 1) / 48.0, v1), tube(a0, r1, height * v1, i / 48.0, v1), 0, layer / 2.0, layerAlpha);
                }
        }
        for (int i = 0; i < 20; i++) {
            double theta = i * 2.399963 + phase;
            double radius = .95 + fract(i * .618033) * 2.8;
            double width = .035 + fract(i * .381966) * .13;
            double tint = fract(i * .381966 + seed * .01);
            for (int j = 0; j < 12; j++) {
                double v0 = j / 12.0, v1 = (j + 1) / 12.0;
                Point[] q = new Point[4];
                double[] vs = {v0, v0, v1, v1};
                for (int k = 0; k < 4; k++) {
                    double v = vs[k], a = theta + v * (1.4 + Math.sin(i)) + clock * (i % 2 == 0 ? .55 : -.4);
                    double r = radius * opening * (.22 + .78 * Math.sin(Math.PI * (.06 + v * .85)));
                    r *= 1 + .10 * Math.sin(v * 12 - clock * 5 + i);
                    double side = (k == 0 || k == 3) ? -1 : 1;
                    q[k] = tube(a + side * width, r, height * v, (side + 1) * .5, v);
                }
                quad(out, q[0], q[1], q[2], q[3], 1, tint, .75);
            }
        }
        for (int ring = 0; ring < 5; ring++) {
            double travel = fract(clock * .24 + ring / 5.0);
            double radius = (1.2 + 4.5 * travel) * opening;
            double cy = .12 + (height - .75) * travel;
            double tilt = .06 + .07 * Math.sin(ring * 1.8 + clock);
            double spin = phase + ring * 1.3 + clock * (ring % 2 == 0 ? .8 : -.65);
            double alpha = .85 * smooth(travel / .09) * (1 - smooth((travel - .86) / .14));
            for (int i = 0; i < 80; i++) {
                if ((i + ring * 7) % 29 < 4) continue;
                double a = TAU * i / 80 + spin, b = TAU * (i + 1) / 80 + spin;
                double thickness = .10 + .12 * (.5 + .5 * Math.sin(a * 3 + clock * 2));
                for (int shell = 0; shell < 2; shell++) {
                    double w = thickness * (shell == 0 ? 2.3 : 1), al = alpha * (shell == 0 ? .17 : 1);
                    double inner = Math.max(0, radius - w), outer = Math.min(6, radius + w);
                    quad(out, ringPoint(a, inner, cy, tilt, spin, i / 80.0, 0), ringPoint(a, outer, cy, tilt, spin, i / 80.0, 1), ringPoint(b, outer, cy, tilt, spin, (i + 1) / 80.0, 1), ringPoint(b, inner, cy, tilt, spin, (i + 1) / 80.0, 0), 2, fract(ring * .27), al);
                }
            }
        }
        for (int i = 0; i < 64; i++) {
            double travel = fract(i * .618033 + clock * (.18 + fract(i * .17) * .12));
            double angle = i * 2.399963 + phase + clock * .22;
            double radius = (1.1 + fract(i * .754877) * 4.55) * opening;
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius, y = height * travel;
            double w = (.025 + fract(i * .4343) * .09) * growth, h = w * (2 + fract(i * .311) * 4);
            double alpha = smooth(travel / .08) * (1 - smooth((travel - .88) / .12));
            for (int side = 0; side < 2; side++) {
                double a = angle + side * Math.PI / 2, dx = Math.cos(a) * w, dz = Math.sin(a) * w;
                quad(out, p(x - dx, Math.max(0, y - h), z - dz, 0, 0), p(x + dx, Math.max(0, y - h), z + dz, 1, 0), p(x + dx, Math.min(height, y + h), z + dz, 1, 1), p(x - dx, Math.min(height, y + h), z - dz, 0, 1), i % 5 == 0 ? 4 : 3, fract(i * .293), alpha);
            }
        }
        return out;
    }

    private static Point ringPoint(double angle, double radius, double y, double tilt, double spin, double u, double v) {
        return tube(angle, radius, Math.max(.03, y + Math.sin(angle - spin) * radius * tilt), u, v);
    }

    private ApolBeamGeometry() {
    }
}
