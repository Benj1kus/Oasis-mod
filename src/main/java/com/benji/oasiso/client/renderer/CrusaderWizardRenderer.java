package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CrusaderWizardModel;
import com.benji.oasiso.common.entity.CrusaderWizardEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.benji.oasiso.client.layer.CrusaderWizardMagicParticlesLayer;

public class CrusaderWizardRenderer extends GeoEntityRenderer<CrusaderWizardEntity> {
    public CrusaderWizardRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrusaderWizardModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new CrusaderWizardMagicParticlesLayer(this));
    }

    @Override
    public boolean shouldRender(CrusaderWizardEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}