package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.CrusaderWarriorModel;
import com.benji.oasiso.common.entity.CrusaderWarriorEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrusaderWarriorRenderer extends GeoEntityRenderer<CrusaderWarriorEntity> {
    public CrusaderWarriorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CrusaderWarriorModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(CrusaderWarriorEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}