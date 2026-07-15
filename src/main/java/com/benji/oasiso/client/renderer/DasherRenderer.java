package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.DasherModel;
import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DasherRenderer extends GeoEntityRenderer<DasherEntity> {
    public DasherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DasherModel());
        this.shadowRadius = 0.7f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(DasherEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}