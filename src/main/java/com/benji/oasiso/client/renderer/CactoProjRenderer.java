package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.CactoProjModel;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CactoProjRenderer extends GeoEntityRenderer<CactoProjEntity> {
    public CactoProjRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CactoProjModel());
    }

    @Override
    public void render(CactoProjEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));

        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}