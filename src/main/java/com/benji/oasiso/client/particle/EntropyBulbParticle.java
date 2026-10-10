package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EntropyBulbParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final double rise, driftX, driftZ;
    private final float baseSize;

    private EntropyBulbParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 28 + random.nextInt(37);
        baseSize = (0.065F + random.nextFloat() * 0.11F) * 1.5F;
        quadSize = baseSize;
        double height = .35 + random.nextDouble() * 1.15;
        rise = height / (lifetime * .85);
        driftX = (random.nextDouble() - .5) * .009;
        driftZ = (random.nextDouble() - .5) * .009;
        setSize(0.105F, 0.105F);
        hasPhysics = true;
        alpha = 0;
        setSprite(sprites.get(0, 8));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        float t = age / (float) lifetime;
        move(driftX, rise * (1 - .3 * t), driftZ);
        quadSize = baseSize * (.8F + .3F * t);
        setSprite(sprites.get(Math.min(8, age * 9 / lifetime), 8));
        alpha = .85F * Math.min(1F, age / 4F) * Math.max(0F, Math.min(1F, ((8F / 9F) - t) / .22F));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.ENTROPY_BULB.get(), Provider::new);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new EntropyBulbParticle(level, x, y, z, sprites);
        }
    }
}
