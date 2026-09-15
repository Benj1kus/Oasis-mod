package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.OsirisTentacleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OsirisTentacleModel extends GeoModel<OsirisTentacleEntity> {
    @Override
    public ResourceLocation getModelResource(OsirisTentacleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/osiris_tentacle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OsirisTentacleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/osiris_stage2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OsirisTentacleEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/osiris_tentacle.animation.json");
    }
}