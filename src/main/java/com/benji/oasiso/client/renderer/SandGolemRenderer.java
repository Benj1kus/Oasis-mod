package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.SandGolemModel;
import com.benji.oasiso.common.entity.SandGolemEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SandGolemRenderer extends GeoEntityRenderer<SandGolemEntity> {
    public SandGolemRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SandGolemModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(SandGolemEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}