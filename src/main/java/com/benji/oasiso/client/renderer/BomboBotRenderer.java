package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.BomboBotModel;
import com.benji.oasiso.common.entity.BomboBotEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BomboBotRenderer extends GeoEntityRenderer<BomboBotEntity> {
    public BomboBotRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BomboBotModel());
        this.shadowRadius = 0.2f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(BomboBotEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}