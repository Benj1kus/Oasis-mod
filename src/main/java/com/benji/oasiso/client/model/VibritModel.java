package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.VibritBlock;
import com.benji.oasiso.common.block.entity.VibritBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class VibritModel extends GeoModel<VibritBlockEntity> {
    private static final ResourceLocation GEO_NORMAL = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/vibrit.geo.json");
    private static final ResourceLocation GEO_MID = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/vibrit_mid.geo.json");
    private static final ResourceLocation GEO_SMALL = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/vibrit_small.geo.json");

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/vibrit.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/vibrit.animation.json");

    @Override
    public ResourceLocation getModelResource(VibritBlockEntity animatable) {
        if (animatable.getBlockState().getBlock() instanceof VibritBlock) {
            VibritBlock.VibritType type = animatable.getBlockState().getValue(VibritBlock.TYPE);
            return switch (type) {
                case MID -> GEO_MID;
                case SMALL -> GEO_SMALL;
                default -> GEO_NORMAL;
            };
        }
        return GEO_NORMAL;
    }

    @Override
    public ResourceLocation getTextureResource(VibritBlockEntity b) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VibritBlockEntity b) {
        return ANIMATION;
    }
}