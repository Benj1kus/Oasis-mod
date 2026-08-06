package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.ChaosAltarBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ChaosAltarModel extends GeoModel<ChaosAltarBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ChaosAltarBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/chaos_altar.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChaosAltarBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/chaos_altar.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChaosAltarBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/chaos_altar.animation.json");
    }
}