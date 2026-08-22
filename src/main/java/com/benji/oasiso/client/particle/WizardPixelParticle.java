package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class WizardPixelParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected WizardPixelParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);

        this.sprites = sprites;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = 8 + this.random.nextInt(7);
        this.quadSize = 0.08F + this.random.nextFloat() * 0.06F;

        this.gravity = 0.0F;
        this.friction = 0.92F;
        this.hasPhysics = false;

        this.setColor(0.35F + this.random.nextFloat() * 0.08F, 1.0F, 0.88F + this.random.nextFloat() * 0.1F);

        this.alpha = 1.0F;
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

        this.alpha = 1.0F - progress;
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new WizardPixelParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}