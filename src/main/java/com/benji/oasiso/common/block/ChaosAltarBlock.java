package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.ChaosAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ChaosAltarBlock extends BaseEntityBlock {


    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D);

    public ChaosAltarBlock(Properties properties) {
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
        return new ChaosAltarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);


        if (blockEntity instanceof ChaosAltarBlockEntity altar && altar.isBlockedByPaladin()) {

            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                altar.spawnBlockedClickEffects(serverLevel);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        ItemStack heldStack = player.getItemInHand(hand);
        ChaosAltarBlockEntity.ActivationType activationType;


        if (heldStack.is(Oasiso.ORB_CHAOS.get())) {

            activationType = ChaosAltarBlockEntity.ActivationType.CHAOS;

        } else if (heldStack.is(Oasiso.ORB_DOMINATION.get())) {

            activationType = ChaosAltarBlockEntity.ActivationType.DOMINANCE;

        } else {
            return InteractionResult.PASS;
        }


        if (!level.isClientSide && blockEntity instanceof ChaosAltarBlockEntity altar) {

            boolean activated = altar.activate(activationType);

            if (activated && !player.getAbilities().instabuild) {
                heldStack.shrink(1);
            }
        }


        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(type, Oasiso.CHAOS_ALTAR_BE.get(), ChaosAltarBlockEntity::serverTick);
    }
}