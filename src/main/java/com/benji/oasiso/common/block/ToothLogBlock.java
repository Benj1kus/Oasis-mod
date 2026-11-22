package com.benji.oasiso.common.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class ToothLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty LEAFY = BooleanProperty.create("leafy");

    private final Supplier<? extends Block> leaves;

    public ToothLogBlock(Properties properties, Supplier<? extends Block> leaves) {
        super(properties);
        this.leaves = leaves;
        registerDefaultState(defaultBlockState().setValue(LEAFY, false).setValue(FeralDrying.DRY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FeralDrying.DRY, LEAFY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
        return super.getStateForPlacement(context).setValue(LEAFY, above.is(leaves.get()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return direction == Direction.UP ? updated.setValue(LEAFY, neighbor.is(leaves.get())) : updated;
    }
}
