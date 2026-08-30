package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AzumalitArmorModel extends GeoModel<AzumalitArmorItem> {
    @Override
    public ResourceLocation getModelResource(AzumalitArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/azumalit_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AzumalitArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/models/armor/azumalit_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AzumalitArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/azumalit_armor.animation.json");
    }
}