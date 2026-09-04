package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.MemoryPuzzleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

public final class MemoryPuzzleBlockRenderer implements BlockEntityRenderer<MemoryPuzzleBlockEntity> {

    private static final ResourceLocation UNKNOWN = texture("memory_unknown");

    public MemoryPuzzleBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MemoryPuzzleBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        float now = blockEntity.getLevel().getGameTime() + partialTick;
        float age = blockEntity.getAnimationStart() == Long.MIN_VALUE ? Float.POSITIVE_INFINITY : now - blockEntity.getAnimationStart();

        MemoryPuzzleBlockEntity.VisualAnimation animation = blockEntity.getAnimation();
        ResourceLocation texture = resolveTexture(blockEntity, animation, age);

        float scale = 1.0F;
        float cyan = 0.0F;
        float shakeX = 0.0F;
        float jumpY = 0.0F;
        float pushX = 0.0F;
        float pushY = 0.0F;
        float pushZ = 0.0F;

        float rotationX = 0.0F;
        float rotationY = 0.0F;
        float rotationZ = 0.0F;

        switch (animation) {
            case REVEAL, HIDE -> {
                scale = MemoryCubeRenderer.bounceScale(age);
                cyan = MemoryCubeRenderer.cyanFlash(age);
            }

            case SELECT -> {
                scale = MemoryCubeRenderer.bounceScale(age);
                cyan = MemoryCubeRenderer.cyanFlash(age) * 0.28F;
            }

            case MATCH -> {
                scale = MemoryCubeRenderer.bounceScale(age);
                cyan = MemoryCubeRenderer.cyanFlash(age) * 0.45F;
            }

            case MISMATCH -> {
                shakeX = MemoryCubeRenderer.mismatchShake(age);
                cyan = MemoryCubeRenderer.cyanFlash(age) * 0.20F;
            }

            case SOLVED -> {
                float push = MemoryCubeRenderer.solvedPush(age);

                Direction direction = blockEntity.getSolvedPushDirection();

                pushX = direction.getStepX() * push;
                pushY = direction.getStepY() * push;
                pushZ = direction.getStepZ() * push;

                float jumpAge = blockEntity.getSolvedJumpStart() == Long.MIN_VALUE ? Float.POSITIVE_INFINITY : now - blockEntity.getSolvedJumpStart();

                jumpY = MemoryCubeRenderer.solvedJump(jumpAge);

                cyan = MemoryCubeRenderer.cyanFlash(age) * 0.42F;
                scale = 1.0F + MemoryCubeRenderer.cyanFlash(age) * 0.035F;

                rotationX = MemoryCubeRenderer.solvedRotation(blockEntity.getBlockPos(), jumpAge, 0);
                rotationY = MemoryCubeRenderer.solvedRotation(blockEntity.getBlockPos(), jumpAge, 1);
                rotationZ = MemoryCubeRenderer.solvedRotation(blockEntity.getBlockPos(), jumpAge, 2);
            }

            default -> {
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.5D + shakeX + pushX, 0.5D + jumpY + pushY, 0.5D + pushZ);

        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotationZ));

        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        MemoryCubeRenderer.renderCube(poseStack, bufferSource, texture, blockEntity.getLevel(), blockEntity.getBlockPos(), packedLight, packedOverlay, cyan);
        poseStack.popPose();
    }

    private static ResourceLocation resolveTexture(MemoryPuzzleBlockEntity blockEntity, MemoryPuzzleBlockEntity.VisualAnimation animation, float age) {
        ResourceLocation symbol = texture(blockEntity.getSymbolTextureName());

        if (blockEntity.isMatched()) {
            return symbol;
        }

        return switch (animation) {
            case REVEAL -> age >= 3.0F ? symbol : UNKNOWN;
            case HIDE -> age < 3.0F ? symbol : UNKNOWN;
            case SELECT, MATCH, MISMATCH, SOLVED -> symbol;

            default -> blockEntity.isVisible() ? symbol : UNKNOWN;
        };
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/block/" + name + ".png");
    }
}
