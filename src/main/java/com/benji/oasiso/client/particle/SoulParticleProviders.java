package com.benji.oasiso.client.particle;

import com.benji.oasiso.Oasiso;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SoulParticleProviders {
    private SoulParticleProviders() {}

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.ENT_SOUL.get(), EntSoulParticle.Provider::new);
        event.registerSpriteSet(Oasiso.SOUL_SMOKE.get(), SoulSmokeParticle.Provider::new);
    }
}
