package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AzumaalModel extends GeoModel<AzumaalEntity> {

    private static final ResourceLocation STAGE_ONE_MODEL = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/azumaal.geo.json");
    private static final ResourceLocation STAGE_TWO_MODEL = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/osiris_stage2.geo.json");

    private static final ResourceLocation STAGE_ONE_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/azumaal.animation.json");
    private static final ResourceLocation STAGE_TWO_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/osiris_stage2.animation.json");

    @Override
    public ResourceLocation getModelResource(AzumaalEntity animatable) {
        return animatable.isStageTwo() ? STAGE_TWO_MODEL : STAGE_ONE_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AzumaalEntity animatable) {
        return animatable.getMainTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(AzumaalEntity animatable) {
        return animatable.isStageTwo() ? STAGE_TWO_ANIMATIONS : STAGE_ONE_ANIMATIONS;
    }
}