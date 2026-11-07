package com.benji.oasiso.client.soulcast;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;

final class SoulBlockChip extends TerrainParticle {
    private final double floor, height;
    private final int rise;
    private static final int HOVER = 60;

    SoulBlockChip(ClientLevel level, double x, double z, SoulStrikeGround.Surface top, double height, int rise, int elapsed) {
        super(level, x, top.y() + .045, z, 0, 0, 0, top.state(), top.pos());
        updateSprite(top.state(), top.pos());
        floor = top.y() + .045;
        this.height = height;
        this.rise = rise;
        hasPhysics = false;
        lifetime = rise + HOVER + (int) Math.ceil(Math.sqrt(height / .045));
        age = elapsed;
        setPos(x, floor + offset(age), z);
        xo = x;
        yo = y;
        zo = z;
        if (age >= lifetime) remove();
    }

    private double offset(int t) {
        if (t < rise) {
            double u = (double) t / rise;
            return height * u * u * (3 - 2 * u);
        }
        double drop = Math.max(0, t - rise - HOVER);
        return Math.max(0, height - .045 * drop * drop);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        setPos(x, floor + offset(age), z);
    }
}
