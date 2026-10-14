package com.benji.oasiso.common.combat;

import net.minecraft.world.phys.Vec3;

public final class ScienceShieldGeometry {
    public static Vec3 radii(double width, double height) {
        double horizontal = 2.0 * Math.max(1.05, width * .5 + .65);
        return new Vec3(horizontal, height * .5 + .3, horizontal);
    }

    public static Vec3 unit(Vec3 p, Vec3 center, Vec3 r) {
        Vec3 d = p.subtract(center);
        return new Vec3(d.x / r.x, d.y / r.y, d.z / r.z);
    }

    public static Vec3 surface(Vec3 unit, Vec3 center, Vec3 r) {
        Vec3 n = unit.normalize();
        return center.add(n.x * r.x, n.y * r.y, n.z * r.z);
    }

    public static Vec3 intersect(Vec3 from, Vec3 to, Vec3 center, Vec3 r) {
        Vec3 a = unit(from, center, r), b = unit(to, center, r).subtract(a);
        double aa = b.lengthSqr(), bb = 2 * a.dot(b), cc = a.lengthSqr() - 1;
        if (aa < 1.0E-10 || cc < 0) return null;
        double discriminant = bb * bb - 4 * aa * cc;
        if (discriminant < 0) return null;
        double t = (-bb - Math.sqrt(discriminant)) / (2 * aa);
        return t >= 0 && t <= 1 ? from.add(to.subtract(from).scale(t)) : null;
    }

    private ScienceShieldGeometry() {
    }
}
