package com.benji.oasiso.client.layer;

import com.benji.oasiso.client.renderer.AzumalitArmorRenderer;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

public class AzumalitHologramTrailLayer extends GeoRenderLayer<AzumalitArmorItem> {

    private static final int HISTORY_LENGTH = 14;
    private static final int SAMPLE_STEP = 2;
    private static final int MAX_HOLOGRAMS = 6;

    private static final float MAX_ALPHA = 0.30F;
    private static final float MIN_ALPHA = 0.035F;

    private final AzumalitArmorRenderer armorRenderer;

    private final Map<LivingEntity, TrailHistory> histories = new WeakHashMap<>();

    public AzumalitHologramTrailLayer(AzumalitArmorRenderer renderer) {
        super(renderer);
        this.armorRenderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, AzumalitArmorItem animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        LivingEntity wearer = this.armorRenderer.getCurrentEntity();

        if (wearer == null || this.armorRenderer.getCurrentSlot() != EquipmentSlot.CHEST) {
            return;
        }

        TrailHistory history = this.histories.computeIfAbsent(wearer, ignored -> new TrailHistory());

        if (!AzumalitArmorItem.hasAzumalitChestplate(wearer) || !wearer.isAlive() || wearer.isInvisible()) {

            history.clear();
            return;
        }

        captureSnapshot(wearer, history, partialTick);

        if (history.snapshots.size() < SAMPLE_STEP + 1) {
            return;
        }

        double currentX = Mth.lerp(partialTick, wearer.xOld, wearer.getX());
        double currentY = Mth.lerp(partialTick, wearer.yOld, wearer.getY());
        double currentZ = Mth.lerp(partialTick, wearer.zOld, wearer.getZ());

        float currentYaw = Mth.rotLerp(partialTick, wearer.yBodyRotO, wearer.yBodyRot);

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
            float hue = (wearer.tickCount * 0.012F + hologramIndex / (float) MAX_HOLOGRAMS) % 1.0F;
            int rgb = Mth.hsvToRgb(hue, 0.85F, 1.0F);

            float red = ((rgb >> 16) & 0xFF) / 255.0F;
            float green = ((rgb >> 8) & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;

            poseStack.pushPose();

            double deltaX = snapshot.position.x - currentX;
            double deltaY = snapshot.position.y - currentY;
            double deltaZ = snapshot.position.z - currentZ;

            double yawRadians = Math.toRadians(currentYaw - 180.0F);

            double cos = Math.cos(-yawRadians);
            double sin = Math.sin(-yawRadians);

            double localX = deltaX * cos - deltaZ * sin;
            double localZ = deltaX * sin + deltaZ * cos;

            poseStack.translate(localX, deltaY, localZ);

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

    private void captureSnapshot(LivingEntity entity, TrailHistory history, float partialTick) {
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
            this.snapshots.clear();
            this.lastCapturedTick = Integer.MIN_VALUE;
        }
    }

    private record TrailSnapshot(Vec3 position, float bodyYaw) {
    }
}
