package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.OsirisSplitEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class OsirisSplitModel extends GeoModel<OsirisSplitEntity> {
    @Override
    public ResourceLocation getModelResource(OsirisSplitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/osiris_split.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OsirisSplitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/osiris_split.png");
    }

    @Override
    public ResourceLocation getAnimationResource(OsirisSplitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/osiris_split.animation.json");
    }
}