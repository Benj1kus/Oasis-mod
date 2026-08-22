package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CircleHintEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CircleHintModel extends GeoModel<CircleHintEntity> {
    @Override
    public ResourceLocation getModelResource(CircleHintEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/circle_hint.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CircleHintEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/circle_hint.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CircleHintEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/circle_hint.animation.json");
    }
}