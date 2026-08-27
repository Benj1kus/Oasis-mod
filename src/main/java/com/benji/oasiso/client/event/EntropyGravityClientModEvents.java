package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.particle.EntropyGravityTrailParticle;
import com.benji.oasiso.client.renderer.EntropyPhysicsBlockRenderer;
import com.benji.oasiso.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EntropyGravityClientModEvents {

    private EntropyGravityClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ENTROPY_PHYSICS_BLOCK.get(), EntropyPhysicsBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.ENTROPY_GRAVITY_TRAIL.get(), EntropyGravityTrailParticle.Provider::new);
    }
}
