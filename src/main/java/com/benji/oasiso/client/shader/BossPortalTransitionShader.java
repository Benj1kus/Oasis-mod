package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossPortalTransitionShader {

    private static final ResourceLocation PATTERN_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/load_pattern.png");

    private static ShaderInstance shader;

    private BossPortalTransitionShader() {
    }


    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {

        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "boss_portal_transition"), DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
    }


    public static boolean render(float cover, float time) {
        if (shader == null || cover <= 0.0001F) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        float aspect = minecraft.getWindow().getWidth() / (float) minecraft.getWindow().getHeight();
        RenderSystem.setShaderTexture(0, PATTERN_TEXTURE);


        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();


        RenderSystem.setShader(() -> shader);


        shader.setSampler("PatternSampler", RenderSystem.getShaderTexture(0));


        setFloat("Time", time);

        setFloat("Cover", cover);

        setFloat("Aspect", aspect);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);


        buffer.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();


        BufferUploader.drawWithShader(buffer.end());


        shader.clear();


        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        return true;
    }


    private static void setFloat(String name, float value) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }
}