package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BomboBotEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BomboBotModel extends GeoModel<BomboBotEntity> {
    @Override
    public ResourceLocation getModelResource(BomboBotEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/bombo_bot.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BomboBotEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/bombo_bot.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BomboBotEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/bombo_bot.animation.json");
    }
}