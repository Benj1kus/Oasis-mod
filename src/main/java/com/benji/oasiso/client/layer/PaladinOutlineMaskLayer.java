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

        renderSwordMask(poseStack, entity, bakedModel, partialTick, PaladinOutlineSystem.swordMask());

        PaladinOutlineSystem.markCaptured();
    }

    private void renderSwordMask(PoseStack poseStack, PaladinEntity entity, BakedGeoModel model, float partialTick, RenderTarget target) {
        PaladinOutlineSystem.copySceneDepth(target);
        target.bindWrite(false);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        PaladinRenderer renderer = (PaladinRenderer) getRenderer();

        renderer.setOutlineMaskMode(PaladinRenderer.OutlineMaskMode.SWORD);

        try {
            RenderType type = RenderType.entityCutoutNoCull(renderer.getTextureLocation(entity));

            VertexConsumer maskBuffer = this.maskBuffers.getBuffer(type);

            renderer.reRender(model, poseStack, this.maskBuffers, entity, type, maskBuffer, partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            this.maskBuffers.endBatch(type);

        } finally {
            renderer.clearOutlineMaskMode();
            RenderSystem.depthMask(true);
            PaladinOutlineSystem.restoreMainTarget();
        }
    }
}