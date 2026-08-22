package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.ChaosBombModel;
import com.benji.oasiso.common.entity.ChaosBombEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChaosBombRenderer extends GeoEntityRenderer<ChaosBombEntity> {
    public ChaosBombRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ChaosBombModel());
        this.shadowRadius = 0.2f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(ChaosBombEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}