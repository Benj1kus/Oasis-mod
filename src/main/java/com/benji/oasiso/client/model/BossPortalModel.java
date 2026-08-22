package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BossPortalEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BossPortalModel extends GeoModel<BossPortalEntity> {
    @Override
    public ResourceLocation getModelResource(BossPortalEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/boss_portal.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BossPortalEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/boss_portal.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BossPortalEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/boss_portal.animation.json");
    }
}