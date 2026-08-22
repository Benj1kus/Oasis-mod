package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.SuperGoldArmorModel;
import com.benji.oasiso.common.item.SuperGoldArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SuperGoldArmorRenderer extends GeoArmorRenderer<SuperGoldArmorItem> {
    public SuperGoldArmorRenderer() {
        super(new SuperGoldArmorModel());
    }
}