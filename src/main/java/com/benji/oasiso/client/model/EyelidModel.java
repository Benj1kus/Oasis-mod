package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EyelidEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EyelidModel extends GeoModel<EyelidEntity> {
    @Override
    public ResourceLocation getModelResource(EyelidEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/eyelid.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EyelidEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/eyelid.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EyelidEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/eyelid.animation.json");
    }
}