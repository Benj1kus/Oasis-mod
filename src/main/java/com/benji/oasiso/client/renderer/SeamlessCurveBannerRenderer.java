package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.SeamlessCurveBannerBlock;
import com.benji.oasiso.common.block.entity.SeamlessCurveBannerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SeamlessCurveBannerRenderer implements BlockEntityRenderer<SeamlessCurveBannerBlockEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/seemless_curve_banner.png");

    private static final double BANNER_WIDTH = 0.90D;
    private static final double SAG_PER_HORIZONTAL_BLOCK = 0.17D;
    private static final double HEIGHT_DIFFERENCE_SAG = 0.28D;

    private static final double MIN_SAG = 0.18D;
    private static final double MAX_SAG = 4.0D;

    private static final int SEGMENTS_PER_BLOCK = 4;

    private static final int MIN_SEGMENTS = 8;
    private static final int MAX_SEGMENTS = 128;

    private static final double TEXTURE_REPEAT_LENGTH = 1.0D;

    public SeamlessCurveBannerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SeamlessCurveBannerBlockEntity banner, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!banner.isConnected() || banner.getLevel() == null) {

            return;
        }

        Vec3 startWorld = banner.getStartPoint();
        Vec3 endWorld = banner.getEndPoint();

        if (endWorld == null) {
            return;
        }
        double totalDistance = startWorld.distanceTo(endWorld);

        if (totalDistance < 0.01D) {
            return;
        }

        double sag = calculateSag(startWorld, endWorld);
        int segments = Mth.clamp((int) Math.ceil(totalDistance * SEGMENTS_PER_BLOCK), MIN_SEGMENTS, MAX_SEGMENTS);

        Vec3 planeNormal = calculatePlaneNormal(banner, startWorld, endWorld);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        Vec3 renderOrigin = new Vec3(banner.getBlockPos().getX(), banner.getBlockPos().getY(), banner.getBlockPos().getZ());
        Vec3 previousWorld = curvePoint(startWorld, endWorld, sag, 0.0D);
        Vec3 previousLocal = previousWorld.subtract(renderOrigin);
        Vec3 previousWidth = widthDirection(startWorld, endWorld, sag, planeNormal, 0.0D).scale(BANNER_WIDTH * 0.5D);

        double textureV = 0.0D;

        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;

            Vec3 currentWorld = curvePoint(startWorld, endWorld, sag, t);
            Vec3 currentLocal = currentWorld.subtract(renderOrigin);
            Vec3 currentWidth = widthDirection(startWorld, endWorld, sag, planeNormal, t).scale(BANNER_WIDTH * 0.5D);

            double segmentLength = previousWorld.distanceTo(currentWorld);
            double nextTextureV = textureV + segmentLength / TEXTURE_REPEAT_LENGTH;

            Vec3 previousTop = previousLocal.add(previousWidth);
            Vec3 previousBottom = previousLocal.subtract(previousWidth);
            Vec3 currentTop = currentLocal.add(currentWidth);
            Vec3 currentBottom = currentLocal.subtract(currentWidth);

            int lightPrevious = LevelRenderer.getLightColor(banner.getLevel(), BlockPos.containing(previousWorld.x, previousWorld.y, previousWorld.z));
            int lightCurrent = LevelRenderer.getLightColor(banner.getLevel(), BlockPos.containing(currentWorld.x, currentWorld.y, currentWorld.z));

            vertex(consumer, pose, previousTop, 0.0F, (float) textureV, lightPrevious, packedOverlay, planeNormal);
            vertex(consumer, pose, previousBottom, 1.0F, (float) textureV, lightPrevious, packedOverlay, planeNormal);
            vertex(consumer, pose, currentBottom, 1.0F, (float) nextTextureV, lightCurrent, packedOverlay, planeNormal);
            vertex(consumer, pose, currentTop, 0.0F, (float) nextTextureV, lightCurrent, packedOverlay, planeNormal);

            previousWorld = currentWorld;
            previousLocal = currentLocal;
            previousWidth = currentWidth;

            textureV = nextTextureV;
        }
    }

    private static Vec3 curvePoint(Vec3 start, Vec3 end, double sag, double t) {
        Vec3 point = start.lerp(end, t);

        double gravity = 4.0D * sag * t * (1.0D - t);
        return point.add(0.0D, -gravity, 0.0D);
    }

    private static Vec3 widthDirection(Vec3 start, Vec3 end, double sag, Vec3 planeNormal, double t) {
        Vec3 tangent = end.subtract(start).add(0.0D, -4.0D * sag * (1.0D - 2.0D * t), 0.0D);

        if (tangent.lengthSqr() < 0.000001D) {
            tangent = new Vec3(1.0D, 0.0D, 0.0D);
        }

        tangent = tangent.normalize();
        Vec3 width = planeNormal.cross(tangent);

        if (width.lengthSqr() < 0.000001D) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        return width.normalize();
    }

    private static Vec3 calculatePlaneNormal(SeamlessCurveBannerBlockEntity banner, Vec3 start, Vec3 end) {
        Vec3 horizontal = new Vec3(end.x - start.x, 0.0D, end.z - start.z);

        if (horizontal.lengthSqr() > 0.000001D) {
            Vec3 direction = horizontal.normalize();
            return direction.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        }

        Direction face = banner.getBlockState().getValue(SeamlessCurveBannerBlock.FACING);
        Vec3 direction = new Vec3(face.getStepX(), 0.0D, face.getStepZ());

        if (direction.lengthSqr() < 0.000001D) {
            direction = new Vec3(1.0D, 0.0D, 0.0D);
        }

        return direction.normalize().cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
    }

    private static double calculateSag(Vec3 start, Vec3 end) {
        double dx = end.x - start.x;
        double dz = end.z - start.z;

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double heightDifference = Math.abs(end.y - start.y);
        double horizontalSag = horizontalDistance * SAG_PER_HORIZONTAL_BLOCK;
        double verticalSag = heightDifference * HEIGHT_DIFFERENCE_SAG;

        return Mth.clamp(Math.max(horizontalSag, verticalSag), MIN_SAG, MAX_SAG);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, float u, float v, int light, int overlay, Vec3 normal) {
        consumer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z).color(255, 255, 255, 255).uv(u, v).overlayCoords(overlay).uv2(light).normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}