package com.benji.oasiso.client.block.entropy;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.EntropyBlock;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyBlockRender {
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(262144));
    private static TextureTarget mask, horizontal;

    private EntropyBlockRender() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || EntropyBlockVisuals.GROUPS.isEmpty()) return;
        List<EntropyBlockVisuals.Group> visible = new ArrayList<>();
        for (var group : EntropyBlockVisuals.GROUPS)
            if (event.getFrustum().isVisible(group.renderBounds())) visible.add(group);
        if (visible.isEmpty()) return;

        ShaderInstance old = RenderSystem.getShader();
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
            drawModels(mc, event.getPoseStack(), visible, eye, tick, partial, false);
            if (EntropyBlockShaders.horizontal == null || EntropyBlockShaders.outline == null) return;
            ensureTargets(main.width, main.height);
            mask.clear(false);
            mask.copyDepthFrom(main);
            mask.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1, -10);
            drawModels(mc, event.getPoseStack(), visible, eye, tick, partial, true);
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

            main.bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            ShaderInstance outline = EntropyBlockShaders.outline;
            RenderSystem.setShaderTexture(0, horizontal.getColorTextureId());
            outline.safeGetUniform("FieldSize").set((float) horizontal.width, (float) horizontal.height);
            outline.safeGetUniform("Time").set(((tick % 240000) + partial) / 20F);
            outline.safeGetUniform("WidthScale").set(Math.max(.65F, Math.min(1.65F, main.height / 1080F)));
            RenderSystem.setShader(() -> outline);
            quad();
        } finally {
            main.bindWrite(true);
            RenderSystem.polygonOffset(0, 0);
            RenderSystem.disablePolygonOffset();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderTexture(0, oldTexture);
            if (old != null) RenderSystem.setShader(() -> old);
        }
    }

    private static void drawModels(Minecraft mc, PoseStack pose, List<EntropyBlockVisuals.Group> groups, Vec3 eye, long tick, float partial, boolean toMask) {
        for (var group : groups) {
            pose.pushPose();
            group.transform(pose, eye, tick, partial);
            for (BlockPos pos : group.members) {
                if (!mc.level.hasChunkAt(pos)) continue;
                BlockState state = mc.level.getBlockState(pos);
                if (!(state.getBlock() instanceof EntropyBlock)) continue;
                pose.pushPose();
                pose.translate(pos.getX() - group.center.x, pos.getY() - group.center.y, pos.getZ() - group.center.z);
                int light = LightTexture.FULL_BRIGHT;
                mc.getBlockRenderer().renderSingleBlock(state, pose, BUFFERS, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            pose.popPose();
        }
        BUFFERS.endBatch();
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

    static void releaseTargets() {
        if (RenderSystem.isOnRenderThread()) releaseNow();
        else RenderSystem.recordRenderCall(EntropyBlockRender::releaseNow);
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
