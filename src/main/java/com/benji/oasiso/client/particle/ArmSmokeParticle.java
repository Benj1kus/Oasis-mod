package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class ArmSmokeParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float baseSize;
    private final float baseAlpha;
    private final float spinSpeed;

    protected ArmSmokeParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        this(level, x, y, z, velocityX, velocityY, velocityZ, sprites, 1.0F);
    }

    protected ArmSmokeParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites, float sizeScale) {
        super(level, x, y, z);

        this.sprites = sprites;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 22 + this.random.nextInt(15);

        float normalizedScale = Math.max(0.05F, sizeScale);
        this.baseSize = (0.18F + this.random.nextFloat() * 0.24F) * normalizedScale;

        this.quadSize = this.baseSize;

        this.baseAlpha = 0.78F + this.random.nextFloat() * 0.18F;

        this.alpha = this.baseAlpha;

        this.gravity = 0.0F;
        this.friction = 0.94F;
        this.hasPhysics = false;

        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);

        this.oRoll = this.roll;

        float spinMagnitude = 0.075F + this.random.nextFloat() * 0.105F;

        this.spinSpeed = this.random.nextBoolean() ? spinMagnitude : -spinMagnitude;
        this.setColor(0.72F, 1.0F, 0.96F);

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
        float fadeStart = 0.55F;

        if (progress <= fadeStart) {
            this.alpha = this.baseAlpha;
        } else {
            float fadeProgress = (progress - fadeStart) / (1.0F - fadeStart);

            float smoothFade = 1.0F - fadeProgress * fadeProgress * (3.0F - 2.0F * fadeProgress);

            this.alpha = this.baseAlpha * smoothFade;
        }

        this.quadSize = this.baseSize * (0.90F + progress * 0.35F);

        this.oRoll = this.roll;
        this.roll += this.spinSpeed * (1.0F - progress * 0.25F);
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
            return new ArmSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}
