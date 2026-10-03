package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.ApolAltarModel;
import com.benji.oasiso.common.block.entity.ApolAltarBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ApolAltarRenderer extends GeoBlockRenderer<ApolAltarBlockEntity> {

    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/emissive/" + "apol_altar_emissive.png");

    public ApolAltarRenderer(BlockEntityRendererProvider.Context context) {
        super(new ApolAltarModel());
        this.addRenderLayer(new GeoRenderLayer<ApolAltarBlockEntity>(this) {
            @Override
            public void render(PoseStack poseStack, ApolAltarBlockEntity altar, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                ApolSummoningBeam.trackAltar(altar);
            }
        });

        this.addRenderLayer(new AutoGlowingGeoLayer<ApolAltarBlockEntity>(this) {

            @Override
            protected RenderType getRenderType(ApolAltarBlockEntity animatable) {
                return RenderType.eyes(EMISSIVE_TEXTURE);
            }
        });
    }
}