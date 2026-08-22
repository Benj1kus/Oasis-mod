package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.projectile.CactoProjEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CactoProjModel extends GeoModel<CactoProjEntity> {
    @Override
    public ResourceLocation getModelResource(CactoProjEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/cacto_proj.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CactoProjEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/cacto_proj.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CactoProjEntity animatable) {
        return null;
    }
}