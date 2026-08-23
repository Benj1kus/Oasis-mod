package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossPortalDistortionShader {

    private static ShaderInstance shader;
    private static TextureTarget sceneCopy;

    private BossPortalDistortionShader() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "boss_portal_distortion"), DefaultVertexFormat.POSITION_TEX), loadedShader -> shader = loadedShader);
    }


    public static boolean render(float baseX, float baseY, float topX, float topY, float baseRadius, float topRadius, float baseDepth, float topDepth, float aspect, float time, float strength) {

        if (shader == null || strength <= 0.00001F) return false;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();

        if (main.width <= 0 || main.height <= 0) return false;

        ensureTarget(main.width, main.height);
        copyMainToScene(main);

        main.bindWrite(true);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);

        shader.setSampler("DiffuseSampler", sceneCopy.getColorTextureId());
        shader.setSampler("DepthSampler", sceneCopy.getDepthTextureId());

        setFloat("Time", time);
        setVec2("PortalBase", baseX, baseY);
        setVec2("PortalTop", topX, topY);
        setFloat("BaseRadius", baseRadius);
        setFloat("TopRadius", topRadius);
        setFloat("BaseDepth", baseDepth);
        setFloat("TopDepth", topDepth);
        setFloat("Aspect", aspect);
        setFloat("Strength", strength);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // screen bottom-left
        buffer.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        // bottom-right
        buffer.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        // top-right
        buffer.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        // top-left
        buffer.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();

        BufferUploader.drawWithShader(buffer.end());
        shader.clear();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        return true;
    }


    private static void ensureTarget(int width, int height) {

        if (sceneCopy == null) {
            sceneCopy = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            sceneCopy.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            return;
        }
        if (sceneCopy.width != width || sceneCopy.height != height) {
            sceneCopy.resize(width, height, Minecraft.ON_OSX);
        }
    }


    private static void copyMainToScene(RenderTarget main) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, sceneCopy.width, sceneCopy.height, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
    }


    private static void setFloat(String name, float value) {
        if (shader == null) return;

        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }


    private static void setVec2(String name, float x, float y) {
        if (shader == null) return;

        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y);
        }
    }
}