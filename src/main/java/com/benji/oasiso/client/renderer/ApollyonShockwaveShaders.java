package com.benji.oasiso.client.renderer;

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
public final class ApollyonShockwaveShaders {
    static ShaderInstance shader;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        shader = null;
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apollyon_shockwave"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> shader = loaded);
    }

    private ApollyonShockwaveShaders() {
    }
}
