package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.KarakWormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KarakWormModel extends GeoModel<KarakWormEntity> {
    @Override
    public ResourceLocation getModelResource(KarakWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/karak_worm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KarakWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/karak_worm.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KarakWormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/entropy_worm.animation.json");
    }
}