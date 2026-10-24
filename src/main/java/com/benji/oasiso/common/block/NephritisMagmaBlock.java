package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.NephritisMagmaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class NephritisMagmaBlock extends BaseEntityBlock {
    public static final BooleanProperty LEAKING = BooleanProperty.create("leaking");
    public static final float LEAK_CHANCE = .30F;

    public NephritisMagmaBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEAKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEAKING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LEAKING, !context.getLevel().isClientSide && context.getLevel().random.nextFloat() < LEAK_CHANCE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NephritisMagmaBlockEntity(pos, state);
    }
}
