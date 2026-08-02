package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EntropyVeinBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    private static final int SOURCE_SEARCH_RADIUS = 12;
    private static final int MAX_SEARCHED_VEINS = 4096;

    public EntropyVeinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STAGE, 0));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(STAGE, 0);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && !state.is(oldState.getBlock()) && state.getValue(STAGE) == 0) {
            level.scheduleTick(pos, this, 20 + level.random.nextInt(20));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, false);
            return;
        }
        if (!isConnectedToEntropySource(level, pos)) {
            decay(level, pos, random);
            return;
        }

        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.setValue(STAGE, 1), 3);
        }
    }

    private void decay(ServerLevel level, BlockPos pos, RandomSource random) {
        level.destroyBlock(pos, false);

        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);

            if (level.getBlockState(neighbourPos).is(Oasiso.ENTROPY_VEIN.get())) {
                level.scheduleTick(
                        neighbourPos,
                        this,
                        2 + random.nextInt(4)
                );
            }
        }
    }

    private boolean isConnectedToEntropySource(ServerLevel level, BlockPos startPos) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        BlockPos immutableStart = startPos.immutable();

        queue.add(immutableStart);
        visited.add(immutableStart);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighbourPos = currentPos.relative(direction);

                if (level.getBlockState(neighbourPos).is(Oasiso.ENTROPY_BLOCK.get())) {
                    return true;
                }

                int dx = neighbourPos.getX() - startPos.getX();
                int dy = neighbourPos.getY() - startPos.getY();
                int dz = neighbourPos.getZ() - startPos.getZ();

                if (dx * dx + dy * dy + dz * dz
                        > SOURCE_SEARCH_RADIUS * SOURCE_SEARCH_RADIUS) {
                    continue;
                }

                if (!level.getBlockState(neighbourPos).is(Oasiso.ENTROPY_VEIN.get())) {
                    continue;
                }

                BlockPos immutableNeighbour = neighbourPos.immutable();

                if (visited.add(immutableNeighbour)) {
                    queue.add(immutableNeighbour);
                }

                if (visited.size() >= MAX_SEARCHED_VEINS) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canExistAt(level, pos);
    }

    public static boolean canExistAt(LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (belowState.isAir() || belowState.is(Oasiso.ENTROPY_BLOCK.get())) {
            return false;
        }

        return belowState.isFaceSturdy(level, belowPos, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    public void animateTick(BlockState state, Level level,
                            BlockPos pos, RandomSource random) {

        if (random.nextInt(14) != 0) {
            return;
        }

        double x = pos.getX() + 0.1D
                + random.nextDouble() * 0.8D;

        double y = pos.getY() + 0.08D
                + random.nextDouble() * 0.12D;

        double z = pos.getZ() + 0.1D
                + random.nextDouble() * 0.8D;

        level.addParticle(
                Oasiso.PURPLE_STARS.get(),
                x,
                y,
                z,
                (random.nextDouble() - 0.5D) * 0.006D,
                0.004D + random.nextDouble() * 0.006D,
                (random.nextDouble() - 0.5D) * 0.006D
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }
}