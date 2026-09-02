package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyGloveFillRenderer {

    private static final Map<BlockPos, BlockAnimation> ANIMATIONS = new HashMap<>();
    private static final MultiBufferSource.BufferSource ANIMATION_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(2 * 1024 * 1024));
    private static final float CELL_INSET = 0.055F;

    private EntropyGloveFillRenderer() {
    }


    public static void addAnimation(BlockPos pos, BlockState state, int duration) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        ANIMATIONS.put(pos.immutable(), new BlockAnimation(state, minecraft.level.getGameTime(), Math.max(2, duration)));
    }


    public static boolean hasVisibleSelection() {
        if (!EntropyGloveFillClientState.isFillMode() || !EntropyGloveFillClientState.hasSelection()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return false;
        }

        BlockPos first = EntropyGloveFillClientState.first();
        BlockPos end = EntropyGloveFillClientState.visibleEnd();

        if (first == null || end == null) {
            return false;
        }


        for (BlockPos pos : BlockPos.betweenClosed(first, end)) {
            if (shouldRenderSelectionCell(minecraft, pos)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            ANIMATIONS.clear();
            return;
        }

        renderSelectionPulse(event);
        renderDragHandle(event);
        renderBlockAnimations(event);
    }

    private static boolean shouldRenderSelectionCell(Minecraft minecraft, BlockPos pos) {

        if (ANIMATIONS.containsKey(pos)) {
            return false;
        }

        if (minecraft.level == null) {
            return false;
        }

        BlockState current = minecraft.level.getBlockState(pos);

        if (!current.canBeReplaced()) {
            return false;
        }

        return minecraft.level.getFluidState(pos).isEmpty();
    }

    private static void renderSelectionPulse(RenderLevelStageEvent event) {
        if (!hasVisibleSelection()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        float time = (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0F;

        float pulse = 0.5F + 0.5F * Mth.sin(time * 2.15F);
        float alpha = 0.040F + pulse * 0.060F;

        renderSelectionCells(event, 0.04F, 0.92F, 0.82F, alpha);
    }

    public static void renderSelectionMask(RenderLevelStageEvent event) {
        if (!hasVisibleSelection()) {
            return;
        }
        renderSelectionCells(event, 1.0F, 1.0F, 1.0F, 1.0F);
    }


    private static void renderSelectionCells(RenderLevelStageEvent event, float red, float green, float blue, float alpha) {
        BlockPos first = EntropyGloveFillClientState.first();
        BlockPos end = EntropyGloveFillClientState.visibleEnd();

        if (first == null || end == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float min = CELL_INSET;
        float max = 1.0F - CELL_INSET;


        for (BlockPos pos : BlockPos.betweenClosed(first, end)) {
            if (!shouldRenderSelectionCell(minecraft, pos)) {

                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

            Matrix4f matrix = poseStack.last().pose();

            addBox(builder, matrix, min, min, min, max, max, max, red, green, blue, alpha);

            poseStack.popPose();
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderDragHandle(RenderLevelStageEvent event) {
        if (!EntropyGloveFillClientState.isFillMode() || !EntropyGloveFillClientState.hasSelection() || EntropyGloveFillClientState.isComplete()) {

            return;
        }

        BlockPos end = EntropyGloveFillClientState.visibleEnd();

        if (end == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();

        float time = (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0F;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 4.2F);
        float coreSize = 0.105F + pulse * 0.025F;
        float haloSize = coreSize + 0.085F;


        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(end.getX() + 0.5D - camera.x, end.getY() + 0.5D - camera.y, end.getZ() + 0.5D - camera.z);

        poseStack.mulPose(Axis.YP.rotationDegrees(time * 85.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addBox(builder, matrix, -haloSize, -haloSize, -haloSize, haloSize, haloSize, haloSize, 0.08F, 1.0F, 0.88F, 0.10F + pulse * 0.08F);
        addBox(builder, matrix, -coreSize, -coreSize, -coreSize, coreSize, coreSize, coreSize, 0.32F, 1.0F, 0.88F, 0.62F + pulse * 0.25F);


        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static void renderBlockAnimations(RenderLevelStageEvent event) {
        if (ANIMATIONS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();

        double currentTime = minecraft.level.getGameTime() + event.getPartialTick();

        BlockRenderDispatcher renderer = minecraft.getBlockRenderer();
        Iterator<Map.Entry<BlockPos, BlockAnimation>> iterator = ANIMATIONS.entrySet().iterator();


        while (iterator.hasNext()) {

            Map.Entry<BlockPos, BlockAnimation> entry = iterator.next();
            BlockPos pos = entry.getKey();
            BlockAnimation animation = entry.getValue();

            float progress = (float) ((currentTime - animation.startTime()) / animation.duration());

            if (progress >= 1.0F) {
                iterator.remove();
                continue;
            }

            if (progress < 0.0F) {
                continue;
            }

            if (progress > 0.965F) {
                continue;
            }

            float scale = getInsertScale(progress);

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5D, -0.5D, -0.5D);

            int light = LevelRenderer.getLightColor(minecraft.level, pos);

            renderer.renderSingleBlock(animation.state(), poseStack, ANIMATION_BUFFERS, light, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
        ANIMATION_BUFFERS.endBatch();
    }

    private static float getInsertScale(float progress) {
        progress = Mth.clamp(progress, 0.0F, 1.0F);

        if (progress < 0.62F) {
            float t = smoothstep(progress / 0.62F);
            return Mth.lerp(t, 0.12F, 1.13F);
        }

        if (progress < 0.82F) {
            float t = smoothstep((progress - 0.62F) / 0.20F);
            return Mth.lerp(t, 1.13F, 0.94F);
        }

        float t = smoothstep((progress - 0.82F) / 0.18F);
        return Mth.lerp(t, 0.94F, 1.0F);
    }

    private static float smoothstep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static void addBox(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        quad(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, a);
        quad(builder, matrix, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, r, g, b, a);
        quad(builder, matrix, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, r, g, b, a);
        quad(builder, matrix, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        quad(builder, matrix, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, a);
        quad(builder, matrix, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b, a);
    }


    private static void quad(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {

        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
    }

    private record BlockAnimation(BlockState state, long startTime, int duration) {
    }
}