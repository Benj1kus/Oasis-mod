package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CactoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CactoModel extends GeoModel<CactoEntity> {
    @Override
    public ResourceLocation getModelResource(CactoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/cacto.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CactoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/cacto.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CactoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/cacto.animation.json");
    }
}