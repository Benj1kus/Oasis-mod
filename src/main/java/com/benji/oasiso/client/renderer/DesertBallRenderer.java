package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.DesertBallModel;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DesertBallRenderer extends GeoEntityRenderer<DesertBallEntity> {
    public DesertBallRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DesertBallModel());
    }
}