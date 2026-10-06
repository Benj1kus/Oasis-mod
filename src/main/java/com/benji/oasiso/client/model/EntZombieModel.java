package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntZombieEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntZombieModel extends GeoModel<EntZombieEntity> {
    @Override
    public ResourceLocation getModelResource(EntZombieEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/ent_zombie.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntZombieEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/ent_zombie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntZombieEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/ent_zombie.animation.json");
    }
}