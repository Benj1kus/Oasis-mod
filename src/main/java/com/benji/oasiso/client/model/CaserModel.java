package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CaserEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CaserModel extends GeoModel<CaserEntity> {
    @Override
    public ResourceLocation getModelResource(CaserEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/caser.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CaserEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/caser.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CaserEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/caser.animation.json");
    }
}