package com.benji.oasiso.client.block.entropy;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.particle.EntropyLightningParticle;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EntropyBlockShaders {
    static ShaderInstance horizontal, outline;

    private EntropyBlockShaders() {
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_block_distance"), DefaultVertexFormat.POSITION_TEX), s -> horizontal = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_block_outline"), DefaultVertexFormat.POSITION_TEX), s -> outline = s);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.ENTROPY_LIGHTNING.get(), EntropyLightningParticle.Provider::new);
    }
}
