package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.BattleHintArrowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BattleHintArrowModel extends GeoModel<BattleHintArrowEntity> {
    @Override
    public ResourceLocation getModelResource(BattleHintArrowEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/battle_hint_arrow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BattleHintArrowEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/battle_hint_arrow.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BattleHintArrowEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/battle_hint_arrow.animation.json");
    }
}