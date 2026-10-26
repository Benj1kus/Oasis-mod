package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.HappyKnifeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HappyKnifeModel extends GeoModel<HappyKnifeEntity> {
    @Override
    public ResourceLocation getModelResource(HappyKnifeEntity e) {
        return id("geo/happy_knife.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HappyKnifeEntity e) {
        return id("textures/entity/happy_knife.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HappyKnifeEntity e) {
        return id("animations/happy_knife.animation.json");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, path);
    }
}
