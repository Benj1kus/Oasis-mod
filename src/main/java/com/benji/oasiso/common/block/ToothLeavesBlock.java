package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class ToothLeavesBlock extends Block {
    public static final BooleanProperty FLOWERED = BooleanProperty.create("flowered");
    private static final BooleanProperty INITIALIZED = BooleanProperty.create("initialized");
    private static final int GROW_DELAY = 100;
    private static final int[] LENGTHS = {2, 3, 5};

    public ToothLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FLOWERED, false).setValue(INITIALIZED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOWERED, INITIALIZED);
    }

    private BlockState initialize(BlockState state, RandomSource random) {
        return state.setValue(INITIALIZED, true).setValue(FLOWERED, state.getValue(FLOWERED) || random.nextFloat() < .30F);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return initialize(defaultBlockState(), context.getLevel().getRandom());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level instanceof ServerLevel server && !old.is(this)) {
            if (!state.getValue(INITIALIZED)) server.scheduleTick(pos, this, 1);
            else scheduleGrowth(state, server, pos);
        }
    }

    private void scheduleGrowth(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(FLOWERED) && level.isEmptyBlock(pos.below())) level.scheduleTick(pos, this, GROW_DELAY);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && side == Direction.DOWN && neighbor.isAir()) scheduleGrowth(state, level, pos);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(INITIALIZED);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(INITIALIZED)) tick(state, level, pos, random);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(INITIALIZED)) {
            BlockState rolled = initialize(state, random);
            level.setBlock(pos, rolled, Block.UPDATE_ALL);
            scheduleGrowth(rolled, level, pos);
            return;
        }
        if (!state.getValue(FLOWERED)) return;
        int length = LENGTHS[random.nextInt(LENGTHS.length)];
        for (int i = 1; i <= length; i++) {
            BlockPos next = pos.below(i);
            if (level.isOutsideBuildHeight(next) || !level.isEmptyBlock(next)) break;
            level.setBlockAndUpdate(next, ModBlocks.TOOTH_VINE.get().defaultBlockState());
        }
    }
}
