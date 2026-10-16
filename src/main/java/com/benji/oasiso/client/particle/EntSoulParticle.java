package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class EntSoulParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private int startDelay;

    private EntSoulParticle(ClientLevel level, double x, double y, double z,
                 double dx, double dy, double dz, SpriteSet sprites) {
        super(level,
                x + (level.random.nextDouble() - 0.5) * 1.4,
                y,
                z + (level.random.nextDouble() - 0.5) * 1.4);
        this.sprites = sprites;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double drift = 0.008 + random.nextDouble() * 0.014;

        this.xd = dx + Math.cos(angle) * drift;
        this.yd = dy * (0.85 + random.nextDouble() * 0.5);
        this.zd = dz + Math.sin(angle) * drift;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 40 + random.nextInt(31);
        this.startDelay = random.nextInt(21);
        this.quadSize = 0.20F + random.nextFloat() * 0.07F;
        this.friction = 0.995F;
        this.alpha = 0;
        setSprite(sprites.get(0, 8));
    }

    @Override
    public void tick() {
        if (startDelay > 0) { startDelay--; return; }
        super.tick();
        if (!isAlive()) return;
        float t = (float) age / lifetime;
        int frame = Math.min(8, (int) (t * 9));
        setSprite(sprites.get(frame, 8));
        float fadeIn = Math.min(1.0F, age / 5.0F);
        float fadeOut = Math.max(0.0F, Math.min(1.0F, (1.0F - t) / 0.4F));
        this.alpha = fadeIn * fadeOut;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double dx, double dy, double dz) {
            return new EntSoulParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
