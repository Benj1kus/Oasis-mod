package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BombulEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BombulModel extends GeoModel<BombulEntity> {
    @Override
    public ResourceLocation getModelResource(BombulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/bombul.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BombulEntity animatable) {
        int frame = (animatable.tickCount / 3) % 3;
        String textureName = frame == 0 ? "bombul" : "bombul_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(BombulEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/bombul.animation.json");
    }
}