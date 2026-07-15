package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.MonkiBigModel;
import com.benji.oasiso.common.entity.MonkiBigEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MonkiBigRenderer extends GeoEntityRenderer<MonkiBigEntity> {
    public MonkiBigRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MonkiBigModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(MonkiBigEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}