package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ApollyonModel extends GeoModel<ApollyonEntity> {
    @Override
    public ResourceLocation getModelResource(ApollyonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/apollyon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ApollyonEntity animatable) {
        int frame = (animatable.tickCount / 6) % 6;
        String textureName = frame == 0 ? "apollyon" : "apollyon_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ApollyonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/apollyon.animation.json");
    }
}