package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.TitanaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TitanaModel extends GeoModel<TitanaEntity> {
    @Override
    public ResourceLocation getModelResource(TitanaEntity animatable) {
        if (animatable.getModelState() == 1) {
            return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/titana_without.geo.json");
        }
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/titana.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TitanaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/titana.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TitanaEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/titana.animation.json");
    }
}