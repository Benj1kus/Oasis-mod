package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.MonkiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GlowmaskLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    public GlowmaskLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        ResourceLocation glowmaskTexture = null;

        if (animatable instanceof MonkiEntity monki) {
            int variant = monki.getVariant();
            String textureName = variant == 0 ? "monki_emissive" : "monki" + (variant + 1) + "_emissive";
            glowmaskTexture = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/emissive/" + textureName + ".png");
        }

        if (glowmaskTexture != null) {
            RenderType glowRenderType = RenderType.eyes(glowmaskTexture);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glowRenderType,
                    bufferSource.getBuffer(glowRenderType), partialTick,
                    15728880, packedOverlay,
                    1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}