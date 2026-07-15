package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.SandedChestModel;
import com.benji.oasiso.common.block.entity.SandedChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SandedChestRenderer extends GeoBlockRenderer<SandedChestBlockEntity> {
    public SandedChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new SandedChestModel());
    }
}