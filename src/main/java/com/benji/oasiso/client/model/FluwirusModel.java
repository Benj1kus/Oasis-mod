package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.FluwirusBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FluwirusModel extends GeoModel<FluwirusBlockEntity> {
    @Override
    public ResourceLocation getModelResource(FluwirusBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/fluwirus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FluwirusBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/fluwirus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FluwirusBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/fluwirus.animation.json");
    }
}