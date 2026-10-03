package com.benji.oasiso.common.entity;

import net.minecraft.world.phys.Vec3;

public final class SpearArtilleryPath {
    public static final int WINDUP_TICKS = 12, FLIGHT_TICKS = 30;

    public static Vec3 recoil(Vec3 origin, Vec3 aim, double progress) {
        Vec3 d = new Vec3(aim.x - origin.x, 0, aim.z - origin.z);
        if (d.lengthSqr() < 1e-8) d = new Vec3(0, 0, 1);
        double t = clamp(progress), ease = t * t * (3 - 2 * t);
        return origin.add(d.normalize().scale(-.85 * ease)).add(0, .18 * ease, 0);
    }

    public static Vec3 control1(Vec3 start, Vec3 end, double ceiling) {
        double rise = 4 + Math.min(3, start.distanceTo(end) * .15);
        return new Vec3(start.x, Math.min(ceiling, start.y + rise), start.z);
    }

    public static Vec3 control2(Vec3 start, Vec3 end, double ceiling) {
        double rise = 4 + Math.min(3, start.distanceTo(end) * .15);
        return new Vec3(end.x, Math.min(ceiling, Math.max(start.y, end.y) + rise), end.z);
    }

    public static Vec3 point(Vec3 start, Vec3 a, Vec3 b, Vec3 end, double progress) {
        double t = clamp(progress);
        t *= t;
        double s = 1 - t;
        return start.scale(s * s * s).add(a.scale(3 * s * s * t)).add(b.scale(3 * s * t * t)).add(end.scale(t * t * t));
    }

    private static double clamp(double t) {
        return Math.max(0, Math.min(1, t));
    }

    private SpearArtilleryPath() {
    }
}
