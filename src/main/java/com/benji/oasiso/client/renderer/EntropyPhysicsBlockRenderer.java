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
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

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

        if (entity.getCarriedBlockState().getRenderShape() != RenderShape.MODEL) {
            renderShapeFallback(entity, entity.getCarriedBlockState(), 0, 0, 0, poseStack, partialTick);
        }

        renderDetachmentPreview(entity, partialTick, poseStack);
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

        EntropyPhysicsBlockEntity.AttachmentPreview preview = entity.getAttachmentPreview(entity.level(), player);
        if (preview == null) {
            return;
        }

        int ox = preview.targetOffset().getX();
        int oy = preview.targetOffset().getY();
        int oz = preview.targetOffset().getZ();

        float time = entity.tickCount + partialTick;
        float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin(time * 0.38F);
        float fillAlpha = 0.035F + pulse * 0.065F;
        float lineAlpha = 0.60F + pulse * 0.40F;
        float inset = 0.025F + (1.0F - pulse) * 0.018F;
        float edgeThickness = 0.018F + pulse * 0.010F;

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);

        renderDepthTestedPreviewBox(poseStack, -0.5F + inset, -0.5F + inset, -0.5F + inset, 0.5F - inset, 0.5F - inset, 0.5F - inset, edgeThickness, 0.00F, 1.00F, 0.94F, fillAlpha, 0.00F, 1.00F, 1.00F, lineAlpha);
        poseStack.popPose();
    }

    private void renderDetachmentPreview(EntropyPhysicsBlockEntity entity, float partialTick, PoseStack poseStack) {
        if (!entity.isNephritisCoated() || !entity.isSettledPhysical()) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        LocalPlayer player = minecraft.player;

        if (player == null || !player.isShiftKeyDown()) {
            return;
        }

        ItemStack glove = findFreeGlove(player);

        if (glove.isEmpty()) {
            return;
        }

        HitResult hitResult = minecraft.hitResult;

        if (!(hitResult instanceof EntityHitResult entityHit) || entityHit.getEntity() != entity) {
            return;
        }

        EntropyPhysicsBlockEntity.DetachmentPreview preview = entity.getDetachmentPreview(player);

        if (preview == null) {
            return;
        }

        int ox = preview.sourceOffset().getX();
        int oy = preview.sourceOffset().getY();
        int oz = preview.sourceOffset().getZ();

        float time = entity.tickCount + partialTick;


        float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin(time * 0.42F);
        float fillAlpha = 0.045F + pulse * 0.070F;
        float lineAlpha = 0.66F + pulse * 0.34F;
        float inset = 0.020F + (1.0F - pulse) * 0.012F;
        float edgeThickness = 0.020F + pulse * 0.011F;

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);

        renderDepthTestedPreviewBox(poseStack, -0.5F + inset, -0.5F + inset, -0.5F + inset, 0.5F - inset, 0.5F - inset, 0.5F - inset, edgeThickness, 0.08F, 1.00F, 0.62F, fillAlpha, 0.00F, 1.00F, 0.72F, lineAlpha);
        poseStack.popPose();
    }

    private static ItemStack findFreeGlove(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof EntropyChestplateGloveItem && !EntropyChestplateGloveItem.hasHeldBlock(main)) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof EntropyChestplateGloveItem && !EntropyChestplateGloveItem.hasHeldBlock(off)) {
            return off;
        }

        return ItemStack.EMPTY;
    }


    private static void renderDepthTestedPreviewBox(PoseStack poseStack, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float edgeThickness, float fillR, float fillG, float fillB, float fillA, float edgeR, float edgeG, float edgeB, float edgeA) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();

        addSolidBox(builder, matrix, minX, minY, minZ, maxX, maxY, maxZ, fillR, fillG, fillB, fillA);
        addBoxEdges(builder, matrix, minX, minY, minZ, maxX, maxY, maxZ, edgeThickness, edgeR, edgeG, edgeB, edgeA);


        BufferUploader.drawWithShader(builder.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
    }

    private static void addBoxEdges(BufferBuilder builder, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float thickness, float r, float g, float b, float a) {
        float half = thickness * 0.5F;

        addSolidBox(builder, matrix, minX, minY - half, minZ - half, maxX, minY + half, minZ + half, r, g, b, a);
        addSolidBox(builder, matrix, minX, minY - half, maxZ - half, maxX, minY + half, maxZ + half, r, g, b, a);
        addSolidBox(builder, matrix, minX, maxY - half, minZ - half, maxX, maxY + half, minZ + half, r, g, b, a);
        addSolidBox(builder, matrix, minX, maxY - half, maxZ - half, maxX, maxY + half, maxZ + half, r, g, b, a);
        addSolidBox(builder, matrix, minX - half, minY, minZ - half, minX + half, maxY, minZ + half, r, g, b, a);
        addSolidBox(builder, matrix, maxX - half, minY, minZ - half, maxX + half, maxY, minZ + half, r, g, b, a);
        addSolidBox(builder, matrix, minX - half, minY, maxZ - half, minX + half, maxY, maxZ + half, r, g, b, a);
        addSolidBox(builder, matrix, maxX - half, minY, maxZ - half, maxX + half, maxY, maxZ + half, r, g, b, a);
        addSolidBox(builder, matrix, minX - half, minY - half, minZ, minX + half, minY + half, maxZ, r, g, b, a);
        addSolidBox(builder, matrix, maxX - half, minY - half, minZ, maxX + half, minY + half, maxZ, r, g, b, a);
        addSolidBox(builder, matrix, minX - half, maxY - half, minZ, minX + half, maxY + half, maxZ, r, g, b, a);
        addSolidBox(builder, matrix, maxX - half, maxY - half, minZ, maxX + half, maxY + half, maxZ, r, g, b, a);
    }

    private static void addSolidBox(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {

        addQuad(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, a);

        addQuad(builder, matrix, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, r, g, b, a);
        addQuad(builder, matrix, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, r, g, b, a);
        addQuad(builder, matrix, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        addQuad(builder, matrix, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, a);
        addQuad(builder, matrix, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, r, g, b, a);
    }

    private static void addQuad(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
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
