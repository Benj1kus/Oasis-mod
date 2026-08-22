package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.BombulModel;
import com.benji.oasiso.client.model.DasherModel;
import com.benji.oasiso.common.entity.BombulEntity;
import com.benji.oasiso.common.entity.DasherEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BombulRenderer extends GeoEntityRenderer<BombulEntity> {
    public BombulRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BombulModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(BombulEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}