package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ChaosBombEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ChaosBombModel extends GeoModel<ChaosBombEntity> {
    @Override
    public ResourceLocation getModelResource(ChaosBombEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/chaos_bomb.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChaosBombEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/chaos_bomb.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChaosBombEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/chaos_bomb.animation.json");
    }
}