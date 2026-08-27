package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EntropyChestplateGloveModel extends GeoModel<EntropyChestplateGloveItem> {

    @Override
    public ResourceLocation getModelResource(EntropyChestplateGloveItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/entropy_chestplate_glove.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntropyChestplateGloveItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/item/entropy_chestplate_glove.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntropyChestplateGloveItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/entropy_chestplate_glove.animation.json");
    }
}
