package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.OsirisSplitModel;
import com.benji.oasiso.common.entity.OsirisSplitEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OsirisSplitRenderer extends GeoEntityRenderer<OsirisSplitEntity> {
    public OsirisSplitRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new OsirisSplitModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(OsirisSplitEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}