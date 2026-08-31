package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.MouthPointModel;
import com.benji.oasiso.common.block.entity.MouthPointBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class MouthPointRenderer extends GeoBlockRenderer<MouthPointBlockEntity> {

    private static final ResourceLocation FIRST_EMISSIVE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/mouth_point_emissive.png");
    private static final ResourceLocation SECOND_EMISSIVE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/mouth_point_second_emissive.png");

    private static final int SPIRALS_PER_EMISSION = 3;

    public MouthPointRenderer(BlockEntityRendererProvider.Context context) {
        super(new MouthPointModel());

        this.addRenderLayer(new AutoGlowingGeoLayer<MouthPointBlockEntity>(this) {
            @Override
            protected RenderType getRenderType(MouthPointBlockEntity animatable) {
                return RenderType.eyes(animatable.getVariant() == 2 ? SECOND_EMISSIVE : FIRST_EMISSIVE);
            }
        });
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MouthPointBlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean smokeBone = "smoke_alt".equals(bone.getName());

        if (!isReRender && smokeBone) {
            bone.setTrackingMatrices(true);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (isReRender || !smokeBone || animatable.isSpawnAnimationActive() || !(animatable.getLevel() instanceof ClientLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!animatable.tryMarkSmokeEmission(gameTime)) {
            return;
        }

        Vec3 nozzle = resolveNozzlePosition(animatable, bone);
        spawnSmokeSpiral(level, nozzle, gameTime);
    }

    private static Vec3 resolveNozzlePosition(MouthPointBlockEntity animatable, GeoBone bone) {
        Vector3d world = bone.getWorldPosition();
        Vec3 blockCenter = Vec3.atCenterOf(animatable.getBlockPos());

        if (world != null && Double.isFinite(world.x) && Double.isFinite(world.y) && Double.isFinite(world.z)) {
            Vec3 candidate = new Vec3(world.x, world.y, world.z);
            double expectedTopY = animatable.getBlockPos().getY() + 47.1D / 16.0D;
            if (candidate.distanceToSqr(blockCenter) < 16.0D && Math.abs(candidate.y - expectedTopY) < 0.75D) {
                return candidate;
            }
        }

        return new Vec3(animatable.getBlockPos().getX() + 0.5D, animatable.getBlockPos().getY() + 47.1D / 16.0D, animatable.getBlockPos().getZ() + 0.5D + 0.25D / 16.0D);
    }

    private static void spawnSmokeSpiral(ClientLevel level, Vec3 start, long gameTime) {
        double basePhase = gameTime * 0.70D;

        for (int i = 0; i < SPIRALS_PER_EMISSION; i++) {
            double angle = basePhase + i * (Math.PI * 2.0D / SPIRALS_PER_EMISSION);
            double radius = 0.025D + level.random.nextDouble() * 0.030D;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            double tangentX = -Math.sin(angle) * (0.008D + level.random.nextDouble() * 0.007D);
            double tangentZ = Math.cos(angle) * (0.008D + level.random.nextDouble() * 0.007D);

            double velocityX = tangentX + offsetX * 0.035D;
            double velocityY = 0.20D + level.random.nextDouble() * 0.04D;
            double velocityZ = tangentZ + offsetZ * 0.035D;

            level.addParticle(Oasiso.MOUTH_SMOKE.get(), start.x + offsetX, start.y, start.z + offsetZ, velocityX, velocityY, velocityZ);
        }
    }
}
