package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SoulGiverEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SoulGiverModel extends GeoModel<SoulGiverEntity> {
    @Override
    public ResourceLocation getModelResource(SoulGiverEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/soul_giver.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SoulGiverEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/soul_giver.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SoulGiverEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/soul_giver.animation.json");
    }
}