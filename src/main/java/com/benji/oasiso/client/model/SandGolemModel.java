package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.SandGolemEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SandGolemModel extends GeoModel<SandGolemEntity> {

    @Override
    public ResourceLocation getModelResource(SandGolemEntity animatable) {
        int stage = animatable.getStage();

        if (stage == 0) return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sand_golem.geo.json");
        if (stage == 1) return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sand_golem_stage1.geo.json");
        if (stage == 2) return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sand_golem_stage2.geo.json");
        if (stage == 3) return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sand_golem_stage3.geo.json");

        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sand_golem_stage4.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SandGolemEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/sand_golem.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SandGolemEntity animatable) {
        int stage = animatable.getStage();

        // До 3-й стадии используем дефолтные анимации
        if (stage <= 2) return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sand_golem.animation.json");

        if (stage == 3) return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sand_golem_stage3.animation.json");

        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sand_golem_stage4.animation.json");
    }
}