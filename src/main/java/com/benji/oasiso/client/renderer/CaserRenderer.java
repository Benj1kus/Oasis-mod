package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.CaserLootLayer;
import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CaserModel;
import com.benji.oasiso.common.entity.CaserEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CaserRenderer extends GeoEntityRenderer<CaserEntity> {
    public CaserRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CaserModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new CaserLootLayer(this));
    }

    @Override
    public boolean shouldRender(CaserEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}