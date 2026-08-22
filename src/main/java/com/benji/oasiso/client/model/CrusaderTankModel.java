package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderTankEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrusaderTankModel extends GeoModel<CrusaderTankEntity> {
    @Override
    public ResourceLocation getModelResource(CrusaderTankEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/crusader_tank.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrusaderTankEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/crusader_tank.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrusaderTankEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/crusader_tank.animation.json");
    }
}