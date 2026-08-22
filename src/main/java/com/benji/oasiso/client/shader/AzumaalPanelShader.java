package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
public final class AzumaalPanelShader {

    private static ShaderInstance shader;

    private AzumaalPanelShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "azumaal_panel"), DefaultVertexFormat.POSITION_TEX), loadedShader -> shader = loadedShader);
    }

    public static boolean render(PoseStack poseStack, float x, float y, float width, float height, float time, float reveal, float alpha) {
        if (shader == null || width <= 0.0F || height <= 0.0F || alpha <= 0.0F) {
            return false;
        }

        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(() -> shader);

        setFloat("Time", time);
        setFloat("Reveal", reveal);
        setFloat("Alpha", alpha);
        setFloat("PanelWidth", width);
        setFloat("PanelHeight", height);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        buffer.vertex(matrix, 0.0F, height, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, width, height, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, width, 0.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).uv(0.0F, 0.0F).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.disableBlend();
        poseStack.popPose();

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