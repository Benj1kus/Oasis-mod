package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderAssasinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrusaderAssasinModel extends GeoModel<CrusaderAssasinEntity> {
    @Override
    public ResourceLocation getModelResource(CrusaderAssasinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/crusader_assasin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrusaderAssasinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/crusader_assasin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrusaderAssasinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/crusader_assasin.animation.json");
    }
}