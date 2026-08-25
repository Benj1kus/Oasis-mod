package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AzumaalModel extends GeoModel<AzumaalEntity> {

    @Override
    public ResourceLocation getModelResource(AzumaalEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/azumaal.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AzumaalEntity animatable) {
        return animatable.getMainTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(AzumaalEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/azumaal.animation.json");
    }
}