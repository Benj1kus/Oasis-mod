package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.FactoryCoreBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class FactoryCoreModel extends GeoModel<FactoryCoreBlockEntity> {
    public static final int FRAME_TICKS = 4;

    private static ResourceLocation id(String s) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, s);
    }

    private static final ResourceLocation GEO = id("geo/factory_core.geo.json"), ANIM = id("animations/factory_core.animation.json");
    private static final ResourceLocation[] BASE = new ResourceLocation[4], GLOW = new ResourceLocation[4];

    static {
        for (int i = 0; i < 4; i++) {
            String name = "factory_core" + (i == 0 ? "" : Integer.toString(i + 1));
            BASE[i] = id("textures/block/" + name + ".png");
            GLOW[i] = id("textures/block/emissive/" + name + "_emissive.png");
        }
    }

    private static int frame(FactoryCoreBlockEntity b) {
        return b.getLevel() == null ? 0 : (int) ((b.getLevel().getGameTime() / FRAME_TICKS) % 4);
    }

    public static ResourceLocation glow(FactoryCoreBlockEntity b) {
        return GLOW[frame(b)];
    }

    @Override
    public ResourceLocation getModelResource(FactoryCoreBlockEntity b) {
        return GEO;
    }

    @Override
    public ResourceLocation getAnimationResource(FactoryCoreBlockEntity b) {
        return ANIM;
    }

    @Override
    public ResourceLocation getTextureResource(FactoryCoreBlockEntity b) {
        return BASE[frame(b)];
    }
}
