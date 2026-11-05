package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.SoulArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SoulArmorModel extends GeoModel<SoulArmorItem> {
    @Override
    public ResourceLocation getModelResource(SoulArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/soul_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SoulArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/models/armor/soul_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SoulArmorItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/soul_armor.animation.json");
    }
}