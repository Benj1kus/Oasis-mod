package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class KarakStoneBlock extends Block {
    public static final IntegerProperty SAND_LAYER = IntegerProperty.create("sand_layer", 0, 2);
    private static final IntegerProperty[] LINKS = new IntegerProperty[6];

    static {
        for (Direction d : Direction.values()) LINKS[d.ordinal()] = IntegerProperty.create("link_" + d.getName(), 0, 2);
    }

    public KarakStoneBlock(Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any().setValue(SAND_LAYER, 0);
        for (IntegerProperty p : LINKS) s = s.setValue(p, 0);
        registerDefaultState(s);
    }

    private static boolean cover(BlockState s) {
        return s.is(ModBlocks.KR_SAND.get()) || s.is(ModBlocks.KR_GRASS.get());
    }

    private static BlockState calculate(BlockGetter level, BlockPos pos, BlockState s) {
        BlockState above = level.getBlockState(pos.above());
        int layer = cover(above) ? 1 : above.is(ModBlocks.KR_STONE.get()) && cover(level.getBlockState(pos.above(2))) ? 2 : 0;
        s = s.setValue(SAND_LAYER, layer);
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            int value = n.is(ModBlocks.NEPHRITIS_MAGMA.get()) ? 2 : d != Direction.UP && n.is(ModBlocks.KR_SAND.get()) ? 1 : 0;
            s = s.setValue(LINKS[d.ordinal()], value);
        }
        return s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return calculate(c.getLevel(), c.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor level, BlockPos pos, BlockPos np) {
        return calculate(level, pos, s);
    }

    @Override
    public void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(s, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) {
            BlockState next = calculate(level, pos, s);
            if (next != s) level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(SAND_LAYER);
        for (IntegerProperty p : LINKS) b.add(p);
    }

    @Override
    public BlockState rotate(BlockState s, Rotation r) {
        BlockState result = s;
        for (Direction d : Direction.values())
            result = result.setValue(LINKS[r.rotate(d).ordinal()], s.getValue(LINKS[d.ordinal()]));
        return result;
    }

    @Override
    public BlockState mirror(BlockState s, Mirror m) {
        BlockState result = s;
        for (Direction d : Direction.values())
            result = result.setValue(LINKS[m.mirror(d).ordinal()], s.getValue(LINKS[d.ordinal()]));
        return result;
    }
}
