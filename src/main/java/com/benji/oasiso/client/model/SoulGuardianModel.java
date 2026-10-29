package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SoulGuardianEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SoulGuardianModel extends GeoModel<SoulGuardianEntity> {
    @Override
    public ResourceLocation getModelResource(SoulGuardianEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/soul_guardian.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SoulGuardianEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/soul_guardian.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SoulGuardianEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/soul_guardian.animation.json");
    }
}