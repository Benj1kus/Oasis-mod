package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SandHandEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SandHandModel extends GeoModel<SandHandEntity> {
    @Override
    public ResourceLocation getModelResource(SandHandEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sand_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SandHandEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/sand_hand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SandHandEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sand_hand.animation.json");
    }
}