package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.MonkiBigModel;
import com.benji.oasiso.client.model.TitanaModel;
import com.benji.oasiso.common.entity.MonkiBigEntity;
import com.benji.oasiso.common.entity.TitanaEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TitanaRenderer extends GeoEntityRenderer<TitanaEntity> {
    public TitanaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TitanaModel());
        this.shadowRadius = 0.7f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(TitanaEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}