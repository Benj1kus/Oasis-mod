package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.WindmillBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class WindmillModel extends GeoModel<WindmillBlockEntity> {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "geo/windmill.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/windmill.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "animations/windmill.animation.json");

    @Override
    public ResourceLocation getModelResource(WindmillBlockEntity b) {
        return GEO;
    }

    @Override
    public ResourceLocation getTextureResource(WindmillBlockEntity b) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WindmillBlockEntity b) {
        return ANIMATION;
    }
}
