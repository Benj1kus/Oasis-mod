package com.benji.oasiso.client.renderer;

import java.util.ArrayList;
import java.util.List;


public final class NephritisMagmaMesh {
    public static final double MIN_HEIGHT = 3.2, MAX_HEIGHT = 8.5;
    private static final int NX = 30, NY = 90;
    private static final double HALF_WIDTH = 1.05;
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    public record Cell(double x0, double y0, double x1, double y1, int r, int g, int b, int a) {
    }

    public static double height(int variant) {
        return MIN_HEIGHT + (MAX_HEIGHT - MIN_HEIGHT) * hash(variant, 91);
    }

    public static List<Cell> build(int variant, double time) {
        double height = height(variant), phase = hash(variant, 7) * Math.PI * 2;
        double[][] drops = new double[18][3];
        for (int i = 0; i < drops.length; i++) {
            double progress = fract(time * (.09 + hash(variant, i + 30) * .06) + hash(variant, i + 100));
            drops[i][0] = Math.sin(progress * 9 + phase + i * 2.4) * (.18 + progress * .38);
            drops[i][1] = progress * (height + .5) - .2;
            drops[i][2] = (.12 + hash(variant, i + 200) * .13) * Math.min(1, progress * 12);
        }
        List<Cell> cells = new ArrayList<>();
        double dx = 2 * HALF_WIDTH / NX, dy = height / NY;
        for (int y = 0; y < NY; y++) {
            double sy = (y + .5) * dy, p = sy / height;
            double fade = 1 - smooth((p - .62) / .38);
            for (int x = 0; x < NX; x++) {
                if (fade <= (BAYER[(y & 3) * 4 + (x & 3)] + .5) / 16.0) continue;
                double sx = -HALF_WIDTH + (x + .5) * dx;
                double field = .06 / (sx * sx + sy * sy + .035);
                for (double[] d : drops) {
                    double xx = sx - d[0], yy = sy - d[1];
                    field += d[2] * d[2] / (xx * xx + yy * yy + .025);
                }
                for (int arm = 0; arm < 2; arm++) {
                    double angle = p * 15 - time * 1.7 + phase + arm * Math.PI;
                    double center = Math.sin(angle) * (.16 + .36 * p);
                    double width = .065 + .045 * (.5 + .5 * Math.cos(angle));
                    double dist = (sx - center) / width;
                    double pulse = .65 + .35 * Math.sin(p * 24 - time * 2.2 + arm * 2);
                    field += Math.exp(-dist * dist * .5) * pulse * 1.15;
                }
                if (field < .7) continue;
                boolean fill = field >= 1.12;
                int r = fill ? 14 : 91, g = fill ? 153 : 255, b = fill ? 149 : 226;
                cells.add(new Cell(-HALF_WIDTH + x * dx, y * dy, -HALF_WIDTH + (x + 1) * dx, (y + 1) * dy, r, g, b, (int) ((fill ? 210 : 245) * (.65 + .35 * fade))));
            }
        }
        return cells;
    }

    private static double smooth(double v) {
        v = Math.max(0, Math.min(1, v));
        return v * v * (3 - 2 * v);
    }

    private static double fract(double x) {
        return x - Math.floor(x);
    }

    private static double hash(int v, int n) {
        long x = v * 0x9E3779B97F4A7C15L + n * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 11) * 0x1.0p-53;
    }

    private NephritisMagmaMesh() {
    }
}
