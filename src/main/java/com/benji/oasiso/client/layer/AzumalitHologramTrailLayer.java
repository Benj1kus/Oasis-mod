package com.benji.oasiso.client.layer;

import com.benji.oasiso.client.renderer.AzumalitArmorRenderer;
import com.benji.oasiso.client.renderer.AzumalitHologramRenderType;
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
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class AzumalitHologramTrailLayer extends GeoRenderLayer<AzumalitArmorItem> {

    private static final int AFTERIMAGE_INTERVAL_TICKS = 3;
    private static final int AFTERIMAGE_LIFETIME_TICKS = 20;
    private static final int MAX_HOLOGRAMS = 6;

    private static final float MAX_ALPHA = 0.32F;
    private static final float MIN_ALPHA = 0.025F;

    private static final double MIN_CAPTURE_DISTANCE = 0.12D;
    private static final double MIN_CAPTURE_DISTANCE_SQR = MIN_CAPTURE_DISTANCE * MIN_CAPTURE_DISTANCE;

    private static final double MIN_HOLOGRAM_DISTANCE = 0.16D;
    private static final double MIN_HOLOGRAM_DISTANCE_SQR = MIN_HOLOGRAM_DISTANCE * MIN_HOLOGRAM_DISTANCE;

    private static final double MIN_MOVEMENT_PER_TICK = 0.015D;
    private static final double MIN_MOVEMENT_PER_TICK_SQR = MIN_MOVEMENT_PER_TICK * MIN_MOVEMENT_PER_TICK;

    private static final double MAX_SNAPSHOT_JUMP = 6.0D;
    private static final double MAX_SNAPSHOT_JUMP_SQR = MAX_SNAPSHOT_JUMP * MAX_SNAPSHOT_JUMP;

    private static final double VERTICAL_MOTION_THRESHOLD = 0.025D;
    private static final double VERTICAL_BASE_SEPARATION = 0.20D;
    private static final double VERTICAL_SPEED_MULTIPLIER = 0.65D;
    private static final double VERTICAL_MAX_SPEED_EXTRA = 0.30D;

    private static final float RAINBOW_SPEED = 0.045F;

    private final AzumalitArmorRenderer armorRenderer;

    private final Map<LivingEntity, EnumMap<EquipmentSlot, TrailHistory>> histories = new WeakHashMap<>();

    public AzumalitHologramTrailLayer(AzumalitArmorRenderer renderer) {
        super(renderer);
        this.armorRenderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, AzumalitArmorItem animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        LivingEntity wearer = this.armorRenderer.getCurrentEntity();
        EquipmentSlot slot = this.armorRenderer.getCurrentSlot();

        if (wearer == null || slot == null) {
            return;
        }

        EnumMap<EquipmentSlot, TrailHistory> entityHistories = this.histories.computeIfAbsent(wearer, ignored -> new EnumMap<>(EquipmentSlot.class));
        TrailHistory history = entityHistories.computeIfAbsent(slot, ignored -> new TrailHistory());

        if (!wearer.isAlive() || wearer.isInvisible() || !(wearer.getItemBySlot(slot).getItem() instanceof AzumalitArmorItem)) {

            history.clear();
            return;
        }

        removeExpiredSnapshots(history, wearer.tickCount);

        captureSnapshot(wearer, bakedModel, history, partialTick);

        if (history.snapshots.isEmpty()) {
            return;
        }

        double currentX = Mth.lerp(partialTick, wearer.xOld, wearer.getX());
        double currentY = Mth.lerp(partialTick, wearer.yOld, wearer.getY());
        double currentZ = Mth.lerp(partialTick, wearer.zOld, wearer.getZ());

        float currentYaw = Mth.rotLerp(partialTick, wearer.yBodyRotO, wearer.yBodyRot);

        RenderType hologramRenderType = AzumalitHologramRenderType.hologram(getRenderer().getTextureLocation(animatable));

        Map<String, BonePose> livePose = captureModelPose(bakedModel);

        int renderedHolograms = 0;

        try {
            for (TrailSnapshot snapshot : history.snapshots) {
                if (renderedHolograms >= MAX_HOLOGRAMS) {
                    break;
                }

                double deltaX = snapshot.position().x - currentX;
                double deltaY = snapshot.position().y - currentY;
                double deltaZ = snapshot.position().z - currentZ;

                double distanceSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

                if (distanceSqr < MIN_HOLOGRAM_DISTANCE_SQR) {
                    continue;
                }

                float age = Mth.clamp((wearer.tickCount + partialTick - snapshot.tick()) / (float) AFTERIMAGE_LIFETIME_TICKS, 0.0F, 1.0F);
                float alpha = Mth.lerp(age, MAX_ALPHA, MIN_ALPHA);

                int rgb = Mth.hsvToRgb(snapshot.hue(), 0.85F, 1.0F);

                float red = ((rgb >> 16) & 0xFF) / 255.0F;
                float green = ((rgb >> 8) & 0xFF) / 255.0F;
                float blue = (rgb & 0xFF) / 255.0F;

                poseStack.pushPose();

                try {

                    double yawRadians = Math.toRadians(currentYaw - 180.0F);

                    double cos = Math.cos(-yawRadians);
                    double sin = Math.sin(-yawRadians);

                    double localX = deltaX * cos - deltaZ * sin;
                    double localZ = deltaX * sin + deltaZ * cos;

                    poseStack.translate(localX, -deltaY, localZ);
                    poseStack.mulPose(Axis.YP.rotationDegrees(currentYaw - snapshot.bodyYaw()));

                    applyModelPose(bakedModel, snapshot.bonePose());

                    if (slot == EquipmentSlot.CHEST) {
                        forceMagicArmHologramsVisible(bakedModel);
                    }

                    VertexConsumer hologramBuffer = bufferSource.getBuffer(hologramRenderType);

                    getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, hologramRenderType, hologramBuffer, partialTick, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);

                } finally {
                    poseStack.popPose();
                }

                renderedHolograms++;
            }

        } finally {
            applyModelPose(bakedModel, livePose);
        }
    }

    private void captureSnapshot(LivingEntity entity, BakedGeoModel model, TrailHistory history, float partialTick) {

        if (history.lastProcessedTick == entity.tickCount) {
            return;
        }

        history.lastProcessedTick = entity.tickCount;

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        Vec3 rawPosition = new Vec3(x, y, z);

        if (history.lastObservedPosition != null && history.lastObservedPosition.distanceToSqr(rawPosition) > MAX_SNAPSHOT_JUMP_SQR) {

            history.clearSnapshotsOnly();
        }

        history.lastObservedPosition = rawPosition;

        double motionX = entity.getX() - entity.xOld;
        double motionY = entity.getY() - entity.yOld;
        double motionZ = entity.getZ() - entity.zOld;

        double movementSqr = motionX * motionX + motionY * motionY + motionZ * motionZ;

        if (movementSqr < MIN_MOVEMENT_PER_TICK_SQR) {
            return;
        }

        if (history.lastSnapshotTick != Integer.MIN_VALUE && entity.tickCount - history.lastSnapshotTick < AFTERIMAGE_INTERVAL_TICKS) {

            return;
        }

        Vec3 snapshotPosition = applyVerticalAfterimagePhysics(rawPosition, motionY);

        TrailSnapshot newest = history.snapshots.peekFirst();

        if (newest != null && newest.position().distanceToSqr(snapshotPosition) < MIN_CAPTURE_DISTANCE_SQR) {

            return;
        }

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

        float hue = (entity.tickCount * RAINBOW_SPEED) % 1.0F;

        Map<String, BonePose> bonePose = captureModelPose(model);

        history.snapshots.addFirst(new TrailSnapshot(snapshotPosition, bodyYaw, entity.tickCount, hue, bonePose));

        history.lastSnapshotTick = entity.tickCount;

        while (history.snapshots.size() > MAX_HOLOGRAMS) {
            history.snapshots.removeLast();
        }
    }

    private Vec3 applyVerticalAfterimagePhysics(Vec3 position, double motionY) {
        if (Math.abs(motionY) < VERTICAL_MOTION_THRESHOLD) {
            return position;
        }

        double direction = motionY > 0.0D ? -1.0D : 1.0D;

        double speedExtra = Math.min(Math.abs(motionY) * VERTICAL_SPEED_MULTIPLIER, VERTICAL_MAX_SPEED_EXTRA);

        double separation = VERTICAL_BASE_SEPARATION + speedExtra;

        return position.add(0.0D, direction * separation, 0.0D);
    }

    private void forceMagicArmHologramsVisible(BakedGeoModel model) {
        model.getBone("armorBody").ifPresent(bone -> {
            bone.setHidden(false);
            bone.setChildrenHidden(false);
        });

        forceBoneSubtreeVisible(model, "magic_arm_left");

        forceBoneSubtreeVisible(model, "magic_arm_right");
    }

    private void forceBoneSubtreeVisible(BakedGeoModel model, String boneName) {
        model.getBone(boneName).ifPresent(this::forceBoneSubtreeVisible);
    }

    private void forceBoneSubtreeVisible(GeoBone bone) {
        bone.setHidden(false);
        bone.setChildrenHidden(false);

        for (GeoBone child : bone.getChildBones()) {

            forceBoneSubtreeVisible(child);
        }
    }

    private void removeExpiredSnapshots(TrailHistory history, int currentTick) {
        while (!history.snapshots.isEmpty()) {
            TrailSnapshot oldest = history.snapshots.peekLast();

            if (currentTick - oldest.tick() <= AFTERIMAGE_LIFETIME_TICKS) {

                break;
            }

            history.snapshots.removeLast();
        }
    }

    private Map<String, BonePose> captureModelPose(BakedGeoModel model) {
        Map<String, BonePose> poses = new HashMap<>();

        for (GeoBone bone : model.topLevelBones()) {
            captureBonePose(bone, poses);
        }

        return poses;
    }

    private void captureBonePose(GeoBone bone, Map<String, BonePose> poses) {
        poses.put(bone.getName(), BonePose.capture(bone));

        for (GeoBone child : bone.getChildBones()) {
            captureBonePose(child, poses);
        }
    }

    private void applyModelPose(BakedGeoModel model, Map<String, BonePose> poses) {
        for (GeoBone bone : model.topLevelBones()) {
            applyBonePose(bone, poses);
        }
    }

    private void applyBonePose(GeoBone bone, Map<String, BonePose> poses) {
        BonePose pose = poses.get(bone.getName());

        if (pose != null) {
            pose.apply(bone);
        }

        for (GeoBone child : bone.getChildBones()) {
            applyBonePose(child, poses);
        }
    }

    private static final class TrailHistory {

        private final Deque<TrailSnapshot> snapshots = new ArrayDeque<>();

        private int lastProcessedTick = Integer.MIN_VALUE;
        private int lastSnapshotTick = Integer.MIN_VALUE;

        private Vec3 lastObservedPosition;

        private void clearSnapshotsOnly() {
            this.snapshots.clear();
            this.lastSnapshotTick = Integer.MIN_VALUE;
        }

        private void clear() {
            this.snapshots.clear();
            this.lastProcessedTick = Integer.MIN_VALUE;
            this.lastSnapshotTick = Integer.MIN_VALUE;
            this.lastObservedPosition = null;
        }
    }

    private record TrailSnapshot(Vec3 position, float bodyYaw, int tick, float hue, Map<String, BonePose> bonePose) {
    }

    private record BonePose(float rotX, float rotY, float rotZ, float posX, float posY, float posZ, float scaleX,
                            float scaleY, float scaleZ, float pivotX, float pivotY, float pivotZ, boolean hidden,
                            boolean childrenHidden) {

        private static BonePose capture(GeoBone bone) {
            return new BonePose(bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                    bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                    bone.getScaleX(), bone.getScaleY(), bone.getScaleZ(),
                    bone.getPivotX(), bone.getPivotY(), bone.getPivotZ(),
                    bone.isHidden(), bone.isHidingChildren());
        }

        private void apply(GeoBone bone) {
            bone.setRotX(this.rotX);
            bone.setRotY(this.rotY);
            bone.setRotZ(this.rotZ);

            bone.setPosX(this.posX);
            bone.setPosY(this.posY);
            bone.setPosZ(this.posZ);

            bone.setScaleX(this.scaleX);
            bone.setScaleY(this.scaleY);
            bone.setScaleZ(this.scaleZ);

            bone.setPivotX(this.pivotX);
            bone.setPivotY(this.pivotY);
            bone.setPivotZ(this.pivotZ);

            bone.setHidden(this.hidden);
            bone.setChildrenHidden(this.childrenHidden);
        }
    }
}
