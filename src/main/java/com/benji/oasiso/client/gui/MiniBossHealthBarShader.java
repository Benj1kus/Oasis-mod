package com.benji.oasiso.client.gui;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MiniBossHealthBarShader {
    private static ShaderInstance shader;

    private MiniBossHealthBarShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "mini_boss_health_bar"), DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
    }

    public static void draw(Matrix4f matrix, ResourceLocation texture, float left, float top, float right, float bottom, float u0, float v0, float u1, float v1, float flash, float alpha) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        ShaderInstance current = shader;
        if (current != null) {
            RenderSystem.setShader(() -> current);
            Uniform uniform = current.getUniform("Flash");
            if (uniform != null) uniform.set(flash);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        b.vertex(matrix, left, top, 0).uv(u0, v0).endVertex();
        b.vertex(matrix, right, top, 0).uv(u1, v0).endVertex();
        b.vertex(matrix, right, bottom, 0).uv(u1, v1).endVertex();
        b.vertex(matrix, left, bottom, 0).uv(u0, v1).endVertex();

        BufferUploader.drawWithShader(b.end());
    }
}
