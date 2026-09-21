package com.benji.oasiso.client.fluid;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class KarakLiquidParticle extends TextureSheetParticle {
    private static ClientLevel budgetLevel;
    private static final java.util.List<Long> bubbleLeases = new java.util.ArrayList<>();
    private static final java.util.List<Long> steamLeases = new java.util.ArrayList<>();
    private final SpriteSet sprites;
    private final boolean steam, surface;
    private final double startY, rise;
    private final int liveTicks;
    private int popAge = -1;


    private KarakLiquidParticle(ClientLevel level, double x, double y, double z, double rise, SpriteSet sprites, boolean steam) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.steam = steam;
        this.surface = rise > 0;
        this.rise = rise;
        this.startY = y;
        this.liveTicks = steam ? 21 + random.nextInt(8) : 22 + random.nextInt(23);
        this.lifetime = liveTicks + (steam ? 0 : 9);
        this.quadSize = steam ? 0.30F + random.nextFloat() * 0.30F : 0.045F + random.nextFloat() * 0.065F;
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0;
        if (steam) {
            setPos(x, y + quadSize, z);
            this.yo = this.y;
        }
        setSprite(sprites.get(0, 6));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (steam) {
            if (++age >= lifetime) {
                remove();
                return;
            }
            setSprite(sprites.get(Math.min(6, age * 7 / lifetime), 6));
            alpha = Math.min(1.0F, (lifetime - age) / 4.0F);
            return;
        }
        age++;
        if (popAge < 0) {
            if (surface) {
                double t = Math.min(1, age / (double) liveTicks);
                y = startY + rise * (1 - (1 - t) * (1 - t));
            } else {
                y += 0.004;
            }
            if (age >= liveTicks || (!surface && !KarakLiquidVfx.contains(level, x, y, z))) popAge = 0;
        } else popAge++;
        setPos(x, y, z);
        if (popAge >= 9) {
            remove();
            return;
        }
        int frame = popAge >= 0 ? 4 + popAge / 3 : Math.min(3, age * 4 / liveTicks);
        setSprite(sprites.get(frame, 6));
        alpha = popAge < 0 ? Math.min(1, age / 3.0F) : Mth.clamp((9 - popAge) / 3.0F, 0, 1);
    }

    static void resetBudget(ClientLevel level) {
        budgetLevel = level;
        bubbleLeases.clear();
        steamLeases.clear();
    }

    @Override
    protected int getLightColor(float partial) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean steam;

        public Provider(SpriteSet sprites, boolean steam) {
            this.sprites = sprites;
            this.steam = steam;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            if (budgetLevel != level) resetBudget(level);
            long now = level.getGameTime();
            var leases = steam ? steamLeases : bubbleLeases;
            leases.removeIf(expiry -> expiry <= now);
            if (leases.size() >= (steam ? 16 : 128)) return null;
            KarakLiquidParticle particle = new KarakLiquidParticle(level, x, y, z, Math.max(0, Math.min(0.8, vy)), sprites, steam);
            leases.add(now + particle.lifetime + 2);
            return particle;
        }
    }
}
