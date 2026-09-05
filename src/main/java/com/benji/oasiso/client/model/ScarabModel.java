package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ScarabEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScarabModel extends GeoModel<ScarabEntity> {

    @Override
    public ResourceLocation getModelResource(ScarabEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/scarab.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ScarabEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID,
                "textures/entity/" + animatable.getScarabTexture());
    }

    @Override
    public ResourceLocation getAnimationResource(ScarabEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/scarab.animation.json");
    }
}
