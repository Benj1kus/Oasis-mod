package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.KarakWormModel;
import com.benji.oasiso.common.entity.KarakWormEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KarakWormRenderer extends GeoEntityRenderer<KarakWormEntity> {
    public KarakWormRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KarakWormModel());
        this.shadowRadius = 0.2f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(KarakWormEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}