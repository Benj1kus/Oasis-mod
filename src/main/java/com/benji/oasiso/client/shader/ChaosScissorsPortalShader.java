package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ChaosScissorsPortalShader {

    private static ShaderInstance shader;

    private ChaosScissorsPortalShader() {
    }


    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {

        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "chaos_scissors_portal"),
                        DefaultVertexFormat.POSITION_TEX),
                loaded -> shader = loaded);
    }


    public static boolean render(PoseStack poseStack, float width, float height, float time, float reveal, float despawn, float seed, boolean glowOnly) {

        if (shader == null || width <= 0.0F || height <= 0.0F) {
            return false;
        }

        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.enableBlend();

        if (glowOnly) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        } else {
            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);

        setFloat("Time", time);
        setFloat("Reveal", reveal);
        setFloat("Despawn", despawn);
        setFloat("Seed", seed);
        setFloat("GlowOnly", glowOnly ? 1.0F : 0.0F);

        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        buffer.vertex(matrix, -halfWidth, -halfHeight, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, halfWidth, -halfHeight, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, halfWidth, halfHeight, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, -halfWidth, halfHeight, 0.0F).uv(0.0F, 1.0F).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        shader.clear();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        return true;
    }


    private static void setFloat(String name, float value) {
        if (shader == null) {
            return;
        }
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}