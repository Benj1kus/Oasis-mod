package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class HappyFlareParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float turn;
    private HappyFlareParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites; xd = vx; yd = vy + .015; zd = vz;
        lifetime = 22 + random.nextInt(9); quadSize = .16F + random.nextFloat() * .12F;
        friction = .92F; gravity = -.005F; hasPhysics = false;
        turn = (random.nextBoolean() ? 1 : -1) * (.018F + random.nextFloat() * .012F);
        roll = (random.nextFloat() - .5F) * .3F; oRoll = roll; setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        super.tick(); if (removed) return;
        oRoll = roll; roll += turn;
        setSpriteFromAge(sprites);
        alpha = Mth.clamp((1 - age / (float)lifetime) / .4F, 0, 1);
    }
    @Override protected int getLightColor(float partial) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new HappyFlareParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
