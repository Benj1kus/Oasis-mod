package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModEntities;
import com.benji.oasiso.client.renderer.HappyKnifeRenderer;
import com.benji.oasiso.client.particle.HappyFlareParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HappyKnifeClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HAPPY_KNIFE.get(), HappyKnifeRenderer::new);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.HAPPY_FLARE.get(), HappyFlareParticle.Provider::new);
    }
}
