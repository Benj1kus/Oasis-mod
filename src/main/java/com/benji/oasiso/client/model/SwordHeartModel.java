package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SwordHeartEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SwordHeartModel extends GeoModel<SwordHeartEntity> {
    @Override
    public ResourceLocation getModelResource(SwordHeartEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sword_heart.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwordHeartEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/sword_heart.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwordHeartEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sword_heart.animation.json");
    }
}