package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.ApolAltarBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ApolAltarModel extends GeoModel<ApolAltarBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ApolAltarBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/apol_altar.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ApolAltarBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/apol_altar.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ApolAltarBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/apol_altar.animation.json");
    }
}