package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.QuiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;

public class QuiModel extends GeoModel<QuiEntity> {
    @Override
    public ResourceLocation getModelResource(QuiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/qui.geo.json");
    }

    @Override
    public void setCustomAnimations(QuiEntity entity, long instanceId, AnimationState<QuiEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        var root = getAnimationProcessor().getBone("qui");
        if (root == null) return;

        float partialTick = animationState.getPartialTick();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        root.setRotY(Mth.wrapDegrees(bodyYaw - entity.getFlightYaw(partialTick)) * Mth.DEG_TO_RAD);
        root.setRotX(-entity.getFlightTilt(partialTick) * Mth.DEG_TO_RAD);
        root.setRotZ(0.0F);
    }

    @Override
    public ResourceLocation getTextureResource(QuiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/qui.png");
    }

    @Override
    public ResourceLocation getAnimationResource(QuiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/qui.animation.json");
    }
}