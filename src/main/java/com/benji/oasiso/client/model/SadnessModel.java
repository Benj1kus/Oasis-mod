package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SadnessEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SadnessModel extends GeoModel<SadnessEntity> {
    @Override
    public ResourceLocation getModelResource(SadnessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sadness.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SadnessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/sadness.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SadnessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sadness.animation.json");
    }
}