package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.BabyCowritModel;
import com.benji.oasiso.common.entity.BabyCowritEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BabyCowritRenderer extends GeoEntityRenderer<BabyCowritEntity> {
    public BabyCowritRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BabyCowritModel());
        this.shadowRadius = 0.2f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(BabyCowritEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}