package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class KarakStoneBlock extends Block {
    public static final IntegerProperty SAND_LAYER = IntegerProperty.create("sand_layer", 0, 2);

    public KarakStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SAND_LAYER, 0));
    }

    private static boolean isCover(BlockState state) {
        return state.is(ModBlocks.KR_SAND.get()) || state.is(ModBlocks.KR_GRASS.get());
    }

    private static int layer(BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (isCover(above)) return 1;
        if (above.is(ModBlocks.KR_STONE.get()) && isCover(level.getBlockState(pos.above(2)))) {
            return 2;
        }
        return 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(SAND_LAYER, layer(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Изменение верхнего камня передаст обновление следующему камню вниз.
        return direction == Direction.UP ? state.setValue(SAND_LAYER, layer(level, pos))
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) {
            BlockState corrected = state.setValue(SAND_LAYER, layer(level, pos));
            if (corrected != state) level.setBlock(pos, corrected, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SAND_LAYER);
    }
}
