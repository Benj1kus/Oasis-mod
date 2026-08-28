package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Six-frame 8x8 molten-nephritis splash animation.
 * The supplied sprite art already moves lower-left -> upper-right; physics only
 * gives the droplet a small ballistic arc and fade.
 */
public class MeltedSplashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float baseSize;

    protected MeltedSplashParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteSet sprites
    ) {
        super(level, x, y, z);

        this.sprites = sprites;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 11 + this.random.nextInt(4);
        this.baseSize = 0.085F + this.random.nextFloat() * 0.045F;
        this.quadSize = this.baseSize;

        this.gravity = 0.065F;
        this.friction = 0.90F;
        this.hasPhysics = true;

        this.roll = (this.random.nextFloat() - 0.5F) * 0.32F;
        this.oRoll = this.roll;

        this.setSprite(this.sprites.get(0, 5));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.removed) {
            return;
        }

        int frame = Math.min(5, this.age / 2);
        this.setSprite(this.sprites.get(frame, 5));

        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = progress < 0.68F
                ? 1.0F
                : Mth.clamp(1.0F - (progress - 0.68F) / 0.32F, 0.0F, 1.0F);

        this.quadSize = this.baseSize * Mth.lerp(progress, 1.0F, 0.76F);

        this.oRoll = this.roll;
        this.roll += (float) (this.xd - this.zd) * 0.035F;
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

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new MeltedSplashParticle(
                    level,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    this.sprites
            );
        }
    }
}
