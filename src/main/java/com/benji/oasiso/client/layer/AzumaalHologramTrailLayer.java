package com.benji.oasiso.client.layer;

import com.benji.oasiso.client.model.AzumaalModel;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

public class AzumaalHologramTrailLayer extends GeoRenderLayer<AzumaalEntity> {

    private static final int HISTORY_LENGTH = 14;
    private static final int SAMPLE_STEP = 2;

    private static final int MAX_HOLOGRAMS = 6;

    private static final float MAX_ALPHA = 0.32F;
    private static final float MIN_ALPHA = 0.035F;

    private final Map<AzumaalEntity, TrailHistory> histories = new WeakHashMap<>();

    public AzumaalHologramTrailLayer(GeoRenderer<AzumaalEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, AzumaalEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        TrailHistory history = histories.computeIfAbsent(animatable, entity -> new TrailHistory());

        if (animatable.getAnimState() == AzumaalEntity.STATE_SPAWN) {
            history.clear();
            return;
        }
        if (animatable.isClone()) {
            history.clear();
            renderCloneOverlay(poseStack, animatable, bakedModel, bufferSource, partialTick, packedOverlay);
            return;
        }

        captureSnapshot(animatable, history, partialTick);

        if (history.snapshots.size() < SAMPLE_STEP + 1) {
            return;
        }

        double currentX = Mth.lerp(partialTick, animatable.xOld, animatable.getX());
        double currentY = Mth.lerp(partialTick, animatable.yOld, animatable.getY());
        double currentZ = Mth.lerp(partialTick, animatable.zOld, animatable.getZ());

        float currentYaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);

        Matrix4f inverseRenderMatrix = new Matrix4f(poseStack.last().pose()).invert();

        RenderType hologramRenderType = RenderType.entityTranslucent(getRenderer().getTextureLocation(animatable));

        int hologramIndex = 0;
        int snapshotIndex = 0;

        for (TrailSnapshot snapshot : history.snapshots) {

            if (snapshotIndex == 0 || snapshotIndex % SAMPLE_STEP != 0) {

                snapshotIndex++;
                continue;
            }

            if (hologramIndex >= MAX_HOLOGRAMS) {
                break;
            }

            float progress = hologramIndex / (float) (MAX_HOLOGRAMS - 1);
            float alpha = Mth.lerp(progress, MAX_ALPHA, MIN_ALPHA);
            float hue = (animatable.tickCount * 0.012F + hologramIndex / (float) MAX_HOLOGRAMS) % 1.0F;

            int rgb = Mth.hsvToRgb(hue, 0.85F, 1.0F);
            float red = ((rgb >> 16) & 0xFF) / 255.0F;
            float green = ((rgb >> 8) & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;

            poseStack.pushPose();

            double deltaX = snapshot.position.x - currentX;
            double deltaY = snapshot.position.y - currentY;
            double deltaZ = snapshot.position.z - currentZ;

            Vector3f localOffset = new Vector3f((float) deltaX, (float) deltaY, (float) deltaZ);

            inverseRenderMatrix.transformDirection(localOffset);

            poseStack.translate(localOffset.x, localOffset.y, localOffset.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(currentYaw - snapshot.bodyYaw));

            float scale = 1.0F + progress * 0.018F;

            poseStack.scale(scale, scale, scale);

            VertexConsumer hologramBuffer = bufferSource.getBuffer(hologramRenderType);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, hologramRenderType, hologramBuffer, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);

            poseStack.popPose();

            hologramIndex++;
            snapshotIndex++;
        }
    }

    private void renderCloneOverlay(PoseStack poseStack, AzumaalEntity animatable, BakedGeoModel bakedModel, MultiBufferSource bufferSource, float partialTick, int packedOverlay) {

        float hue = (animatable.tickCount * 0.012F + animatable.getCloneIndex() * 0.17F) % 1.0F;

        int rgb = Mth.hsvToRgb(hue, 0.85F, 1.0F);
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        RenderType cloneRenderType = RenderType.entityTranslucent(getRenderer().getTextureLocation(animatable));

        VertexConsumer cloneBuffer = bufferSource.getBuffer(cloneRenderType);

        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, cloneRenderType, cloneBuffer, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, 0.68F);
    }

    private void captureSnapshot(AzumaalEntity entity, TrailHistory history, float partialTick) {
        if (history.lastCapturedTick == entity.tickCount) {
            return;
        }

        history.lastCapturedTick = entity.tickCount;

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        history.snapshots.addFirst(new TrailSnapshot(new Vec3(x, y, z), bodyYaw));

        while (history.snapshots.size() > HISTORY_LENGTH) {

            history.snapshots.removeLast();
        }
    }

    private static final class TrailHistory {

        private final Deque<TrailSnapshot> snapshots = new ArrayDeque<>();

        private int lastCapturedTick = Integer.MIN_VALUE;

        private void clear() {
            snapshots.clear();

            lastCapturedTick = Integer.MIN_VALUE;
        }
    }

    private record TrailSnapshot(Vec3 position, float bodyYaw) {
    }
}