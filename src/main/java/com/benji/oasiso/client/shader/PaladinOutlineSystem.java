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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PaladinOutlineSystem {

    private static TextureTarget goldMask;
    private static TextureTarget swordMask;
    private static TextureTarget sceneDepth;

    private static boolean sceneDepthCaptured;
    private static boolean capturedThisFrame;

    private PaladinOutlineSystem() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            beginFrame();
            return;
        }


        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            captureSceneDepth();
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            composite(event.getPartialTick());
        }
    }

    private static void beginFrame() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        ensureTargets(main.width, main.height);

        goldMask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        swordMask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        sceneDepth.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

        goldMask.clear(Minecraft.ON_OSX);
        swordMask.clear(Minecraft.ON_OSX);
        sceneDepth.clear(Minecraft.ON_OSX);

        main.bindWrite(false);

        sceneDepthCaptured = false;
        capturedThisFrame = false;
    }

    private static void captureSceneDepth() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        ensureTargets(main.width, main.height);
        blitDepth(main, sceneDepth);
        main.bindWrite(false);

        sceneDepthCaptured = true;
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
        ensureFromMinecraft();
        RenderTarget source = sceneDepthCaptured ? sceneDepth : Minecraft.getInstance().getMainRenderTarget();

        blitDepth(source, destination);
        destination.bindWrite(false);
    }


    private static void blitDepth(RenderTarget source, RenderTarget destination) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, source.width, source.height, 0, 0, destination.width, destination.height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
    }


    public static void restoreMainTarget() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private static void composite(float partialTick) {
        ShaderInstance shader = PaladinOutlineShaders.getOutlineShader();

        if (!capturedThisFrame || shader == null || goldMask == null || swordMask == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

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

        setVec2(shader, "TexelSize", 1.0F / main.width, 1.0F / main.height);

        float time = (minecraft.level.getGameTime() + partialTick) / 20.0F;

        setFloat(shader, "Time", time);
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

        if (sceneDepth == null) {
            sceneDepth = createTarget(width, height);
        }

        if (goldMask.width != width || goldMask.height != height) {

            goldMask.resize(width, height, Minecraft.ON_OSX);
            goldMask.setFilterMode(GL11.GL_NEAREST);
        }

        if (swordMask.width != width || swordMask.height != height) {

            swordMask.resize(width, height, Minecraft.ON_OSX);
            swordMask.setFilterMode(GL11.GL_NEAREST);
        }

        if (sceneDepth.width != width || sceneDepth.height != height) {

            sceneDepth.resize(width, height, Minecraft.ON_OSX);
            sceneDepth.setFilterMode(GL11.GL_NEAREST);
        }
    }


    private static TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.setFilterMode(GL11.GL_NEAREST);
        return target;
    }

    private static void setFloat(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }


    private static void setVec2(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }
}
