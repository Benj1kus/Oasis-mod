package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.AzumaalStageTwoOutlinePass;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

public class AzumaalStageTwoMouthSmokeLayer extends GeoRenderLayer<AzumaalEntity> {

    private static final int SMOKE_INTERVAL = 40;
    private final Map<AzumaalEntity, Integer> lastEmissionTick = new WeakHashMap<>();

    public AzumaalStageTwoMouthSmokeLayer(GeoRenderer<AzumaalEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, AzumaalEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!animatable.isStageTwo()) {
            return;
        }

        if (AzumaalStageTwoOutlinePass.isRenderingMaskPass()) {
            return;
        }

        if ((animatable.tickCount + animatable.getId() * 11) % SMOKE_INTERVAL != 0) {
            return;
        }

        Integer previous = lastEmissionTick.get(animatable);

        if (previous != null && previous == animatable.tickCount) {
            return;
        }

        lastEmissionTick.put(animatable, animatable.tickCount);

        Vec3 right = getBoneWorldPosition(bakedModel, "smoke_right");
        Vec3 left = getBoneWorldPosition(bakedModel, "smoke_left");

        if (right == null || left == null) {

            return;
        }

        Vec3 mouthAxis = right.subtract(left);
        if (mouthAxis.lengthSqr() < 1.0E-6D) {
            return;
        }

        mouthAxis = mouthAxis.normalize();
        spawnSmokeBurst(right, mouthAxis);
        spawnSmokeBurst(left, mouthAxis.scale(-1.0D));
    }

    private Vec3 getBoneWorldPosition(BakedGeoModel model, String boneName) {
        GeoBone bone = model.getBone(boneName).orElse(null);

        if (bone == null) {
            return null;
        }

        Vector3d position = bone.getWorldPosition();
        return new Vec3(position.x, position.y, position.z);
    }

    private void spawnSmokeBurst(Vec3 origin, Vec3 direction) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        for (int i = 0; i < 6; i++) {

            Vec3 velocity = direction.scale(0.045D + minecraft.level.random.nextDouble() * 0.030D).add(randomSpread(minecraft, 0.020D, 0.014D));

            minecraft.level.addParticle(Oasiso.AZUMAAL_MOUTH_SMOKE.get(), origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }

        Vec3 middle = origin.add(direction.scale(0.16D));

        for (int i = 0; i < 5; i++) {

            Vec3 velocity = direction.scale(0.085D + minecraft.level.random.nextDouble() * 0.035D).add(randomSpread(minecraft, 0.025D, 0.018D));
            minecraft.level.addParticle(Oasiso.AZUMAAL_MOUTH_SMOKE.get(), middle.x, middle.y, middle.z, velocity.x, velocity.y, velocity.z);
        }
        Vec3 end = origin.add(direction.scale(0.34D));
        for (int i = 0; i < 3; i++) {
            Vec3 velocity = direction.scale(0.135D + minecraft.level.random.nextDouble() * 0.050D).add(randomSpread(minecraft, 0.030D, 0.022D));
            minecraft.level.addParticle(Oasiso.AZUMAAL_MOUTH_SMOKE.get(), end.x, end.y, end.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private Vec3 randomSpread(Minecraft minecraft, double horizontal, double vertical) {
        return new Vec3((minecraft.level.random.nextDouble() - 0.5D) * horizontal, minecraft.level.random.nextDouble() * vertical, (minecraft.level.random.nextDouble() - 0.5D) * horizontal);
    }
}