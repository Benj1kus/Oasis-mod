package com.benji.oasiso.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public final class FeralRootsBlock extends Block {
    public FeralRootsBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FeralDrying.DRY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FeralDrying.DRY);
    }
}
