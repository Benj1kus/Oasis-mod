package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.HashMap;
import java.util.Map;

public final class EntropySoilBlock extends Block {

    public static final IntegerProperty SAND_LAYER = IntegerProperty.create("sand_layer", 0, 5);
    public static final DirectionProperty SAND_FROM = DirectionProperty.create("sand_from");
    public static final DirectionProperty SAND_CORNER = DirectionProperty.create("sand_corner");
    private static final Direction[] ORDER = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN};

    public EntropySoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SAND_LAYER, 0).setValue(SAND_FROM, Direction.UP).setValue(SAND_CORNER, Direction.EAST));
    }

    private static boolean isSand(BlockState state) {
        return state.is(ModBlocks.KR_SAND.get()) || state.is(ModBlocks.KR_GRASS.get());
    }

    private record Cover(int layer, Direction from) {
    }

    private Cover baseCover(BlockGetter level, BlockPos pos) {
        int contacts = 0;
        Direction from = Direction.UP;
        for (Direction direction : ORDER) {
            if (isSand(level.getBlockState(pos.relative(direction)))) {
                from = direction;
                if (++contacts >= 3) return new Cover(3, Direction.UP);
            }
        }
        if (contacts >= 1) {
            boolean continues = level.getBlockState(pos.relative(from.getOpposite())).is(this);
            return new Cover(continues ? 1 : 4, from);
        }
        for (Direction direction : ORDER) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).is(this) && isSand(level.getBlockState(neighbor.relative(direction)))) {
                return new Cover(2, direction);
            }
        }
        return new Cover(0, Direction.UP);
    }

    private boolean supportsCorner(BlockGetter level, BlockPos pos, Direction side, Direction otherSide, boolean strongOnly) {
        BlockPos neighbor = pos.relative(side);
        BlockState neighborState = level.getBlockState(neighbor);
        if (isSand(neighborState)) return true;
        if (!neighborState.is(this)) return false;
        Cover cover = baseCover(level, neighbor);
        if (cover.layer() == 3) return true;
        if (strongOnly || cover.layer() == 0) return false;
        if (cover.from() == otherSide) return true;
        return (cover.layer() == 1 || cover.layer() == 4) && cover.from() == side.getOpposite();
    }

    private BlockState primaryState(BlockState state, BlockGetter level, BlockPos pos) {
        Cover base = baseCover(level, pos);
        if (base.layer() != 3) {
            for (int i = 0; i < ORDER.length; i++) {
                for (int j = i + 1; j < ORDER.length; j++) {
                    Direction a = ORDER[i];
                    Direction b = ORDER[j];

                    if (a.getAxis() == b.getAxis()) continue;
                    boolean strongOnly = base.layer() != 0;

                    if (supportsCorner(level, pos, a, b, strongOnly) && supportsCorner(level, pos, b, a, strongOnly)) {
                        return state.setValue(SAND_LAYER, 5).setValue(SAND_FROM, a).setValue(SAND_CORNER, b);
                    }
                }
            }
        }

        return state.setValue(SAND_LAYER, base.layer()).setValue(SAND_FROM, base.from()).setValue(SAND_CORNER, Direction.EAST);
    }

    private BlockState primaryAt(BlockGetter level, BlockPos pos, Map<BlockPos, BlockState> cache) {
        return cache.computeIfAbsent(pos.immutable(), key -> primaryState(defaultBlockState(), level, key));
    }

    private boolean shortenedByCorner(BlockGetter level, BlockPos pos, BlockState own, Map<BlockPos, BlockState> cache) {
        if (own.getValue(SAND_LAYER) != 1) return false;
        Direction from = own.getValue(SAND_FROM);
        for (Direction side : ORDER) {
            if (side.getAxis() == from.getAxis()) continue;
            BlockPos neighbor = pos.relative(side);
            if (!level.getBlockState(neighbor).is(this)) continue;
            BlockState other = primaryAt(level, neighbor, cache);
            if (other.getValue(SAND_LAYER) == 5 && (other.getValue(SAND_FROM) == from || other.getValue(SAND_CORNER) == from))
                return true;
        }
        return false;
    }

    private BlockState resolve(BlockState state, BlockGetter level, BlockPos pos) {
        Map<BlockPos, BlockState> cache = new HashMap<>();
        BlockState own = primaryState(state, level, pos);
        cache.put(pos.immutable(), own);
        int layer = own.getValue(SAND_LAYER);

        if (layer == 3 || layer == 5 || layer == 4) return own;

        if (layer == 1) {
            return shortenedByCorner(level, pos, own, cache) ? own.setValue(SAND_LAYER, 4) : own;
        }

        for (Direction side : ORDER) {
            BlockPos neighbor = pos.relative(side);
            if (!level.getBlockState(neighbor).is(this)) continue;
            BlockState other = primaryAt(level, neighbor, cache);
            if (other.getValue(SAND_LAYER) == 5 && (other.getValue(SAND_FROM) == side || other.getValue(SAND_CORNER) == side)) {
                return own.setValue(SAND_LAYER, 0).setValue(SAND_FROM, Direction.UP).setValue(SAND_CORNER, Direction.EAST);
            }
        }

        // partsand
        if (layer == 2) {
            for (Direction side : ORDER) {
                BlockPos neighbor = pos.relative(side);
                if (!level.getBlockState(neighbor).is(this) || !isSand(level.getBlockState(neighbor.relative(side))))
                    continue;
                BlockState other = primaryAt(level, neighbor, cache);
                if (other.getValue(SAND_LAYER) == 1 && other.getValue(SAND_FROM) == side && !shortenedByCorner(level, neighbor, other, cache)) {
                    return own.setValue(SAND_FROM, side);
                }
            }
            return own.setValue(SAND_LAYER, 0).setValue(SAND_FROM, Direction.UP).setValue(SAND_CORNER, Direction.EAST);
        }
        return own;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return resolve(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return resolve(state, level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && !oldState.is(this)) {
            BlockState corrected = resolve(state, level, pos);
            if (corrected != state) level.setBlock(pos, corrected, Block.UPDATE_ALL);
            scheduleLocal(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving);
        if (!level.isClientSide) scheduleLocal(level, pos);
    }

    private void scheduleLocal(Level level, BlockPos pos) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 4) continue;
                    BlockPos next = pos.offset(dx, dy, dz);
                    if (level.hasChunkAt(next) && level.getBlockState(next).is(this)) {
                        level.scheduleTick(next, this, 1);
                    }
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState corrected = resolve(state, level, pos);
        if (corrected != state) level.setBlock(pos, corrected, Block.UPDATE_ALL);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SAND_FROM, rotation.rotate(state.getValue(SAND_FROM))).setValue(SAND_CORNER, rotation.rotate(state.getValue(SAND_CORNER)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction from = state.getValue(SAND_FROM);
        Direction corner = state.getValue(SAND_CORNER);
        return state.setValue(SAND_FROM, mirror.getRotation(from).rotate(from)).setValue(SAND_CORNER, mirror.getRotation(corner).rotate(corner));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SAND_LAYER, SAND_FROM, SAND_CORNER);
    }
}
