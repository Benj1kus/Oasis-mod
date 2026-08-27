package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.EntropyChestplateModel;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class EntropyChestplateRenderer extends GeoArmorRenderer<EntropyChestplateItem> {

    public EntropyChestplateRenderer() {
        super(new EntropyChestplateModel());
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}