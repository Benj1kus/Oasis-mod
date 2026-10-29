package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.HappinessEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HappinessModel extends GeoModel<HappinessEntity> {
    @Override
    public ResourceLocation getModelResource(HappinessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/happiness.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HappinessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, animatable.isShrineSoul() ? "textures/entity/happiness_soul.png" : "textures/entity/happiness.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HappinessEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/happiness.animation.json");
    }
}