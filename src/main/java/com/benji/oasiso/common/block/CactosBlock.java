package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactosBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = RotatedPillarBlock.AXIS;
    public static final EnumProperty<CactosType> TYPE = EnumProperty.create("type", CactosType.class);

    private static final VoxelShape COLLISION_SHAPE = Block.box(1.0D, 1.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public CactosBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(TYPE, CactosType.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, TYPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockState state = this.defaultBlockState().setValue(AXIS, axis);
        return calculateShape(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (!state.canSurvive(level, currentPos)) {
            level.scheduleTick(currentPos, this, 1);
        }
        return calculateShape(state, level, currentPos);
    }

    private BlockState calculateShape(BlockState state, LevelAccessor level, BlockPos pos) {
        boolean u = level.getBlockState(pos.above()).getBlock() instanceof CactosBlock;
        boolean d = level.getBlockState(pos.below()).getBlock() instanceof CactosBlock;
        boolean n = level.getBlockState(pos.north()).getBlock() instanceof CactosBlock;
        boolean s = level.getBlockState(pos.south()).getBlock() instanceof CactosBlock;
        boolean e = level.getBlockState(pos.east()).getBlock() instanceof CactosBlock;
        boolean w = level.getBlockState(pos.west()).getBlock() instanceof CactosBlock;

        int count = (u?1:0) + (d?1:0) + (n?1:0) + (s?1:0) + (e?1:0) + (w?1:0);

        int countY = (n?1:0) + (e?1:0) + (s?1:0) + (w?1:0);
        int countZ = (u?1:0) + (d?1:0) + (e?1:0) + (w?1:0);
        int countX = (u?1:0) + (d?1:0) + (n?1:0) + (s?1:0);

        if (countZ >= 3) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.CROSS);
        if (countX >= 3) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.CROSS);
        if (countY >= 3) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.CROSS);
        if (count >= 3) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.CROSS);

        if (count == 2) {
            if (n && e) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.CORNER_0);
            if (e && s) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.CORNER_90);
            if (s && w) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.CORNER_180);
            if (w && n) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.CORNER_270);

            if (u && e) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.CORNER_0);
            if (e && d) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.CORNER_90);
            if (d && w) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.CORNER_180);
            if (w && u) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.CORNER_270);

            if (u && s) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.CORNER_0);
            if (s && d) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.CORNER_90);
            if (d && n) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.CORNER_180);
            if (n && u) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.CORNER_270);

            if (u && d) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.STRAIGHT);
            if (n && s) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.STRAIGHT);
            if (e && w) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.STRAIGHT);
        }

        if (count == 1) {
            if (u || d) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, CactosType.STRAIGHT);
            if (n || s) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, CactosType.STRAIGHT);
            if (e || w) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, CactosType.STRAIGHT);
        }

        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.hurt(level.damageSources().cactus(), 5.0F);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int solidCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState adjState = level.getBlockState(pos.relative(dir));
            if (adjState.isSolidRender(level, pos.relative(dir)) && !(adjState.getBlock() instanceof CactosBlock)) {
                solidCount++;
            }
        }
        return solidCount <= 1;
    }

    public enum CactosType implements StringRepresentable {
        STRAIGHT("straight"), CORNER_0("corner_0"), CORNER_90("corner_90"),
        CORNER_180("corner_180"), CORNER_270("corner_270"), CROSS("cross");

        private final String name;
        CactosType(String name) { this.name = name; }
        @Override
        public String getSerializedName() { return this.name; }
    }
}