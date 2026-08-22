package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.GasterModel;
import com.benji.oasiso.common.entity.GasterEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GasterRenderer extends GeoEntityRenderer<GasterEntity> {
    public GasterRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GasterModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public boolean shouldRender(GasterEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}