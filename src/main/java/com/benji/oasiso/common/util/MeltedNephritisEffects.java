package com.benji.oasiso.common.util;

import com.benji.oasiso.Oasiso;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class MeltedNephritisEffects {

    private MeltedNephritisEffects() {
    }
    public static void spawnBurst(ServerLevel level, Vec3 center) {
        int count = 14 + level.random.nextInt(7);

        for (int i = 0; i < count; i++) {
            double vx = (level.random.nextDouble() - 0.5D) * 0.24D;
            double vy = 0.055D + level.random.nextDouble() * 0.16D;
            double vz = (level.random.nextDouble() - 0.5D) * 0.24D;
            level.sendParticles(Oasiso.MELTED_SPLASH.get(), center.x + (level.random.nextDouble() - 0.5D) * 0.45D, center.y + (level.random.nextDouble() - 0.5D) * 0.35D, center.z + (level.random.nextDouble() - 0.5D) * 0.45D, 0, vx, vy, vz, 1.0D);
        }
    }

    public static void spawnIdle(ServerLevel level, Vec3 center) {
        double vx = (level.random.nextDouble() - 0.5D) * 0.035D;
        double vy = 0.025D + level.random.nextDouble() * 0.045D;
        double vz = (level.random.nextDouble() - 0.5D) * 0.035D;

        level.sendParticles(Oasiso.MELTED_SPLASH.get(), center.x, center.y, center.z, 0, vx, vy, vz, 1.0D);
    }
}
