package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CircleHintModel;
import com.benji.oasiso.common.entity.CircleHintEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CircleHintRenderer extends GeoEntityRenderer<CircleHintEntity> {
    public CircleHintRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CircleHintModel());
        this.shadowRadius = 0.0f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(CircleHintEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}