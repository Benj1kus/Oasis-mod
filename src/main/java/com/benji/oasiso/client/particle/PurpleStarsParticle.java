package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class PurpleStarsParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected PurpleStarsParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z);

        this.sprites = sprites;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 12 + this.random.nextInt(7);
        this.quadSize = 0.12F + this.random.nextFloat() * 0.08F;

        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = false;

        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);

        this.oRoll = this.roll;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.removed) {
            return;
        }

        this.setSpriteFromAge(this.sprites);

        float progress = (float) this.age / (float) this.lifetime;

        this.alpha = 1.0F - progress * 0.55F;

        this.oRoll = this.roll;
        this.roll += 0.035F;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new PurpleStarsParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}