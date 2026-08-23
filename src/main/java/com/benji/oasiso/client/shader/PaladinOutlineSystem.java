package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class PaladinOutlineSystem {

    private static ShaderInstance shader;
    private static TextureTarget goldMask;
    private static TextureTarget swordMask;

    private static boolean capturedThisFrame;

    private PaladinOutlineSystem() {
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderRegistration {
        private ShaderRegistration() {
        }

        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "paladin_outline"), DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            beginFrame();
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            composite(event.getPartialTick());
        }
    }


    private static void beginFrame() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) return;

        RenderTarget main = minecraft.getMainRenderTarget();
        ensureTargets(main.width, main.height);

        goldMask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        swordMask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        goldMask.clear(Minecraft.ON_OSX);
        swordMask.clear(Minecraft.ON_OSX);

        main.bindWrite(false);
        capturedThisFrame = false;
    }

    public static RenderTarget goldMask() {
        ensureFromMinecraft();
        return goldMask;
    }

    public static RenderTarget swordMask() {
        ensureFromMinecraft();
        return swordMask;
    }

    public static void markCaptured() {
        capturedThisFrame = true;
    }

    public static void copySceneDepth(RenderTarget destination) {

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, destination.width, destination.height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

        destination.bindWrite(false);
    }

    public static void restoreMainTarget() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private static void composite(float partialTick) {
        if (!capturedThisFrame || shader == null || goldMask == null || swordMask == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();

        main.bindWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);

        shader.setSampler("GoldMask", goldMask.getColorTextureId());
        shader.setSampler("SwordMask", swordMask.getColorTextureId());

        setVec2("TexelSize", 1.0F / main.width, 1.0F / main.height);

        float time = (minecraft.level.getGameTime() + partialTick) / 20.0F;

        setFloat("Time", time);

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
    }

    private static void ensureFromMinecraft() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        ensureTargets(main.width, main.height);
    }

    private static void ensureTargets(int width, int height) {

        if (goldMask == null) {
            goldMask = createTarget(width, height);
        }

        if (swordMask == null) {
            swordMask = createTarget(width, height);
        }

        if (goldMask.width != width || goldMask.height != height) {
            goldMask.resize(width, height, Minecraft.ON_OSX);
        }

        if (swordMask.width != width || swordMask.height != height) {
            swordMask.resize(width, height, Minecraft.ON_OSX);
        }
    }


    private static TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        return target;
    }

    private static void setFloat(String name, float value) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setVec2(String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y);
        }
    }
}