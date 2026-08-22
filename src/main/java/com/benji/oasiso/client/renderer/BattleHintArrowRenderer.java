package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.BattleHintArrowModel;
import com.benji.oasiso.common.entity.BattleHintArrowEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BattleHintArrowRenderer extends GeoEntityRenderer<BattleHintArrowEntity> {
    public BattleHintArrowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BattleHintArrowModel());
        this.shadowRadius = 0.0f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public boolean shouldRender(BattleHintArrowEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}