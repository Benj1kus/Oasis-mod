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
public final class EmotionOutlineShaders {
    static ShaderInstance mask, distance, outline;

    static boolean ready() {
        return mask != null && distance != null && outline != null;
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        EmotionOutlinePass.release();
        mask = distance = outline = null;
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("emotion_mask"), DefaultVertexFormat.POSITION_TEX), s -> mask = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("emotion_distance"), DefaultVertexFormat.POSITION_TEX), s -> distance = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("emotion_outline"), DefaultVertexFormat.POSITION_TEX), s -> outline = s);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, name);
    }
}
