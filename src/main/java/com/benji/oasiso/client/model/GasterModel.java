package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.GasterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GasterModel extends GeoModel<GasterEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/gaster.geo.json");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/gaster.png");

    private static final ResourceLocation SMILE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/gaster_smile.png");

    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/gaster.animation.json");

    @Override
    public ResourceLocation getModelResource(GasterEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GasterEntity animatable) {
        return animatable.isSmiling() ? SMILE_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GasterEntity animatable) {
        return ANIMATION;
    }
}