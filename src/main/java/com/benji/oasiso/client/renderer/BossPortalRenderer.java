package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.BossPortalChaosLeakLayer;
import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.BossPortalModel;
import com.benji.oasiso.common.entity.BossPortalEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BossPortalRenderer extends GeoEntityRenderer<BossPortalEntity> {

    public BossPortalRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BossPortalModel());
        this.shadowRadius = 0.1f;

        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new BossPortalChaosLeakLayer(this));
    }

    @Override
    public boolean shouldRender(BossPortalEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}