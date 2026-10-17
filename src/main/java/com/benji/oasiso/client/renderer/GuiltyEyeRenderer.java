package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.ChaosAltarModel;
import com.benji.oasiso.client.model.GuiltyEyeModel;
import com.benji.oasiso.common.block.entity.ChaosAltarBlockEntity;
import com.benji.oasiso.common.block.entity.CleanGeoBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class GuiltyEyeRenderer extends GeoBlockRenderer<CleanGeoBlockEntity> {

    private static final ResourceLocation EMISSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/emissive/" + "guilty_eye_emissive.png");

    public GuiltyEyeRenderer(BlockEntityRendererProvider.Context context) {
        super(new GuiltyEyeModel());

        this.addRenderLayer(new AutoGlowingGeoLayer<CleanGeoBlockEntity>(this) {

            @Override
            protected RenderType getRenderType(CleanGeoBlockEntity animatable) {
                return RenderType.eyes(EMISSIVE_TEXTURE);
            }
        });
    }
}