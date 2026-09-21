package com.benji.oasiso.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class KarakSandDustParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float initialSize;
    private int groundedTicks;

    private KarakSandDustParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 50 + random.nextInt(26);
        initialSize = quadSize = .04F + random.nextFloat() * .05F;
        gravity = .07F;
        friction = .98F;
        hasPhysics = true;
        xd = (random.nextDouble() - .5) * .012;
        yd = -.025 - random.nextDouble() * .02;
        zd = (random.nextDouble() - .5) * .012;
        setSprite(sprites.get(0, 5));
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSprite(sprites.get(Math.min(5, age * 6 / lifetime), 5));
        float t = Mth.clamp((age / (float) lifetime - .35F) / .65F, 0, 1);
        alpha = 1 - t * t * (3 - 2 * t);
        if (onGround) {
            if (++groundedTicks >= 8) {
                remove();
                return;
            }
            alpha *= 1 - groundedTicks / 8.0F;
        }
        quadSize = initialSize * (1 - .20F * t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private static ClientLevel lastLevel;
        private static final java.util.List<Long> leases = new java.util.ArrayList<>();
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        public static void reset(ClientLevel level) {
            lastLevel = level;
            leases.clear();
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            if (lastLevel != level) {
                lastLevel = level;
                leases.clear();
            }
            long now = level.getGameTime();
            leases.removeIf(end -> end <= now);
            if (leases.size() >= 100) return null;
            KarakSandDustParticle particle = new KarakSandDustParticle(level, x, y, z, sprites);
            leases.add(now + particle.lifetime + 2);
            return particle;
        }
    }
}
