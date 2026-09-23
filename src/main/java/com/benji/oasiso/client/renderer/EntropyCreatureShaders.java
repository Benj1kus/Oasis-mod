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
public final class EntropyCreatureShaders {
    static ShaderInstance mask, distance, outline;

    private EntropyCreatureShaders() {
    }

    static boolean ready() {
        return mask != null && distance != null && outline != null;
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        EntropyCreatureOutlinePass.release();
        mask = distance = outline = null;
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_creature_mask"), DefaultVertexFormat.POSITION_TEX), shader -> mask = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_creature_distance"), DefaultVertexFormat.POSITION_TEX), shader -> distance = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_creature_outline"), DefaultVertexFormat.POSITION_TEX), shader -> outline = shader);
    }
}
