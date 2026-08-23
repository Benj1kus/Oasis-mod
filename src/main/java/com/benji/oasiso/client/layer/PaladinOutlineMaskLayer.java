package com.benji.oasiso.client.layer;

import com.benji.oasiso.client.renderer.PaladinRenderer;
import com.benji.oasiso.client.shader.PaladinOutlineSystem;
import com.benji.oasiso.common.entity.PaladinEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PaladinOutlineMaskLayer extends GeoRenderLayer<PaladinEntity> {

    private final MultiBufferSource.BufferSource maskBuffers = MultiBufferSource.immediate(new BufferBuilder(512));

    public PaladinOutlineMaskLayer(GeoRenderer<PaladinEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, PaladinEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!entity.isAlive() || entity.getAnimState() == PaladinEntity.STATE_DEATH) {
            return;
        }

        renderMask(poseStack, entity, bakedModel, partialTick, PaladinOutlineSystem.goldMask(), PaladinRenderer.OutlineMaskMode.GOLD);
        renderMask(poseStack, entity, bakedModel, partialTick, PaladinOutlineSystem.swordMask(), PaladinRenderer.OutlineMaskMode.SWORD);

        PaladinOutlineSystem.markCaptured();
    }

    private void renderMask(PoseStack poseStack, PaladinEntity entity, BakedGeoModel model, float partialTick, RenderTarget target, PaladinRenderer.OutlineMaskMode mode) {

        PaladinOutlineSystem.copySceneDepth(target);

        target.bindWrite(false);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        PaladinRenderer renderer = (PaladinRenderer) getRenderer();

        renderer.setOutlineMaskMode(mode);

        try {

            RenderType maskRenderType = RenderType.entityCutoutNoCull(renderer.getTextureLocation(entity));

            VertexConsumer maskBuffer = this.maskBuffers.getBuffer(maskRenderType);

            renderer.reRender(model, poseStack, this.maskBuffers, entity, maskRenderType, maskBuffer, partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            this.maskBuffers.endBatch(maskRenderType);

        } finally {
            renderer.clearOutlineMaskMode();
            RenderSystem.depthMask(true);
            PaladinOutlineSystem.restoreMainTarget();
        }
    }
}