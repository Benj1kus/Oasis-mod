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

@Mod.EventBusSubscriber(
        modid = Oasiso.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class EntropyPhysicsOutlineShaders {

    private static ShaderInstance compositeShader;

    private EntropyPhysicsOutlineShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(
            RegisterShadersEvent event
    ) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        new ResourceLocation(
                                Oasiso.MODID,
                                "entropy_physics_outline"
                        ),
                        DefaultVertexFormat.POSITION_TEX
                ),
                shader ->
                        compositeShader = shader
        );
    }

    public static ShaderInstance getCompositeShader() {
        return compositeShader;
    }
}
