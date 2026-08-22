package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CactoModel;
import com.benji.oasiso.common.entity.CactoEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CactoRenderer extends GeoEntityRenderer<CactoEntity> {
    public CactoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CactoModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(CactoEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}