package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropySpiderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class EntropySpiderModel extends GeoModel<EntropySpiderEntity> {
    private final EntropySpiderIK ik = new EntropySpiderIK();

    @Override
    public ResourceLocation getModelResource(EntropySpiderEntity entity) {
        return id("geo/entropy_spider.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntropySpiderEntity entity) {
        return id("textures/entity/entropy_spider.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntropySpiderEntity entity) {
        return id("animations/entropy_spider.animation.json");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, path);
    }

    @Override
    public void setCustomAnimations(EntropySpiderEntity entity, long instanceId, AnimationState<EntropySpiderEntity> state) {
        super.setCustomAnimations(entity, instanceId, state);
        ik.apply(this, entity, state.getPartialTick());
    }
}
