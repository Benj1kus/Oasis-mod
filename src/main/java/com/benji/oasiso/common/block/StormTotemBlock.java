package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.StormTotemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

public class StormTotemBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = box(3.0D, 0.0D, 3.0D, 13.0D, 26.0D, 13.0D);


    public StormTotemBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return new StormTotemBlockEntity(pos, state);
    }


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {

            return createTickerHelper(type,
                    Oasiso.STORM_TOTEM_BLOCK_ENTITY.get(),
                    StormTotemBlockEntity::clientTick);
        }

        return createTickerHelper(type,
                Oasiso.STORM_TOTEM_BLOCK_ENTITY.get(),
                StormTotemBlockEntity::serverTick);
    }
}