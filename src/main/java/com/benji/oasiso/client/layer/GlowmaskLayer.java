package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.GlowmaskEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;


public class GlowmaskLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {


    public GlowmaskLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }


    @Override
    public void render(
            PoseStack poseStack,
            T animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {


        if (!(animatable instanceof GlowmaskEntity glowEntity))
            return;


        ResourceLocation glowmaskTexture =
                glowEntity.getGlowmaskTexture();


        RenderType glowRenderType =
                RenderType.eyes(glowmaskTexture);


        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                glowRenderType,
                bufferSource.getBuffer(glowRenderType),
                partialTick,
                15728880,
                packedOverlay,
                1F,
                1F,
                1F,
                1F
        );
    }
}