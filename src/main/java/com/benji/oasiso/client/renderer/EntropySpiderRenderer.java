package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.EntropySpiderModel;
import com.benji.oasiso.common.entity.EntropySpiderEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EntropySpiderRenderer extends GeoEntityRenderer<EntropySpiderEntity> {
    public EntropySpiderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EntropySpiderModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(EntropySpiderEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}