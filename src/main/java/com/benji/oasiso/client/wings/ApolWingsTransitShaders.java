package com.benji.oasiso.client.wings;

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
public final class ApolWingsTransitShaders {
    static ShaderInstance shader;

    @SubscribeEvent
    public static void register(RegisterShadersEvent e) throws IOException {
        shader = null;
        e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_wings_transit"), DefaultVertexFormat.POSITION_TEX), s -> shader = s);
    }

    private ApolWingsTransitShaders() {
    }
}
