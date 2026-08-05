package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.LieBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.benji.oasiso.common.block.LieBlock;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.model.data.ModelData;

public class LieBlockRenderer implements BlockEntityRenderer<LieBlockEntity> {

    public LieBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LieBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState mimicState = be.getMimicState();
        if (mimicState == null) return;

        float alpha = 1.0f;
        Player player = Minecraft.getInstance().player;

        boolean hasArmor = false;
        if (player != null) {
            int armorCount = 0;
            for (ItemStack armor : player.getArmorSlots()) {
                if (armor.is(Oasiso.SUPER_GOLD_HELMET.get()) ||
                        armor.is(Oasiso.SUPER_GOLD_CHESTPLATE.get()) ||
                        armor.is(Oasiso.SUPER_GOLD_LEGGINGS.get()) ||
                        armor.is(Oasiso.SUPER_GOLD_BOOTS.get())) {
                    armorCount++;
                }
            }
            hasArmor = (armorCount == 4);
        }

        if (be.isPhasing() && !hasArmor) {
            if (player != null) {
                BlockPos fadeOrigin =
                        be.getFadeOriginPos();

                double distance = Math.sqrt(
                        player.distanceToSqr(
                                fadeOrigin.getX() + 0.5D,
                                fadeOrigin.getY() + 0.5D,
                                fadeOrigin.getZ() + 0.5D
                        )
                );

                double multiplier =
                        be.hasNephritisSource()
                                ? LieBlock.NEPHRITIS_RANGE_MULTIPLIER
                                : 1.0D;

                double invisibleDistance =
                        LieBlock.NORMAL_INVISIBLE_DISTANCE
                                * multiplier;

                double fadeDistance =
                        LieBlock.NORMAL_FADE_DISTANCE
                                * multiplier;

                alpha = (float) Mth.clamp(
                        (distance - invisibleDistance)
                                / fadeDistance,
                        0.0D,
                        1.0D
                );
            }
        }

        if (alpha <= 0.01f) return;

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        if (alpha < 0.99f) {
            VertexConsumer translucentBuffer = bufferSource.getBuffer(RenderType.translucent());
            AlphaWrapper alphaBuffer = new AlphaWrapper(translucentBuffer, (int) (alpha * 255));

            var model = dispatcher.getBlockModel(mimicState);
            dispatcher.getModelRenderer().tesselateBlock(be.getLevel(), model, mimicState, be.getBlockPos(), poseStack, alphaBuffer, false, be.getLevel().random, mimicState.getSeed(be.getBlockPos()), packedOverlay, ModelData.EMPTY, RenderType.translucent());
        } else {
            dispatcher.renderSingleBlock(mimicState, poseStack, bufferSource, packedLight, packedOverlay, ModelData.EMPTY, null);
        }
    }

    private static class AlphaWrapper implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int alpha;

        public AlphaWrapper(VertexConsumer delegate, int alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { delegate.color(r, g, b, (a * this.alpha) / 255); return this; }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, (a * this.alpha) / 255); }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}