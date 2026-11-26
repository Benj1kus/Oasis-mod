package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.CrusaderSkelBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrusaderSkelModel extends GeoModel<CrusaderSkelBlockEntity> {
    @Override
    public ResourceLocation getModelResource(CrusaderSkelBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/crusader_skel.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrusaderSkelBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/crusader_skel.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrusaderSkelBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/crusader_skel.animation.json");
    }
}