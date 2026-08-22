package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class GoldenHeartParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float originalSize;

    protected GoldenHeartParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z);

        this.sprites = sprites;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 12 + this.random.nextInt(5);

        this.originalSize = 0.18F + this.random.nextFloat() * 0.08F;

        this.quadSize = this.originalSize;

        this.gravity = 0.015F;
        this.friction = 0.88F;
        this.hasPhysics = false;

        this.roll = (this.random.nextFloat() - 0.5F) * 0.25F;

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

        if (progress > 0.55F) {
            this.alpha = Mth.clamp(1.0F - (progress - 0.55F) / 0.45F, 0.0F, 1.0F);
        }

        this.oRoll = this.roll;
        this.roll += this.xd * 0.1F;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = ((float) this.age + partialTick) / (float) this.lifetime;

        float scale;


        if (progress < 0.2F) {
            scale = Mth.lerp(progress / 0.2F, 0.35F, 1.25F);
        } else {
            scale = Mth.lerp((progress - 0.2F) / 0.8F, 1.25F, 0.0F);
        }

        return this.originalSize * scale;
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
            return new GoldenHeartParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}