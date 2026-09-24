package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EntropyEyeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private EntropyEyeParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 33; // 11 кадров, каждый держится 3 тика.
        quadSize = .18F + random.nextFloat() * .10F;
        gravity = 0; hasPhysics = false;
        xd = vx * .25; yd = .008 + Math.abs(vy) * .25; zd = vz * .25;
        setSprite(sprites.get(0, 10));
        alpha = 0;
    }
    @Override public void tick() {
        super.tick();
        if (removed) return;
        setSprite(sprites.get(Math.min(10, age / 3), 10));
        alpha = Math.min(1F, age / 4F) * Math.min(1F, (lifetime - age) / 7F);
    }
    @Override protected int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new EntropyEyeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.ENTROPY_EYE.get(), Provider::new);
    }
}
