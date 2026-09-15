package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.AzumaalStageTwoOutlinePass;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.benji.oasiso.common.entity.ai.AzumaalDeathManager;
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

public class AzumaalStageTwoDeathBubbleLayer extends GeoRenderLayer<AzumaalEntity> {

    private static final int EMISSION_INTERVAL = 2;

    private final Map<AzumaalEntity, Integer> lastEmissionTick = new WeakHashMap<>();

    public AzumaalStageTwoDeathBubbleLayer(GeoRenderer<AzumaalEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, AzumaalEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!entity.isStageTwo() || !entity.isDeathSequenceActive()) {
            return;
        }

        if (AzumaalStageTwoOutlinePass.isRenderingMaskPass()) {
            return;
        }

        int deathTicks = entity.getDeathVisualTicks();

        if (deathTicks < AzumaalDeathManager.STAGE_TWO_PUDDLE_TICK) {
            return;
        }

        if (deathTicks >= AzumaalDeathManager.STAGE_TWO_DEATH_DURATION) {
            return;
        }

        if (entity.tickCount % EMISSION_INTERVAL != 0) {
            return;
        }

        Integer previous = lastEmissionTick.get(entity);

        if (previous != null && previous == entity.tickCount) {
            return;
        }

        lastEmissionTick.put(entity, entity.tickCount);
        GeoBone luja = bakedModel.getBone("luja").orElse(null);

        if (luja == null) {
            return;
        }

        Vector3d worldPosition = luja.getWorldPosition();
        Vec3 origin = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        spawnBubbleBurst(origin);
    }

    private void spawnBubbleBurst(Vec3 origin) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        int count = 4 + minecraft.level.random.nextInt(4);
        for (int i = 0; i < count; i++) {

            double angle = minecraft.level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.15D + minecraft.level.random.nextDouble() * 1.80D;
            double spawnX = origin.x + Math.cos(angle) * radius;
            double spawnY = origin.y + 0.03D + minecraft.level.random.nextDouble() * 0.12D;
            double spawnZ = origin.z + Math.sin(angle) * radius;

            double swirlSpeed = 0.018D + minecraft.level.random.nextDouble() * 0.045D;
            double radialDrift = (minecraft.level.random.nextDouble() - 0.5D) * 0.018D;
            double vx = -Math.sin(angle) * swirlSpeed + Math.cos(angle) * radialDrift;
            double vz = Math.cos(angle) * swirlSpeed + Math.sin(angle) * radialDrift;
            double vy = 0.040D + minecraft.level.random.nextDouble() * 0.070D;

            minecraft.level.addParticle(Oasiso.ARM_SMOKE.get(), spawnX, spawnY, spawnZ, vx, vy, vz);
        }
    }
}