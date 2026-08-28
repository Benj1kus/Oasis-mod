package com.benji.oasiso.client.shader;

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
public final class OasisoTooltipShaders {

    private static ShaderInstance backgroundShader;
    private static ShaderInstance glowShader;

    private OasisoTooltipShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "tooltip_background"), DefaultVertexFormat.POSITION_TEX), shader -> backgroundShader = shader);

        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "tooltip_glow"), DefaultVertexFormat.POSITION_TEX), shader -> glowShader = shader);
    }

    public static ShaderInstance getBackgroundShader() {
        return backgroundShader;
    }

    public static ShaderInstance getGlowShader() {
        return glowShader;
    }
}
