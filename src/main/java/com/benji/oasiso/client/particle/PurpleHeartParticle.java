package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


public final class PurpleHeartParticle extends GoldenHeartParticle {
    private PurpleHeartParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites);
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new PurpleHeartParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(Oasiso.PURPLE_HEART.get(), Provider::new);
        }
    }
}
