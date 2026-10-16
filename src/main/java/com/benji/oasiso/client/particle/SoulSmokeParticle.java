package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class SoulSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private int startDelay;

    private SoulSmokeParticle(ClientLevel level, double x, double y, double z,
                 double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 28 + random.nextInt(21);
        this.quadSize = 0.5F * switch (random.nextInt(3)) {
            case 0 -> 1.0F / 1.75F;
            case 1 -> 1.0F / 1.5F;
            default -> 1.0F;
        };
        this.friction = 0.96F;
        this.alpha = 1.0F;
        setSprite(sprites.get(0, 2));
    }

    @Override
    public void tick() {
        if (startDelay > 0) { startDelay--; return; }
        super.tick();
        if (!isAlive()) return;
        float t = (float) age / lifetime;
        int frame = Math.min(2, (int) (t * 3));
        setSprite(sprites.get(frame, 2));
        float fade = Math.max(0.0F, Math.min(1.0F, (t - 0.15F) / 0.85F));
        this.alpha = 1.0F - fade * fade * (3.0F - 2.0F * fade);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double dx, double dy, double dz) {
            return new SoulSmokeParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
