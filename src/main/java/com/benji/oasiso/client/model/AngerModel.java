package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AngerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AngerModel extends GeoModel<AngerEntity> {
    @Override
    public ResourceLocation getModelResource(AngerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/anger.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AngerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, animatable.isShrineSoul() ? "textures/entity/anger_soul.png" : "textures/entity/anger.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AngerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/anger.animation.json");
    }
}