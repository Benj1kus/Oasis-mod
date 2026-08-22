package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DasherModel extends GeoModel<DasherEntity> {
    @Override
    public ResourceLocation getModelResource(DasherEntity animatable) {
        if (animatable.isMagnetic()) {
            return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/dasher_magnet.geo.json");
        }
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/dasher.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DasherEntity animatable) {
        int frame = (animatable.tickCount / 4) % 4;
        String textureName = frame == 0 ? "dasher" : "dasher_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DasherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/dasher.animation.json");
    }
}