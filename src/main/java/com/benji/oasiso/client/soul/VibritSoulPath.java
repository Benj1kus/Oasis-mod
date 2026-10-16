package com.benji.oasiso.client.soul;

import net.minecraft.world.phys.Vec3;


public final class VibritSoulPath {
    public static final int LIFE_TICKS = 64;
    public static final double RISE_END = .15;
    public static final double CIRCLE_END = .55;
    public static final double CIRCLE_RADIUS = 2.2;
    public static final double RISE_HEIGHT = 3.3;

    private VibritSoulPath() {
    }

    public static Vec3 sample(Vec3 from, Vec3 to, int seed, double progress) {
        double t = Math.max(0, Math.min(1, progress));
        if (t <= RISE_END) {
            return from.add(0, RISE_HEIGHT * smooth(t / RISE_END), 0);
        }
        Vec3 top = from.add(0, RISE_HEIGHT, 0);
        if (t <= CIRCLE_END) {
            double u = smooth((t - RISE_END) / (CIRCLE_END - RISE_END));
            double phase = (seed & 255) / 256.0 * Math.PI * 2;
            double angle = phase + ((seed & 256) == 0 ? 1 : -1) * Math.PI * 2 * u;
            return top.add(CIRCLE_RADIUS * (Math.cos(angle) - Math.cos(phase)), CIRCLE_RADIUS * (Math.sin(angle) - Math.sin(phase)), 0);
        }
        double u = smooth((t - CIRCLE_END) / (1 - CIRCLE_END)), v = 1 - u;
        Vec3 direction = to.subtract(top);
        Vec3 p1 = top.add(direction.scale(.27)).add(0, .4, 0);
        Vec3 p2 = to.subtract(direction.scale(.18)).add(0, .35, 0);
        return top.scale(v * v * v).add(p1.scale(3 * v * v * u)).add(p2.scale(3 * v * u * u)).add(to.scale(u * u * u));
    }

    private static double smooth(double x) {
        return x * x * (3 - 2 * x);
    }
}
