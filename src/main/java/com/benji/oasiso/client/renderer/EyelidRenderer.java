package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.EyelidModel;
import com.benji.oasiso.common.entity.EyelidEntity;
import com.benji.oasiso.client.layer.EyelidTrailLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;

public class EyelidRenderer extends GeoEntityRenderer<EyelidEntity> {

    private final EyelidTrailLayer trailRenderer = new EyelidTrailLayer();


    public EyelidRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EyelidModel());
        this.shadowRadius = 0.0F;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public void render(EyelidEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        this.trailRenderer.render(entity, poseStack, bufferSource, partialTick);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void applyRotations(EyelidEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }

    @Override
    public boolean shouldRender(EyelidEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}