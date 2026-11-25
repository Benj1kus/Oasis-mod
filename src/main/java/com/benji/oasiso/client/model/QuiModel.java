package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.QuiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class QuiModel extends GeoModel<QuiEntity> {
    @Override
    public ResourceLocation getModelResource(QuiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/qui.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(QuiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/qui.png");
    }

    @Override
    public ResourceLocation getAnimationResource(QuiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/qui.animation.json");
    }
}