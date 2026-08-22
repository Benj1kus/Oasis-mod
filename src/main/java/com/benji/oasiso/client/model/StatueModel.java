package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.StatueBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.model.GeoModel;
import net.minecraftforge.registries.ForgeRegistries;

public class StatueModel extends GeoModel<StatueBlockEntity> {

    @Override
    public ResourceLocation getModelResource(StatueBlockEntity animatable) {
        String name = getBlockRegistryName(animatable);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/" + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StatueBlockEntity animatable) {
        String name = getBlockRegistryName(animatable);
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(StatueBlockEntity animatable) {
        return null;
    }

    private String getBlockRegistryName(StatueBlockEntity animatable) {
        if (animatable == null || animatable.getBlockState() == null) {
            return "monki_statue";
        }
        Block block = animatable.getBlockState().getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

        if (blockId != null && blockId.getNamespace().equals(Oasiso.MODID)) {
            return blockId.getPath();
        }

        return "monki_statue"; // fallback
    }
}