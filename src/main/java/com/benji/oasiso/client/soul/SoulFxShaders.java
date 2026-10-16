package com.benji.oasiso.client.soul;

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
public final class SoulFxShaders {
    public static ShaderInstance mask, outline, arc;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        mask = outline = arc = null;
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("soul_mask"), DefaultVertexFormat.POSITION_TEX), s -> mask = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("soul_outline"), DefaultVertexFormat.POSITION_TEX), s -> outline = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), id("soul_arc"), DefaultVertexFormat.POSITION_COLOR_TEX), s -> arc = s);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, name);
    }

    private SoulFxShaders() {
    }
}
