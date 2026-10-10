package com.benji.oasiso.client.geyser;

import java.util.ArrayList;
import java.util.List;

public final class BigGeyserGeometry {
    public record Point(double x, double y, double z, float u, float v) {
    }

    public record Face(Point[] points, float layer) {
        public double distanceSquared(double ox, double oy, double oz) {
            double x = ox, y = oy, z = oz;
            for (Point p : points) {
                x += p.x / 4;
                y += p.y / 4;
                z += p.z / 4;
            }
            return x * x + y * y + z * z;
        }
    }

    public static List<Face> build(double seconds, int seed, double maxHeight) {
        List<Face> result = new ArrayList<>();
        if (seconds <= 0 || seconds >= 2 || maxHeight <= 0) return result;
        double growth = 1 - Math.pow(1 - Math.min(1, seconds / .18), 3);
        double height = Math.min(5, maxHeight) * growth;
        int sides = 12, segments = 22;
        for (int layer = 0; layer < 4; layer++)
            for (int j = 0; j < segments; j++)
                for (int i = 0; i < sides; i++) {
                    result.add(new Face(new Point[]{point(i / (double) sides, j / (double) segments, layer, height, seconds, seed), point((i + 1) / (double) sides, j / (double) segments, layer, height, seconds, seed), point((i + 1) / (double) sides, (j + 1) / (double) segments, layer, height, seconds, seed), point(i / (double) sides, (j + 1) / (double) segments, layer, height, seconds, seed)}, layer / 3F));
                }

        for (int i = 0; i < 16; i++) {
            double phase = fract(i * .61803398875 + seed * .013 + seconds * (.65 + (i % 3) * .12));
            double y = height * (.08 + .83 * phase);
            double angle = i * 2.399963 + seed * .1 + seconds * .55;
            double radius = .47 + .10 * Math.sin(i * 1.7 + seconds * 3);
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
            double size = (.028 + (i % 4) * .009) * (1 - .45 * phase) * growth;
            double tall = Math.min(size * (2.0 + (i % 3)), height * .06);
            for (int plane = 0; plane < 2; plane++) {
                double a = angle + plane * Math.PI / 2, dx = Math.cos(a) * size, dz = Math.sin(a) * size;
                result.add(new Face(new Point[]{p(x, y - tall, z, .5), p(x + dx, y, z + dz, 1), p(x, y + tall, z, .5), p(x - dx, y, z - dz, 0)}, .1F));
            }
        }
        return result;
    }

    private static Point p(double x, double y, double z, double u) {
        return new Point(x, y, z, (float) u, (float) (y / 5));
    }

    private static Point point(double u, double v, int layer, double height, double time, int seed) {
        double a = u * Math.PI * 2;
        double tip = layer == 0 ? 1 : .87 + .13 * Math.cos(a * 3 + layer * .9 + seed * .11);
        double y = v * height * tip;
        double bendX = triangle(y * .43 - time * .8 + seed * .03);
        double bendZ = triangle(y * .36 - time * .67 + seed * .017 + .3);
        double shift = Math.pow(v, .75) * .085;
        double envelope = (.55 + .45 * Math.min(1, v * 5)) * (1 - .76 * Math.pow(v, 4));
        double radii = switch (layer) {
            case 0 -> .19;
            case 1 -> .32;
            case 2 -> .41;
            default -> .49;
        };
        double ridge = 1 + .16 * triangle(v * 4 - time * 2.3 + u * 3 + seed * .01);
        double teeth = 1 + .10 * Math.cos(a * 3 + seed * .2) * Math.sin(v * Math.PI);
        double radius = radii * envelope * ridge * teeth;
        return new Point(Math.cos(a) * radius + bendX * shift, y, Math.sin(a) * radius + bendZ * shift, (float) u, (float) (y / 5));
    }

    private static double fract(double x) {
        return x - Math.floor(x);
    }

    private static double triangle(double x) {
        return 1 - 4 * Math.abs(fract(x) - .5);
    }

    private BigGeyserGeometry() {
    }
}
