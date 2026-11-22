package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.model.FluwirusModel;
import com.benji.oasiso.common.block.entity.FluwirusBlockEntity;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluwirusRenderer extends GeoBlockRenderer<FluwirusBlockEntity> {
    public FluwirusRenderer(BlockEntityRendererProvider.Context context) {
        super(new FluwirusModel());
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FLUWIRUS_BE.get(), FluwirusRenderer::new);
    }
}
