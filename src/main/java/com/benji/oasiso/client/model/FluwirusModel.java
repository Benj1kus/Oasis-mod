package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.FluwirusBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;

public class FluwirusModel extends GeoModel<FluwirusBlockEntity> {
    @Override
    public ResourceLocation getModelResource(FluwirusBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/fluwirus.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FluwirusBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/fluwirus.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FluwirusBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/fluwirus.animation.json");
    }

    @Override
    public void setCustomAnimations(FluwirusBlockEntity flower, long id, AnimationState<FluwirusBlockEntity> state) {
        super.setCustomAnimations(flower, id, state);
        var look = getAnimationProcessor().getBone("head_look");
        if (look != null) {
            boolean tracking = flower.isPlayingAwakeIdle(id);
            float partial = state.getPartialTick();

            look.setRotY(tracking ? flower.lookYaw(partial) : 0.0F);
            look.setRotX(tracking ? flower.lookPitch(partial) : 0.0F);
        }
    }
}
