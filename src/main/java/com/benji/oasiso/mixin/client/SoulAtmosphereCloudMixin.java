package com.benji.oasiso.mixin.client;

import com.benji.oasiso.client.soulstorage.SoulAtmosphere;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class SoulAtmosphereCloudMixin {
    @Redirect(method = "renderClouds", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"))
    private void soulClouds(VertexBuffer buffer, Matrix4f view, Matrix4f projection, ShaderInstance shader) {
        float a = SoulAtmosphere.strength();
        if (a >= 0.999F) return;
        float[] c = RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.setShaderColor(c[0], c[1], c[2], c[3] * (1 - a));
            buffer.drawWithShader(view, projection, shader);
        } finally {
            RenderSystem.setShaderColor(c[0], c[1], c[2], c[3]);
        }
    }
}
