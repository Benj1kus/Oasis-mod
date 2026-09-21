package com.benji.oasiso.client.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.particle.KarakSandDustParticle;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KarakLiquidShaders {
    static ShaderInstance shore, submerged;

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "karak_shore"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> shore = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "karak_submerged"), DefaultVertexFormat.POSITION_TEX), s -> submerged = s);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Oasiso.KR_SAND_DUST.get(), KarakSandDustParticle.Provider::new);
        event.registerSpriteSet(Oasiso.KARAK_BUBBLES.get(), sprites -> new KarakLiquidParticle.Provider(sprites, false));
        event.registerSpriteSet(Oasiso.KARAK_STEAM.get(), sprites -> new KarakLiquidParticle.Provider(sprites, true));
    }

    static void uniform(ShaderInstance shader, String name, float value) {
        var u = shader.getUniform(name);
        if (u != null) u.set(value);
    }

    private KarakLiquidShaders() {
    }
}
