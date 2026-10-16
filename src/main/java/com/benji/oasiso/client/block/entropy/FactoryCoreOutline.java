package com.benji.oasiso.client.block.entropy;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.renderer.LateOutlineQueue;
import com.benji.oasiso.client.factory.FactoryCoreVisuals;
import org.joml.Matrix4f;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactoryCoreOutline {
    private static TextureTarget mask, horizontal;

    private FactoryCoreOutline() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || FactoryCoreVisuals.cores().isEmpty()) return;
        List<FactoryCoreVisuals.Core> visible = new ArrayList<>();
        for (var group : FactoryCoreVisuals.cores())
            if (event.getFrustum().isVisible(new net.minecraft.world.phys.AABB(group.center(), group.center()).inflate(1.2)))
                visible.add(group);
        if (visible.isEmpty()) return;

        ShaderInstance old = RenderSystem.getShader();
        boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND), oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean oldMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int oldFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int srcRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);

        int oldTexture = RenderSystem.getShaderTexture(0);
        float[] tint = RenderSystem.getShaderColor().clone();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderTarget main = mc.getMainRenderTarget();
        Vec3 eye = event.getCamera().getPosition();
        long tick = mc.level.getGameTime();
        float partial = event.getPartialTick();
        try {
            main.bindWrite(true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);

            if (EntropyBlockShaders.horizontal == null || EntropyBlockShaders.outline == null) return;
            ensureTargets(main.width, main.height);
            mask.clear(false);
            mask.copyDepthFrom(main);
            mask.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1, -10);
            drawMask(event.getPoseStack(), visible, eye);
            RenderSystem.polygonOffset(0, 0);
            RenderSystem.disablePolygonOffset();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            horizontal.bindWrite(true);
            ShaderInstance h = EntropyBlockShaders.horizontal;
            RenderSystem.setShaderTexture(0, mask.getColorTextureId());
            h.safeGetUniform("FieldSize").set((float) horizontal.width, (float) horizontal.height);
            RenderSystem.setShader(() -> h);
            quad();

            // The mask and horizontal field belong to this frame. Only the final
            // screen-space overlay must wait until clouds/transparency are finished.
            TextureTarget field = horizontal;
            LateOutlineQueue.submit(() -> composite(main, field, tick, partial));
        } finally {
            main.bindWrite(true);
            RenderSystem.polygonOffset(0, 0);
            RenderSystem.disablePolygonOffset();
            RenderSystem.depthMask(oldMask);
            RenderSystem.depthFunc(oldFunc);
            if (oldDepth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (oldCull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcA, dstA);
            if (oldBlend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderTexture(0, oldTexture);
            if (old != null) RenderSystem.setShader(() -> old);
        }
    }

    private static final int[][] FACES = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
    private static final BufferBuilder MASK_BUILDER = new BufferBuilder(4096);

    private static void drawMask(PoseStack pose, List<FactoryCoreVisuals.Core> cores, Vec3 eye) {
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        MASK_BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (var core : cores) {
            var p = core.pos();
            Matrix4f transform = new Matrix4f(pose.last().pose()).translate((float) (p.getX() - eye.x), (float) (p.getY() - eye.y), (float) (p.getZ() - eye.z)).mul(core.matrix());
            for (int[] face : FACES)
                for (int corner : face) {
                    float x = (corner & 1) == 0 ? -.375F : .375F;
                    float y = (corner & 2) == 0 ? -.375F : .375F;
                    float z = (corner & 4) == 0 ? -.375F : .375F;
                    MASK_BUILDER.vertex(transform, x, y, z).color(255, 255, 255, 255).endVertex();
                }
        }
        BufferUploader.drawWithShader(MASK_BUILDER.end());
    }

    private static void ensureTargets(int w, int h) {
        if (mask != null && mask.width == w && mask.height == h) return;
        releaseNow();
        mask = new TextureTarget(w, h, true, false);
        mask.setClearColor(0, 0, 0, 0);
        mask.setFilterMode(GL11.GL_NEAREST);
        horizontal = new TextureTarget(Math.max(1, (w + 1) / 2), Math.max(1, (h + 1) / 2), false, false);
        horizontal.setFilterMode(GL11.GL_NEAREST);
    }

    @SubscribeEvent
    public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) releaseTargets();
    }

    static void releaseTargets() {
        if (RenderSystem.isOnRenderThread()) releaseNow();
        else RenderSystem.recordRenderCall(FactoryCoreOutline::releaseNow);
    }

    private static void releaseNow() {
        if (mask != null) {
            mask.destroyBuffers();
            mask = null;
        }
        if (horizontal != null) {
            horizontal.destroyBuffers();
            horizontal = null;
        }
    }

    private static void composite(RenderTarget main, TextureTarget field, long tick, float partial) {
        if (field != horizontal || EntropyBlockShaders.outline == null) return;
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        try {
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest(); // Visibility was already tested when drawing the mask.
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            main.bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ShaderInstance outline = EntropyBlockShaders.outline;
            RenderSystem.setShaderTexture(0, field.getColorTextureId());
            outline.safeGetUniform("FieldSize").set((float) field.width, (float) field.height);
            outline.safeGetUniform("Time").set(((tick % 240000) + partial) / 20F);
            outline.safeGetUniform("WidthScale").set(Math.max(.65F, Math.min(1.65F, main.height / 1080F)));
            RenderSystem.setShader(() -> outline);
            quad();
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void quad() {
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(-1, -1, 0).uv(0, 0).endVertex();
        b.vertex(1, -1, 0).uv(1, 0).endVertex();
        b.vertex(1, 1, 0).uv(1, 1).endVertex();
        b.vertex(-1, 1, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
