package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.DesertBallModel;
import com.benji.oasiso.client.model.SandHandModel;
import com.benji.oasiso.common.entity.SandHandEntity;
import com.benji.oasiso.common.entity.projectile.DesertBallEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SandHandRenderer extends GeoEntityRenderer<SandHandEntity> {
    public SandHandRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SandHandModel());
    }
}