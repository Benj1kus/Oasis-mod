package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CowritEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CowritModel extends GeoModel<CowritEntity> {
    @Override
    public ResourceLocation getModelResource(CowritEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/cowrit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CowritEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/cowrit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CowritEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/cowrit.animation.json");
    }
}