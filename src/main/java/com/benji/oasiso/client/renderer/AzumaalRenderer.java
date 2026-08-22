package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.AzumaalModel;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.benji.oasiso.client.layer.AzumaalHologramTrailLayer;
import com.benji.oasiso.client.layer.AzumaalBladeSlashLayer;
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
    }

    @Override
    public void render(AzumaalEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        if (entity.isDeathSequenceActive()) {

            AzumaalDeathRayRenderer.render(entity, partialTick, poseStack, bufferSource);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(AzumaalEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}