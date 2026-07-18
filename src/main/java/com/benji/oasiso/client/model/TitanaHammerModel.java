package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.TitanaHammerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TitanaHammerModel extends GeoModel<TitanaHammerItem> {
    @Override
    public ResourceLocation getModelResource(TitanaHammerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/titana_hammer.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TitanaHammerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/item/titana_hammer.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TitanaHammerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/empty.json");
    }
}