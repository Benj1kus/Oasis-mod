package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.MonkiBigEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MonkiBigModel extends GeoModel<MonkiBigEntity> {
    @Override
    public ResourceLocation getModelResource(MonkiBigEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/monki_big.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MonkiBigEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/monki_big.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MonkiBigEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/monki_big.animation.json");
    }
}