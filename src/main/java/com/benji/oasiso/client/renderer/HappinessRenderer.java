package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.HappinessModel;
import com.benji.oasiso.common.entity.HappinessEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HappinessRenderer extends ShrineEmotionRenderer<HappinessEntity> {
    public HappinessRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HappinessModel(), 0xFFD34A);
        this.shadowRadius = 0.1f;

    }

    @Override
    public boolean shouldRender(HappinessEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}