package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.SandedChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SandedChestModel extends GeoModel<SandedChestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(SandedChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/sanded_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SandedChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/sanded_chest.png"); // Обрати внимание, я положил в папку block
    }

    @Override
    public ResourceLocation getAnimationResource(SandedChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/sanded_chest.animation.json");
    }
}