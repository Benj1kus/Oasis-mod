package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.GraveSoulEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GraveSoulModel extends GeoModel<GraveSoulEntity> {
    @Override
    public ResourceLocation getModelResource(GraveSoulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/grave_soul.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GraveSoulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/grave_soul.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GraveSoulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/grave_soul.animation.json");
    }
}