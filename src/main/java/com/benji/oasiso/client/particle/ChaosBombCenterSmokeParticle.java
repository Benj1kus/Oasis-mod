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

public class ChaosBombCenterSmokeParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private final float baseSize;

    private final float waveOffset;

    protected ChaosBombCenterSmokeParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z);

        this.sprites = sprites;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 30 + this.random.nextInt(15);

        this.baseSize = 0.45F + this.random.nextFloat() * 0.7F;

        this.quadSize = this.baseSize;

        this.gravity = 0.0F;

        this.friction = 0.95F;

        this.hasPhysics = false;

        this.waveOffset = this.random.nextFloat() * ((float) Math.PI * 2.0F);

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

        this.quadSize = this.baseSize * (1.0F + progress * 0.8F);

        float fade = Mth.clamp((1.0F - progress) / 0.55F, 0.0F, 1.0F);

        this.alpha = 0.88F * fade;

        double wave = this.age * 0.38D + this.waveOffset;

        this.xd += Math.sin(wave) * 0.0018D;
        this.zd += Math.cos(wave * 0.9D) * 0.0018D;
        this.yd += 0.0015D;

        this.oRoll = this.roll;
        this.roll += 0.006F;
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
            return new ChaosBombCenterSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}