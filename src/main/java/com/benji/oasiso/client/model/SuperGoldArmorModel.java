package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.SuperGoldArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SuperGoldArmorModel extends GeoModel<SuperGoldArmorItem> {
    @Override
    public ResourceLocation getModelResource(SuperGoldArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/super_gold_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SuperGoldArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/models/armor/super_gold_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SuperGoldArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/super_gold_armor.animation.json");
    }
}