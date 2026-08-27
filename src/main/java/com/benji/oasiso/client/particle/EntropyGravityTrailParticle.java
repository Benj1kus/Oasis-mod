package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class EntropyGravityTrailParticle extends TextureSheetParticle {

    private final float startSize;

    protected EntropyGravityTrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.pickSprite(sprites);
        this.hasPhysics = false;
        this.friction = 1.0F;

        this.lifetime = 26;
        this.startSize = 0.075F + this.random.nextFloat() * 0.025F;
        this.quadSize = this.startSize;

        this.rCol = 0.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.95F;
    }

    @Override
    public void tick() {
        super.tick();

        float life = this.age / (float) this.lifetime;
        this.alpha = 0.95F * (1.0F - life);
        this.quadSize = this.startSize * (1.0F - life * 0.35F);
    }

    @Override
    public int getLightColor(float partialTick) {
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new EntropyGravityTrailParticle(level, x, y, z, this.sprites);
        }
    }
}
