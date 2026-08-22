package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.layer.PaladinShockwaveLayer;
import com.benji.oasiso.client.layer.PaladinSwordSlashLayer;
import com.benji.oasiso.client.model.PaladinModel;
import com.benji.oasiso.common.entity.PaladinEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.benji.oasiso.client.layer.PaladinQteHeartAnchorLayer;

public class PaladinRenderer extends GeoEntityRenderer<PaladinEntity> {

    public PaladinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PaladinModel());
        this.shadowRadius = 0.4F;

        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new PaladinSwordSlashLayer(this));
        addRenderLayer(new PaladinQteHeartAnchorLayer(this));
        addRenderLayer(new PaladinShockwaveLayer(this));
    }


    @Override
    public boolean shouldRender(PaladinEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}