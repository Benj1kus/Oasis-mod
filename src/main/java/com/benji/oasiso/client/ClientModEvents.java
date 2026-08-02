package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.*;
import com.benji.oasiso.client.particle.PurpleStarsParticle;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerParticleProviders(
            RegisterParticleProvidersEvent event
    ) {
        event.registerSpriteSet(
                Oasiso.PURPLE_STARS.get(),
                PurpleStarsParticle.Provider::new
        );
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                Oasiso.MONKI.get(),
                MonkiRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.LIE_BLOCK_BE.get(),
                com.benji.oasiso.client.renderer.LieBlockRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.DOUM_PALM_SIGN_BE.get(),
                net.minecraft.client.renderer.blockentity.SignRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.DOUM_PALM_HANGING_SIGN_BE.get(),
                net.minecraft.client.renderer.blockentity.HangingSignRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.SAND_GOLEM.get(),
                SandGolemRenderer::new
        );

        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.WANDERING_TRADER, com.benji.oasiso.client.renderer.DesertWanderingTraderRenderer::new);

        event.registerEntityRenderer(
                Oasiso.DESERT_BALL.get(),
                DesertBallRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DOUM_PALM_BOAT.get(),
                context -> new DoumPalmBoatRenderer(context, false)
        );

        event.registerEntityRenderer(
                Oasiso.DOUM_PALM_CHEST_BOAT.get(),
                context -> new DoumPalmBoatRenderer(context, true)
        );

        event.registerEntityRenderer(
                Oasiso.CACTO_PROJ.get(),
                CactoProjRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.MONKI_BIG.get(),
                MonkiBigRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.TITANA.get(),
                TitanaRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.CASER.get(),
                CaserRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.SAND_HAND.get(),
                SandHandRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DASHER.get(),
                DasherRenderer::new
        );


        event.registerEntityRenderer(
                Oasiso.CACTO.get(),
                CactoRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.STAT_BE.get(),
                StatBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.STATUE_BE.get(),
                StatueRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.SANDED_CHEST_BE.get(),
                SandedChestRenderer::new
        );

    }
}