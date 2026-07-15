package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.*;

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
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                Oasiso.MONKI.get(),
                MonkiRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DESERT_BALL.get(),
                DesertBallRenderer::new
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
                Oasiso.SAND_HAND.get(),
                SandHandRenderer::new
        );

        event.registerEntityRenderer(
                Oasiso.DASHER.get(),
                DasherRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.STAT_BE.get(),
                StatBlockEntityRenderer::new
        );

        event.registerBlockEntityRenderer(
                Oasiso.SANDED_CHEST_BE.get(),
                SandedChestRenderer::new
        );

    }
}