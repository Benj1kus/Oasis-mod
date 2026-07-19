package com.benji.oasiso.common.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class DirectionalPillarBlock extends DirectionalBlock {

    public DirectionalPillarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.UP)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Логика как у бревен: блок ставится опираясь на ту грань, по которой кликнул игрок
        // Если хочешь чтобы строилось в другую сторону, добавь .getOpposite() в конце
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}