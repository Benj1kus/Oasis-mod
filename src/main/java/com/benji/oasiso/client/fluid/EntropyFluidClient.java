package com.benji.oasiso.client.fluid;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EntropyFluidClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModKarakFluids.ENTROPY_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModKarakFluids.FLOWING_ENTROPY_WATER.get(), RenderType.translucent());
        });
    }

    private EntropyFluidClient() {}
}
