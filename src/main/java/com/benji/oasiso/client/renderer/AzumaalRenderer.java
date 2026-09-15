package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.*;
import com.benji.oasiso.client.model.AzumaalModel;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.benji.oasiso.client.renderer.effect.AzumaalDeathRayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public class AzumaalRenderer extends GeoEntityRenderer<AzumaalEntity> {
    public AzumaalRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AzumaalModel());
        this.shadowRadius = 0.5F;
        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new AzumaalHologramTrailLayer(this));
        addRenderLayer(new AzumaalBladeSlashLayer(this));
        addRenderLayer(new AzumaalShockwaveLayer(this));
        addRenderLayer(new AzumaalStageTwoMouthSmokeLayer(this));
        addRenderLayer(new AzumaalStageTwoMegaBeamLayer(this));
    }

    @Override
    public void render(AzumaalEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isDeathSequenceActive() && !entity.isStageTwo()) {
            AzumaalDeathRayRenderer.render(entity, partialTick, poseStack, bufferSource);
        }
        boolean megaBeamRotation = entity.isStageTwo() && (entity.getAnimState() == AzumaalEntity.STATE_STAGE_TWO_BEAM_ACTIVE || entity.getAnimState() == AzumaalEntity.STATE_STAGE_TWO_MOUTH_CLOSE);
        if (!megaBeamRotation) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

            return;
        }

        float oldBodyRot = entity.yBodyRot;
        float oldBodyRotO = entity.yBodyRotO;
        float oldHeadRot = entity.yHeadRot;
        float oldHeadRotO = entity.yHeadRotO;

        try {
            entity.yBodyRotO = entity.yRotO;
            entity.yBodyRot = entity.getYRot();
            entity.yHeadRotO = entity.yRotO;
            entity.yHeadRot = entity.getYRot();

            float smoothYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            super.render(entity, smoothYaw, partialTick, poseStack, bufferSource, packedLight);

        } finally {
            entity.yBodyRot = oldBodyRot;
            entity.yBodyRotO = oldBodyRotO;
            entity.yHeadRot = oldHeadRot;
            entity.yHeadRotO = oldHeadRotO;
        }
    }

    @Override
    public boolean shouldRender(AzumaalEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}