package com.benji.oasiso.client.model;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.EntropyWellBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class EntropyWellModel extends GeoModel<EntropyWellBlockEntity> {
    public static final int FRAME_TICKS = 4;

    private static ResourceLocation id(String s) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, s);
    }

    private static final ResourceLocation GEO = id("geo/entropy_well.geo.json"), ANIM = id("animations/entropy_well.animation.json");
    private static final ResourceLocation[] BASE = new ResourceLocation[5], GLOW = new ResourceLocation[5];

    static {
        for (int i = 0; i < 5; i++) {
            String name = "entropy_well" + (i == 0 ? "" : Integer.toString(i + 1));
            BASE[i] = id("textures/block/" + name + ".png");
            GLOW[i] = id("textures/block/emissive/" + name + "_emissive.png");
        }
    }

    private static int frame(EntropyWellBlockEntity b) {
        return b.getLevel() == null ? 0 : (int) ((b.getLevel().getGameTime() / FRAME_TICKS) % 5);
    }

    public static ResourceLocation glow(EntropyWellBlockEntity b) {
        return GLOW[frame(b)];
    }

    @Override
    public ResourceLocation getModelResource(EntropyWellBlockEntity b) {
        return GEO;
    }

    @Override
    public ResourceLocation getAnimationResource(EntropyWellBlockEntity b) {
        return ANIM;
    }

    @Override
    public ResourceLocation getTextureResource(EntropyWellBlockEntity b) {
        return BASE[frame(b)];
    }
}
