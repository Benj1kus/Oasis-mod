package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyCreatureEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntropyCreatureModel extends GeoModel<EntropyCreatureEntity> {
    @Override
    public ResourceLocation getModelResource(EntropyCreatureEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/entropy_creature.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntropyCreatureEntity animatable) {
        int frame = (animatable.tickCount / 3) % 3;
        String textureName = frame == 0 ? "entropy_creature" : "entropy_creature_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntropyCreatureEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/entropy_creature.animation.json");
    }
}