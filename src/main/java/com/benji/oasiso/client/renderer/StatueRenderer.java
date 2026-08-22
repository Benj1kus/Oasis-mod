package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.StatueModel;
import com.benji.oasiso.common.block.entity.StatueBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class StatueRenderer extends GeoBlockRenderer<StatueBlockEntity> {

    public StatueRenderer(BlockEntityRendererProvider.Context context) {
        super(new StatueModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        float rotation = -facing.toYRot();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));
    }
}