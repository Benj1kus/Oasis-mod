package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ApolWingsArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ApolWingsModel extends GeoModel<ApolWingsArmorItem> {

    @Override
    public ResourceLocation getModelResource(ApolWingsArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/apol_wings.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ApolWingsArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/models/armor/apol_wings.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ApolWingsArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/apol_wings.animation.json");
    }
}
