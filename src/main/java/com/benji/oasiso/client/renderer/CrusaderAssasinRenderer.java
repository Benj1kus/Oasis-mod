package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CrusaderAssasinModel;
import com.benji.oasiso.common.entity.CrusaderAssasinEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrusaderAssasinRenderer extends GeoEntityRenderer<CrusaderAssasinEntity> {
    public CrusaderAssasinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrusaderAssasinModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(CrusaderAssasinEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}