package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.MonkiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MonkiModel extends GeoModel<MonkiEntity> {
    @Override
    public ResourceLocation getModelResource(MonkiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/monki.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MonkiEntity animatable) {
        int variant = animatable.getVariant();
        String textureName = variant == 0 ? "monki" : "monki" + (variant + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MonkiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/monki.animation.json");
    }
}