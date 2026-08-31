package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.MouthPointBlockEntity;
import com.benji.oasiso.common.waypoint.AzumalitWaypointManager;
import com.benji.oasiso.common.waypoint.MouthPointTeleportServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MouthPointBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 45.0D, 16.0D);

    public MouthPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MouthPointBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockEntity rawBlockEntity = level.getBlockEntity(pos);

        if (!(rawBlockEntity instanceof MouthPointBlockEntity mouthPoint)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (!mouthPoint.isOwnedBy(serverPlayer.getUUID()) || !mouthPoint.hasPartner()) {
            mouthPoint.spawnBlockedClickEffects(serverLevel);
            return InteractionResult.CONSUME;
        }

        if (!MouthPointTeleportServer.begin(serverPlayer, mouthPoint)) {
            mouthPoint.spawnBlockedClickEffects(serverLevel);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity rawBlockEntity = level.getBlockEntity(pos);

            if (!level.isClientSide && level instanceof ServerLevel serverLevel && rawBlockEntity instanceof MouthPointBlockEntity mouthPoint && !mouthPoint.isRemovalCallbackSuppressed()) {
                AzumalitWaypointManager.onWaypointRemoved(serverLevel, mouthPoint);
            }
        }

        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
