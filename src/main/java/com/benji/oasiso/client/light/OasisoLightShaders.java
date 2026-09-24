package com.benji.oasiso.client.light;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OasisoLightShaders {
    static ShaderInstance light;

    private OasisoLightShaders() {
    }

    static boolean ready() {
        return light != null;
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        OasisoLightPass.release();
        OasisoLocalLights.resetFailure();
        light = null;
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "oasiso_local_light"),
                DefaultVertexFormat.POSITION_TEX), shader -> light = shader);
    }
}
