package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class KarakStoneBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty SAND_LAYER = IntegerProperty.create("sand_layer", 0, 2);
    public static final BooleanProperty HORIZ = BooleanProperty.create("horiz");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public KarakStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(SAND_LAYER, 0)
                .setValue(HORIZ, false)
                .setValue(FACING, Direction.NORTH));
    }

    private static boolean isCover(BlockState state) {
        return state.is(ModBlocks.KR_SAND.get()) || state.is(ModBlocks.KR_GRASS.get());
    }

    private static BlockState calculateState(BlockGetter level, BlockPos pos, BlockState currentState) {
        BlockState above = level.getBlockState(pos.above());
        int sandLayer = 0;
        if (isCover(above)) {
            sandLayer = 1;
        } else if (above.is(ModBlocks.KR_STONE.get()) && isCover(level.getBlockState(pos.above(2)))) {
            sandLayer = 2;
        }

        if (sandLayer > 0) {
            return currentState.setValue(SAND_LAYER, sandLayer)
                    .setValue(HORIZ, false)
                    .setValue(FACING, Direction.NORTH);
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (isCover(neighbor)) {
                return currentState.setValue(SAND_LAYER, 0)
                        .setValue(HORIZ, true)
                        .setValue(FACING, dir);
            }
        }

        return currentState.setValue(SAND_LAYER, 0)
                .setValue(HORIZ, false)
                .setValue(FACING, Direction.NORTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return calculateState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal() || direction == Direction.UP) {
            return calculateState(level, pos, state);
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) {
            BlockState corrected = calculateState(level, pos, state);
            if (corrected != state) {
                level.setBlock(pos, corrected, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SAND_LAYER, HORIZ, FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}