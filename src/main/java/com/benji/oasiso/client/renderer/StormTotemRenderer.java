package com.benji.oasiso.client.renderer;

import com.benji.oasiso.common.block.entity.StormTotemBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

public class StormTotemRenderer implements BlockEntityRenderer<StormTotemBlockEntity> {

    private static final AABB OUTLINE = new AABB(3.0D / 16.0D, 0.0D, 3.0D / 16.0D, 13.0D / 16.0D, 26.0D / 16.0D, 13.0D / 16.0D).inflate(0.025D);

    public StormTotemRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StormTotemBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float intensity = blockEntity.getStormIntensity();

        long gameTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime();

        float pulse = 0.72F + (float) Math.sin((gameTime + partialTick) * 0.16F) * 0.18F;
        float alpha = 0.55F + intensity * 0.35F;

        alpha *= pulse;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer, OUTLINE, 1.0F, 0.72F, 0.12F, alpha);
    }


    @Override
    public boolean shouldRenderOffScreen(StormTotemBlockEntity blockEntity) {
        return true;
    }


    @Override
    public int getViewDistance() {
        return 48;
    }
}