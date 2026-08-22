package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DesertBallModel extends GeoModel<DesertBallEntity> {
    @Override
    public ResourceLocation getModelResource(DesertBallEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/desertball.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DesertBallEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/desertball.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DesertBallEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/desertball.animation.json");
    }
}