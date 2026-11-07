package com.benji.oasiso.client.soulcast;

import java.util.Random;

/**
 * One texel is one model pixel. The same mask is used in both directions of the reveal.
 */
final class SoulCrackPattern {
    static final int SCALE = 16, RADIUS = 8, HALF = RADIUS * SCALE, SIZE = HALF * 2 + 1;
    static final int INNER = 0x27F5C2, EDGE = 0x1379A1;
    static final int WHITE = 0xF5FFFE;

    static double ditherThreshold(int x, int z) {
        return (BAYER[(x & 3) + 4 * (z & 3)] + 0.5) / 16.0;
    }

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    final int[] colors = new int[SIZE * SIZE];

    SoulCrackPattern(long seed) {
        Random rng = new Random(seed);
        double phase = rng.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 11; i++) {
            double bearing = phase + Math.PI * 2 * (i + rng.nextDouble() * .35) / 11;
            double length = HALF * (i == 0 ? 1 : .74 + rng.nextDouble() * .26);
            double x = 0, z = 0;
            for (int j = 1; j <= 10; j++) {
                double t = j / 10d, r = length * t;
                double skew = (rng.nextDouble() - .5) * .32 * (1 - .35 * t);
                double nx = r * Math.cos(bearing + skew), nz = r * Math.sin(bearing + skew);
                stroke(x, z, nx, nz, 2.45 - 1.85 * t);
                if (j == 3 || j == 5 || j == 7) {
                    double a = bearing + (rng.nextBoolean() ? 1 : -1) * (.45 + .50 * rng.nextDouble());
                    double bx = nx, bz = nz;
                    for (int k = 0; k < 3; k++) {
                        double step = 5 + rng.nextDouble() * 8, turn = a + (rng.nextDouble() - .5) * .65;
                        double ex = bx + step * Math.cos(turn), ez = bz + step * Math.sin(turn);
                        stroke(bx, bz, ex, ez, 1.25 - .27 * k);
                        bx = ex;
                        bz = ez;
                    }
                }
                x = nx;
                z = nz;
            }
        }
    }

    private void stroke(double x, double z, double ex, double ez, double core) {
        double dx = ex - x, dz = ez - z, len = dx * dx + dz * dz, outer = core + 1;
        for (int iz = Math.max(-HALF, (int) Math.floor(Math.min(z, ez) - outer)); iz <= Math.min(HALF, Math.ceil(Math.max(z, ez) + outer)); iz++) {
            for (int ix = Math.max(-HALF, (int) Math.floor(Math.min(x, ex) - outer)); ix <= Math.min(HALF, Math.ceil(Math.max(x, ex) + outer)); ix++) {
                if (ix * ix + iz * iz > HALF * HALF) continue;
                double t = Math.max(0, Math.min(1, ((ix - x) * dx + (iz - z) * dz) / len));
                double offX = ix - x - t * dx, offZ = iz - z - t * dz;
                double dist = Math.sqrt(offX * offX + offZ * offZ);
                if (dist > outer) continue;
                int idx = (iz + HALF) * SIZE + ix + HALF;
                boolean bright = dist <= core && (dist < core * .40 || BAYER[(ix & 3) + 4 * (iz & 3)] >= 3);
                if (dist <= core * 0.32) {
                    colors[idx] = WHITE;
                } else if (bright && colors[idx] != WHITE) {
                    colors[idx] = INNER;
                } else if (colors[idx] == 0) {
                    colors[idx] = EDGE;
                }
            }
        }
    }
}
