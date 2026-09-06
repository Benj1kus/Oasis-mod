package com.benji.oasiso.common.item;

import com.benji.oasiso.common.block.SeamlessCurveBannerBlock;
import com.benji.oasiso.common.block.entity.SeamlessCurveBannerBlockEntity;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SeamlessCurveBannerItem extends Item {

    /*
     * Максимальная длина одной ткани.
     *
     * Можно спокойно увеличить позже.
     */
    private static final double MAX_CONNECTION_DISTANCE = 48.0D;

    private static final double MIN_CONNECTION_DISTANCE = 1.0D;

    private static final String TAG_ANCHOR = "SeamlessCurveBannerAnchor";

    public SeamlessCurveBannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();

        /*
         * Если предыдущая попытка почему-то
         * не закончилась корректно, убираем
         * старый временный anchor.
         */
        if (!level.isClientSide) {
            cleanupPreviousAnchor(level, stack);
        }

        BlockPos supportPos = context.getClickedPos();

        Direction face = context.getClickedFace();

        BlockState supportState = level.getBlockState(supportPos);

        if (!supportState.isFaceSturdy(level, supportPos, face)) {
            return InteractionResult.FAIL;
        }

        /*
         * Anchor ставится в воздухе
         * непосредственно перед выбранной стеной.
         */
        BlockPos anchorPos = supportPos.relative(face);

        if (!level.isEmptyBlock(anchorPos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {

            BlockState anchorState = ModBlocks.SEEMLESS_CURVE_BANNER.get().defaultBlockState().setValue(SeamlessCurveBannerBlock.FACING, face);

            boolean placed = level.setBlock(anchorPos, anchorState, Block.UPDATE_ALL);

            if (!placed) {
                return InteractionResult.FAIL;
            }

            player.displayClientMessage(Component.literal("Hold RMB and release on the second surface"), true);
        }

        /*
         * NBT нужен только пока игрок
         * держит ПКМ.
         */
        stack.getOrCreateTag().putLong(TAG_ANCHOR, anchorPos.asLong());

        player.startUsingItem(context.getHand());

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains(TAG_ANCHOR)) {
            return;
        }

        BlockPos anchorPos = BlockPos.of(tag.getLong(TAG_ANCHOR));

        /*
         * Клиенту ничего решать не разрешаем.
         * Финальная связь создаётся только сервером.
         */
        if (level.isClientSide) {
            clearAnchorTag(stack);
            return;
        }

        if (!(livingEntity instanceof Player player)) {

            cancelConnection(level, anchorPos, stack);

            return;
        }

        if (!(level.getBlockEntity(anchorPos) instanceof SeamlessCurveBannerBlockEntity banner)) {

            clearAnchorTag(stack);
            return;
        }

        BlockHitResult hit = rayTraceDestination(level, player);

        if (hit.getType() != HitResult.Type.BLOCK) {

            cancelConnection(level, anchorPos, stack);

            player.displayClientMessage(Component.literal("Banner connection cancelled"), true);

            return;
        }

        BlockPos endSupport = hit.getBlockPos();

        Direction endFace = hit.getDirection();

        BlockState endState = level.getBlockState(endSupport);

        if (!endState.isFaceSturdy(level, endSupport, endFace)) {

            cancelConnection(level, anchorPos, stack);

            return;
        }

        Vec3 startPoint = banner.getStartPoint();

        Vec3 endPoint = attachmentPoint(endSupport, endFace);

        double distance = startPoint.distanceTo(endPoint);

        if (distance < MIN_CONNECTION_DISTANCE || distance > MAX_CONNECTION_DISTANCE) {

            cancelConnection(level, anchorPos, stack);

            player.displayClientMessage(Component.literal("Banner distance must be between 1 and 48 blocks"), true);

            return;
        }

        banner.setConnection(endSupport, endFace);

        /*
         * Только теперь предмет реально тратится.
         *
         * Поэтому неудачная попытка ничего
         * у игрока не забирает.
         */
        clearAnchorTag(stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.displayClientMessage(Component.literal("Banner connected"), true);
    }

    private static BlockHitResult rayTraceDestination(Level level, Player player) {
        Vec3 eye = player.getEyePosition();

        Vec3 end = eye.add(player.getLookAngle().scale(MAX_CONNECTION_DISTANCE));

        return level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    private static Vec3 attachmentPoint(BlockPos supportPos, Direction face) {
        Vec3 center = Vec3.atCenterOf(supportPos);

        return center.add(face.getStepX() * 0.501D, face.getStepY() * 0.501D, face.getStepZ() * 0.501D);
    }

    private static void cancelConnection(Level level, BlockPos anchorPos, ItemStack stack) {
        if (level.getBlockState(anchorPos).is(ModBlocks.SEEMLESS_CURVE_BANNER.get())) {

            /*
             * false:
             * незаконченный anchor ничего не дропает,
             * потому что предмет ещё находится в руке.
             */
            level.removeBlock(anchorPos, false);
        }

        clearAnchorTag(stack);
    }

    private static void cleanupPreviousAnchor(Level level, ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains(TAG_ANCHOR)) {

            return;
        }

        BlockPos oldPos = BlockPos.of(tag.getLong(TAG_ANCHOR));

        if (level.getBlockEntity(oldPos) instanceof SeamlessCurveBannerBlockEntity oldBanner && !oldBanner.isConnected()) {

            level.removeBlock(oldPos, false);
        }

        clearAnchorTag(stack);
    }

    private static void clearAnchorTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(TAG_ANCHOR);

        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }
}