package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BabyCowritEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BabyCowritModel extends GeoModel<BabyCowritEntity> {
    @Override
    public ResourceLocation getModelResource(BabyCowritEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/baby_cowrit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BabyCowritEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/baby_cowrit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BabyCowritEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/baby_cowrit.animation.json");
    }
}