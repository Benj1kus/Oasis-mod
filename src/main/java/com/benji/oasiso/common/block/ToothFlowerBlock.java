package com.benji.oasiso.common.block;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ToothFlowerBlock extends FlowerBlock {

    public ToothFlowerBlock(MobEffect effect, int duration, Properties properties) {
        super(effect, duration, properties);
        registerDefaultState(defaultBlockState().setValue(FeralDrying.DRY, 0));
    }

    private static boolean maybePlaced (BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(ModBlocks.TOOTH_GRASS.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FeralDrying.DRY);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return maybePlaced(state);
    }
}
