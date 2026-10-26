package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.AngerModel;
import com.benji.oasiso.common.entity.AngerEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AngerRenderer extends EmotionOutlineRenderer<AngerEntity> {
    public AngerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AngerModel(), 0xFF354B);
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(AngerEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}