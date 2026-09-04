package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.MemoryCoreBlockEntity;
import com.benji.oasiso.common.block.entity.MemoryPuzzleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

public final class MemoryCoreBlockRenderer implements BlockEntityRenderer<MemoryCoreBlockEntity> {

    private static final ResourceLocation OFF = texture("memory_core_off");
    private static final ResourceLocation ON = texture("memory_core_on");

    public MemoryCoreBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MemoryCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        float now = blockEntity.getLevel().getGameTime() + partialTick;
        float age = blockEntity.getAnimationStart() == Long.MIN_VALUE ? Float.POSITIVE_INFINITY : now - blockEntity.getAnimationStart();

        MemoryPuzzleBlockEntity.VisualAnimation animation = blockEntity.getAnimation();

        float scale = 1.0F;
        float cyan = 0.0F;
        float jumpY = 0.0F;
        float rotationX = 0.0F;
        float rotationY = 0.0F;
        float rotationZ = 0.0F;

        if (animation == MemoryPuzzleBlockEntity.VisualAnimation.REVEAL) {
            scale = MemoryCubeRenderer.bounceScale(age);
            cyan = MemoryCubeRenderer.cyanFlash(age) * 0.72F;
        }

        if (animation == MemoryPuzzleBlockEntity.VisualAnimation.SOLVED) {
            jumpY = MemoryCubeRenderer.solvedJump(age);
            cyan = MemoryCubeRenderer.cyanFlash(age) * 0.28F;

            rotationX = MemoryCubeRenderer.solvedRotation(blockEntity.getBlockPos(), age, 10);
            rotationY = MemoryCubeRenderer.solvedRotation(blockEntity.getBlockPos(), age, 11);
            rotationZ = MemoryCubeRenderer.solvedRotation(blockEntity.getBlockPos(), age, 12);
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D + jumpY, 0.5D);

        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));

        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        MemoryCubeRenderer.renderCube(poseStack, bufferSource, blockEntity.isCoreOn() ? ON : OFF, blockEntity.getLevel(), blockEntity.getBlockPos(), packedLight, packedOverlay, cyan);

        poseStack.popPose();
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/" + name + ".png");
    }
}
