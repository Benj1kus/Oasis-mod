package com.benji.oasiso.client.renderer;

import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EntropyPhysicsBlockRenderer extends EntityRenderer<EntropyPhysicsBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;

    private final MultiBufferSource.BufferSource outlineBuffers = MultiBufferSource.immediate(new BufferBuilder(4096));

    public EntropyPhysicsBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntropyPhysicsBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        double wiggle = entity.getPullWiggle(partialTick);
        poseStack.translate(wiggle, 0.5D + Math.abs(wiggle) * 0.25D, -wiggle * 0.65D);

        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getVisualYaw(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVisualPitch(partialTick)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getVisualRoll(partialTick)));

        renderPart(entity, entity.getCarriedBlockState(), 0, 0, 0, poseStack, bufferSource, packedLight);

        for (EntropyPhysicsBlockEntity.StructurePart part : entity.getAttachedParts()) {
            renderPart(entity, part.state(), part.offset().getX(), part.offset().getY(), part.offset().getZ(), poseStack, bufferSource, packedLight);
        }

        renderOutline(entity, entity.getCarriedBlockState(), 0, 0, 0, poseStack);

        for (EntropyPhysicsBlockEntity.StructurePart part : entity.getAttachedParts()) {
            renderOutline(entity, part.state(), part.offset().getX(), part.offset().getY(), part.offset().getZ(), poseStack);
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderPart(EntropyPhysicsBlockEntity entity, BlockState state, int offsetX, int offsetY, int offsetZ, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);

        if (state.getRenderShape() == RenderShape.MODEL) {
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            this.blockRenderer.renderSingleBlock(state, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            return;
        }

        ItemStack stack = new ItemStack(state.getBlock());
        if (!stack.isEmpty()) {
            this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
        }

        poseStack.popPose();
    }

    private void renderOutline(EntropyPhysicsBlockEntity entity, BlockState state, int offsetX, int offsetY, int offsetZ, PoseStack poseStack) {
        VoxelShape shape = state.getShape(entity.level(), entity.blockPosition(), CollisionContext.empty());

        if (shape.isEmpty()) {
            shape = Shapes.block();
        }

        poseStack.pushPose();
        poseStack.translate(offsetX - 0.5D, offsetY - 0.5D, offsetZ - 0.5D);

        RenderType lineType = RenderType.lines();
        VertexConsumer lines = this.outlineBuffers.getBuffer(lineType);

        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(4.5F);

        LevelRenderer.renderVoxelShape(poseStack, lines, shape, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, 1.0F, 1.0F, false);

        this.outlineBuffers.endBatch(lineType);
        RenderSystem.lineWidth(1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntropyPhysicsBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
