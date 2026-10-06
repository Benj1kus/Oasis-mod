package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntCreeperEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntCreeperModel extends GeoModel<EntCreeperEntity> {
    @Override
    public ResourceLocation getModelResource(EntCreeperEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/ent_creeper.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntCreeperEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, animatable.isEntropyStriking()?"textures/entity/ent_creeper_strike.png":"textures/entity/ent_creeper.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntCreeperEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/ent_creeper.animation.json");
    }
}