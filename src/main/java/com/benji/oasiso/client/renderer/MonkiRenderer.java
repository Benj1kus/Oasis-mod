package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.MonkiModel;
import com.benji.oasiso.common.entity.MonkiEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MonkiRenderer extends GeoEntityRenderer<MonkiEntity> {
    public MonkiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MonkiModel());
        this.shadowRadius = 0.3f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(MonkiEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}