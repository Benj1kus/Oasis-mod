package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.KrombulModel;
import com.benji.oasiso.common.entity.KrombulEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KrombulRenderer extends GeoEntityRenderer<KrombulEntity> {
    public KrombulRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KrombulModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(KrombulEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}