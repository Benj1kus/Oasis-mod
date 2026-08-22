package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

public class DesertWanderingTraderRenderer extends WanderingTraderRenderer {
    private static final ResourceLocation DESERT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/wandering_trader_desert.png");

    public DesertWanderingTraderRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WanderingTrader trader) {
        CompoundTag data = trader.getPersistentData();

        if (!data.contains("IsDesertTrader")) {
            boolean isDesert = trader.level().getBiome(trader.blockPosition()).is(net.minecraftforge.common.Tags.Biomes.IS_DESERT);
            data.putBoolean("IsDesertTrader", isDesert);
        }

        if (data.getBoolean("IsDesertTrader")) {
            return DESERT_TEXTURE;
        }

        return super.getTextureLocation(trader);
    }
}