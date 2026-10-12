package com.benji.oasiso.common.factory;

public final class FactoryCoreBlastRules {
    public static final int CHARGE_TICKS = 160;
    public static final int EFFECT_TICKS = 200;
    public static final double DAMAGE_RADIUS = 20;
    public static final double SHOCK_RADIUS = 30;
    public static final double VIEW_RADIUS = 128;

    public static float damage(double distanceSquared) {
        return distanceSquared <= 100 ? 100 : distanceSquared <= 400 ? 50 : 0;
    }

    public static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    public static double shake(double ageSeconds, double distance) {
        if (ageSeconds < 0 || ageSeconds >= 4 || distance > SHOCK_RADIUS) return 0;
        return (1.8 + 3.7 * (1 - clamp(distance / SHOCK_RADIUS))) * Math.pow(1 - ageSeconds / 4, 2);
    }

    private FactoryCoreBlastRules() {
    }
}
