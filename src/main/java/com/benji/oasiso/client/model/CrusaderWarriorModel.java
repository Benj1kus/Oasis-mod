package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderWarriorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrusaderWarriorModel extends GeoModel<CrusaderWarriorEntity> {
    @Override
    public ResourceLocation getModelResource(CrusaderWarriorEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/crusader_warrior.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrusaderWarriorEntity animatable) {
        int frame = (animatable.tickCount / 3) % 3;
        String textureName = frame == 0 ? "crusader_warrior" : "crusader_warrior_frame" + (frame + 1);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrusaderWarriorEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/crusader_warrior.animation.json");
    }
}