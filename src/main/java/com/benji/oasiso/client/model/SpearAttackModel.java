package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SpearAttackEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SpearAttackModel extends GeoModel<SpearAttackEntity> {
    @Override
    public ResourceLocation getModelResource(SpearAttackEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/spear_attack.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SpearAttackEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/spear_attack.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SpearAttackEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/spear_attack.animation.json");
    }
}