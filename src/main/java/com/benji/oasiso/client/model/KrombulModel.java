package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.KrombulEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KrombulModel extends GeoModel<KrombulEntity> {
    @Override
    public ResourceLocation getModelResource(KrombulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/krombul.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KrombulEntity animatable) {
        int frame = (animatable.tickCount / 4) % 4;
        String textureName = frame == 0 ? "krombul" : "krombul_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(KrombulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/krombul.animation.json");
    }
}