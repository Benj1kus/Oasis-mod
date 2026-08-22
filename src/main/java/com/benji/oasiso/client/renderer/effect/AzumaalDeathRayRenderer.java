package com.benji.oasiso.client.renderer.effect;

import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.ai.AzumaalDeathManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

public final class AzumaalDeathRayRenderer {
    private static final long RANDOM_SEED = 432L;
    private static final int MAX_RAYS = 60;


    private static final int CENTER_RED = 165;
    private static final int CENTER_GREEN = 255;
    private static final int CENTER_BLUE = 245;


    private static final int EDGE_RED = 30;
    private static final int EDGE_GREEN = 210;
    private static final int EDGE_BLUE = 255;


    private AzumaalDeathRayRenderer() {
    }

    public static void render(AzumaalEntity boss, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!boss.isDeathSequenceActive()) {
            return;
        }

        float deathTime = boss.getDeathVisualTicks() + partialTick;
        float progress = Mth.clamp(deathTime / AzumaalDeathManager.DEATH_DURATION, 0.0F, 1.0F);

        float appear = smoothstep(0.02F, 0.20F, progress);
        float finale = smoothstep(0.62F, 1.0F, progress);

        int rayCount = Mth.clamp(Mth.floor(appear * 25.0F + finale * 35.0F), 0, MAX_RAYS);

        if (rayCount <= 0) {
            return;
        }

        float pulse = 0.82F + 0.18F * (0.5F + 0.5F * Mth.sin(deathTime * 0.28F));
        int centerAlpha = Mth.clamp(Mth.floor(220.0F * appear * pulse), 0, 255);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        RandomSource random = RandomSource.create(RANDOM_SEED);

        poseStack.pushPose();
        poseStack.translate(0.0D, boss.getBbHeight() * 0.50D, 0.0D);

        poseStack.mulPose(Axis.YP.rotationDegrees(deathTime * 0.55F));

        for (int i = 0; i < rayCount; i++) {

            poseStack.pushPose();

            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));


            float randomLength = 3.5F + random.nextFloat() * 5.5F;
            float length = randomLength * (0.72F + progress * 0.58F + finale * 1.05F);
            float width = (0.16F + random.nextFloat() * 0.30F) * (0.8F + finale * 0.85F);

            int rayAlpha = Mth.clamp(Mth.floor(centerAlpha * (0.72F + random.nextFloat() * 0.28F)), 0, 255);

            Matrix4f matrix = poseStack.last().pose();

            drawRayPlane(consumer, matrix, length, width, 1.0F, 0.0F, rayAlpha);
            drawRayPlane(consumer, matrix, length, width, 0.0F, 1.0F, rayAlpha);

            final float diagonal = 0.70710677F;

            drawRayPlane(consumer, matrix, length, width, diagonal, diagonal, rayAlpha);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void drawRayPlane(VertexConsumer consumer, Matrix4f matrix, float length, float width, float widthX, float widthZ, int centerAlpha) {
        float x = width * widthX;
        float z = width * widthZ;

        addVertex(consumer, matrix, 0.0F, 0.0F, 0.0F, CENTER_RED, CENTER_GREEN, CENTER_BLUE, centerAlpha);

        addVertex(consumer, matrix, 0.0F, 0.0F, 0.0F, CENTER_RED, CENTER_GREEN, CENTER_BLUE, centerAlpha);

        addVertex(consumer, matrix, -x, length, -z, EDGE_RED, EDGE_GREEN, EDGE_BLUE, 0);

        addVertex(consumer, matrix, x, length, z, EDGE_RED, EDGE_GREEN, EDGE_BLUE, 0);

        addVertex(consumer, matrix, x, length, z, EDGE_RED, EDGE_GREEN, EDGE_BLUE, 0);

        addVertex(consumer, matrix, -x, length, -z, EDGE_RED, EDGE_GREEN, EDGE_BLUE, 0);

        addVertex(consumer, matrix, 0.0F, 0.0F, 0.0F, CENTER_RED, CENTER_GREEN, CENTER_BLUE, centerAlpha);

        addVertex(consumer, matrix, 0.0F, 0.0F, 0.0F, CENTER_RED, CENTER_GREEN, CENTER_BLUE, centerAlpha);
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static float smoothstep(float start, float end, float value) {
        float progress = Mth.clamp((value - start) / (end - start),

                0.0F, 1.0F);

        return progress * progress * (3.0F - 2.0F * progress);
    }
}