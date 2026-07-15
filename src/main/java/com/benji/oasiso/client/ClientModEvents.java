package com.benji.oasiso.client;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.DesertBallRenderer;
import com.benji.oasiso.client.renderer.MonkiBigRenderer;
import com.benji.oasiso.client.renderer.MonkiRenderer;

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

    }
}