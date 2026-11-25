package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.QuiModel;
import com.benji.oasiso.common.entity.QuiEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class QuiRenderer extends GeoEntityRenderer<QuiEntity> {
    public QuiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new QuiModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(QuiEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}