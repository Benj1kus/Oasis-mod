package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.layer.SwordHeartAuraLayer;
import com.benji.oasiso.client.model.SwordHeartModel;
import com.benji.oasiso.common.entity.SwordHeartEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SwordHeartRenderer extends GeoEntityRenderer<SwordHeartEntity> {

    public SwordHeartRenderer(EntityRendererProvider.Context context) {
        super(context, new SwordHeartModel());

        this.shadowRadius = 0.0F;

        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new SwordHeartAuraLayer(this));
    }

    @Override
    public boolean shouldRender(SwordHeartEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}