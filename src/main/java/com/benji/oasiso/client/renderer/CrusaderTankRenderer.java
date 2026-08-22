package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CrusaderTankModel;
import com.benji.oasiso.common.entity.CrusaderTankEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrusaderTankRenderer extends GeoEntityRenderer<CrusaderTankEntity> {
    public CrusaderTankRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrusaderTankModel());
        this.shadowRadius = 0.6f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(CrusaderTankEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}