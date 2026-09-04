package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.ScarabModel;
import com.benji.oasiso.common.entity.ScarabEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class ScarabRenderer extends GeoEntityRenderer<ScarabEntity> {
    public ScarabRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ScarabModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public void render(ScarabEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        try {
            ScarabFlightRenderUtil.applyTilt(poseStack, entity, partialTick);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRender(ScarabEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}