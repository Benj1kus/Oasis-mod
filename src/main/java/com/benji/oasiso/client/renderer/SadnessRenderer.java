package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.SadnessModel;
import com.benji.oasiso.common.entity.SadnessEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SadnessRenderer extends ShrineEmotionRenderer<SadnessEntity> {
    public SadnessRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SadnessModel(), 0x60C8FF);
        this.shadowRadius = 0.3f;

    }

    @Override
    public boolean shouldRender(SadnessEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}