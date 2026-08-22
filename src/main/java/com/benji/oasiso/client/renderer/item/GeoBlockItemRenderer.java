package com.benji.oasiso.client.renderer.item;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.common.item.GeoBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GeoBlockItemRenderer extends GeoItemRenderer<GeoBlockItem> {

    public GeoBlockItemRenderer(GeoBlockItem item) {
        super(new GeoModel<GeoBlockItem>() {
            @Override
            public ResourceLocation getModelResource(GeoBlockItem animatable) {
                return animatable.getGeoModel();
            }

            @Override
            public ResourceLocation getTextureResource(GeoBlockItem animatable) {
                return animatable.getGeoTexture();
            }

            @Override
            public ResourceLocation getAnimationResource(GeoBlockItem animatable) {
                return animatable.getGeoAnimation();
            }
        });

        if (item.getEmissiveTexture() != null) {
            addRenderLayer(new GlowmaskLayer<>(this));
        }
    }
}