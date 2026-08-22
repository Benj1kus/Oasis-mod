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

public class ChaosBombSparksParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private final float baseSize;

    protected ChaosBombSparksParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z);

        this.sprites = sprites;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;

        this.lifetime = 30 + this.random.nextInt(18);

        this.baseSize = 0.055F + this.random.nextFloat() * 0.075F;

        this.quadSize = this.baseSize;

        this.gravity = 0.72F;

        this.friction = 0.98F;

        this.hasPhysics = true;

        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);

        this.oRoll = this.roll;

        this.setSprite(this.sprites.get(0, 3));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.removed) {
            return;
        }
        int frame = Math.min(3, this.age / 2);

        this.setSprite(this.sprites.get(frame, 3));

        if (this.onGround) {

            this.yd = 0.0D;
            this.xd *= 0.68D;
            this.zd *= 0.68D;
        }

        float progress = (float) this.age / (float) this.lifetime;
        float fade = Mth.clamp((1.0F - progress) / 0.28F, 0.0F, 1.0F);

        this.alpha = fade;
        this.oRoll = this.roll;
        this.roll += 0.04F;
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
            return new ChaosBombSparksParticle(level, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}