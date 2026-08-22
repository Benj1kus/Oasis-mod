package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PaladinModel extends GeoModel<PaladinEntity> {
    @Override
    public ResourceLocation getModelResource(PaladinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/paladin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PaladinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/paladin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PaladinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/paladin.animation.json");
    }
}