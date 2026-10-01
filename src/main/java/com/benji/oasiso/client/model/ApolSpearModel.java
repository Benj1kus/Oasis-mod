package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ApolSpearItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ApolSpearModel extends GeoModel<ApolSpearItem> {
    @Override
    public ResourceLocation getModelResource(ApolSpearItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/apol_spear.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ApolSpearItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/item/apol_spear.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ApolSpearItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/apol_spear.animation.json");
    }
}