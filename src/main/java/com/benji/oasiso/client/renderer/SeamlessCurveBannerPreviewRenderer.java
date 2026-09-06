package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.SeamlessCurveBannerBlock;
import com.benji.oasiso.common.block.entity.SeamlessCurveBannerBlockEntity;
import com.benji.oasiso.common.item.SeamlessCurveBannerItem;
import com.benji.oasiso.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SeamlessCurveBannerPreviewRenderer {

    private static final String TAG_ANCHOR = "SeamlessCurveBannerAnchor";

    private static final double MISS_PREVIEW_DISTANCE = 12.0D;

    private SeamlessCurveBannerPreviewRenderer() {
    }

    @SubscribeEvent
    public static void renderPreview(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {

            return;
        }
        if (!minecraft.player.isUsingItem()) {
            return;
        }

        ItemStack stack = minecraft.player.getUseItem();
        if (!stack.is(ModItems.SEEMLESS_CURVE_BANNER_ITEM.get())) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ANCHOR)) {

            return;
        }

        BlockPos anchorPos = BlockPos.of(tag.getLong(TAG_ANCHOR));
        if (!(minecraft.level.getBlockEntity(anchorPos) instanceof SeamlessCurveBannerBlockEntity banner)) {

            return;
        }

        Vec3 start = banner.getStartPoint();
        float partialTick = event.getPartialTick();
        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        Vec3 look = minecraft.player.getViewVector(partialTick);
        Vec3 rayEnd = eye.add(look.scale(SeamlessCurveBannerItem.MAX_CONNECTION_DISTANCE));
        BlockHitResult hit = minecraft.level.clip(new ClipContext(eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));

        Vec3 end;
        boolean valid = false;

        if (hit.getType() == HitResult.Type.BLOCK) {

            BlockPos supportPos = hit.getBlockPos();
            Direction face = hit.getDirection();
            end = attachmentPoint(supportPos, face);
            BlockState state = minecraft.level.getBlockState(supportPos);
            double distance = start.distanceTo(end);

            valid = state.isFaceSturdy(minecraft.level, supportPos, face) && distance >= SeamlessCurveBannerItem.MIN_CONNECTION_DISTANCE && distance <= SeamlessCurveBannerItem.MAX_CONNECTION_DISTANCE;

        } else {
            end = eye.add(look.scale(MISS_PREVIEW_DISTANCE));
        }

        Direction startFace = banner.getBlockState().getValue(SeamlessCurveBannerBlock.FACING);

        float pulse = 0.5F + 0.5F * (float) Math.sin((minecraft.level.getGameTime() + partialTick) * 0.32D);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        SeamlessCurveBannerRenderer.renderPreview(minecraft.level, poseStack, bufferSource, start, end, startFace, valid, pulse);
        poseStack.popPose();
        bufferSource.endBatch(SeamlessCurveBannerRenderer.getPreviewRenderType());
    }

    private static Vec3 attachmentPoint(BlockPos supportPos, Direction face) {
        Vec3 center = Vec3.atCenterOf(supportPos);

        return center.add(face.getStepX() * 0.501D, face.getStepY() * 0.501D, face.getStepZ() * 0.501D);
    }
}