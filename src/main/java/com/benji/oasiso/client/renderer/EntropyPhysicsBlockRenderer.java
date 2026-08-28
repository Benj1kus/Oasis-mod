package com.benji.oasiso.client.renderer;

import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.benji.oasiso.common.item.EntropyChestplateGloveItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntropyPhysicsBlockRenderer extends EntityRenderer<EntropyPhysicsBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;

    private final MultiBufferSource.BufferSource outlineBuffers = MultiBufferSource.immediate(new BufferBuilder(4096));

    private final MultiBufferSource.BufferSource previewBuffers = MultiBufferSource.immediate(new BufferBuilder(4096));

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

        if (entity.getCarriedBlockState().getRenderShape() != RenderShape.MODEL) {
            renderShapeFallback(entity, entity.getCarriedBlockState(), 0, 0, 0, poseStack, partialTick);
        }

        renderAttachmentPreview(entity, partialTick, poseStack);

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

    private void renderShapeFallback(EntropyPhysicsBlockEntity entity, BlockState state, int offsetX, int offsetY, int offsetZ, PoseStack poseStack, float partialTick) {
        var shape = state.getShape(entity.level(), entity.blockPosition(), net.minecraft.world.phys.shapes.CollisionContext.empty());

        if (shape.isEmpty()) {
            shape = net.minecraft.world.phys.shapes.Shapes.block();
        }

        float time = entity.tickCount + partialTick;

        float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin(time * 0.16F);
        float green = 0.80F + pulse * 0.20F;
        float blue = 0.88F + pulse * 0.12F;
        float alpha = 0.72F + pulse * 0.28F;

        poseStack.pushPose();

        poseStack.translate(offsetX - 0.5D, offsetY - 0.5D, offsetZ - 0.5D);

        RenderType lineType = RenderType.lines();

        VertexConsumer lines = this.outlineBuffers.getBuffer(lineType);

        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(3.7F + pulse * 1.3F);

        LevelRenderer.renderVoxelShape(poseStack, lines, shape, 0.0D, 0.0D, 0.0D, 0.0F, green, blue, alpha, false);

        this.outlineBuffers.endBatch(lineType);

        RenderSystem.lineWidth(1.0F);

        poseStack.popPose();
    }

    private void renderAttachmentPreview(EntropyPhysicsBlockEntity entity, float partialTick, PoseStack poseStack) {
        if (!entity.isNephritisCoated() || !entity.isSettledPhysical()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        LocalPlayer player = minecraft.player;

        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        ItemStack placingStack = findHeldPlaceableBlock(player);

        if (!(placingStack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockState previewState = blockItem.getBlock().defaultBlockState();

        if (!EntropyChestplateGloveItem.canAttachBlockState(entity.level(), entity.blockPosition(), previewState)) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;

        if (!(hitResult instanceof EntityHitResult entityHit) || entityHit.getEntity() != entity) {
            return;
        }

        Vec3 localHit = entityHit.getLocation().subtract(entity.position());

        EntropyPhysicsBlockEntity.AttachmentPreview preview = entity.getAttachmentPreview(entity.level(), localHit);

        if (preview == null) {
            return;
        }

        int ox = preview.targetOffset().getX();
        int oy = preview.targetOffset().getY();
        int oz = preview.targetOffset().getZ();

        float time = entity.tickCount + partialTick;

        float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin(time * 0.38F);
        float fillAlpha = 0.055F + pulse * 0.135F;
        float lineAlpha = 0.52F + pulse * 0.48F;
        float inset = 0.025F + (1.0F - pulse) * 0.018F;

        poseStack.pushPose();

        poseStack.translate(ox, oy, oz);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderType fillType = RenderType.debugFilledBox();

        VertexConsumer fill = this.previewBuffers.getBuffer(fillType);

        LevelRenderer.addChainedFilledBoxVertices(poseStack, fill, -0.5F + inset, -0.5F + inset, -0.5F + inset, 0.5F - inset, 0.5F - inset, 0.5F - inset, 0.00F, 1.00F, 0.94F, fillAlpha);

        this.previewBuffers.endBatch(fillType);

        RenderType lineType = RenderType.lines();
        VertexConsumer lines = this.outlineBuffers.getBuffer(lineType);
        RenderSystem.lineWidth(4.2F + pulse * 2.0F);

        LevelRenderer.renderLineBox(poseStack, lines, new AABB(-0.5D + inset, -0.5D + inset, -0.5D + inset, 0.5D - inset, 0.5D - inset, 0.5D - inset), 0.00F, 1.00F, 1.00F, lineAlpha);

        this.outlineBuffers.endBatch(lineType);

        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static ItemStack findHeldPlaceableBlock(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();

        if (main.getItem() instanceof BlockItem) {
            return main;
        }

        ItemStack off = player.getOffhandItem();

        if (off.getItem() instanceof BlockItem) {
            return off;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getTextureLocation(EntropyPhysicsBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
