package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class EntropyFlameParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float baseSize;
    private final float baseAlpha;
    private final float driftX;
    private final float driftZ;

    protected EntropyFlameParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z);

        this.sprites = sprites;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 16 + this.random.nextInt(14);

        if (this.random.nextFloat() < 0.22F) {
            this.baseSize = 0.42F + this.random.nextFloat() * 0.20F;

        } else {
            this.baseSize = 0.13F + this.random.nextFloat() * 0.30F;
        }

        this.quadSize = this.baseSize;

        this.baseAlpha = 0.78F + this.random.nextFloat() * 0.18F;
        this.alpha = this.baseAlpha;

        this.gravity = 0.0F;
        this.friction = 0.93F;
        this.hasPhysics = false;

        this.driftX = (this.random.nextFloat() - 0.5F) * 0.010F;
        this.driftZ = (this.random.nextFloat() - 0.5F) * 0.010F;

        this.roll = 0.0F;
        this.oRoll = 0.0F;

        this.setColor(0.38F, 1.0F, 0.95F);
        updateAnimationSprite();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        updateAnimationSprite();

        float progress = (float) this.age / (float) this.lifetime;

        this.alpha = this.baseAlpha * (1.0F - progress * progress);
        this.quadSize = this.baseSize * (0.92F + progress * 0.32F);

        this.xd += this.driftX;
        this.zd += this.driftZ;
        this.yd += 0.0015D;

        this.move(this.xd, this.yd, this.zd);

        this.xd *= 0.92D;
        this.yd *= 0.94D;
        this.zd *= 0.92D;

        this.oRoll = 0.0F;
        this.roll = 0.0F;
    }

    private void updateAnimationSprite() {
        int frame = (this.age / 2) % 5;
        this.setSprite(this.sprites.get(frame, 4));
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
            return new EntropyFlameParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}