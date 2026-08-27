package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.EntropyChestplateModel;
import com.benji.oasiso.common.item.EntropyChestplateItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class EntropyChestplateRenderer extends GeoArmorRenderer<EntropyChestplateItem> {

    public EntropyChestplateRenderer() {
        super(new EntropyChestplateModel());
        this.addRenderLayer(new GlowmaskLayer<>(this));
    }
}
