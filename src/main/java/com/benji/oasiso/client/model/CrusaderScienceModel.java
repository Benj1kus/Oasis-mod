package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderScienceEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrusaderScienceModel extends GeoModel<CrusaderScienceEntity> {
    @Override
    public ResourceLocation getModelResource(CrusaderScienceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/crusader_science.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrusaderScienceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/crusader_science.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrusaderScienceEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/crusader_science.animation.json");
    }
}