package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.OsirisTentacleModel;
import com.benji.oasiso.common.entity.OsirisTentacleEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OsirisTentacleRenderer extends GeoEntityRenderer<OsirisTentacleEntity> {
    public OsirisTentacleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new OsirisTentacleModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(OsirisTentacleEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}