package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModKarakFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EPipeBlock extends Block implements LiquidBlockContainer {
    public static final EnumProperty<Direction.Axis> AXIS = RotatedPillarBlock.AXIS;
    public static final EnumProperty<EPipeType> TYPE = EnumProperty.create("type", EPipeType.class);
    public static final EnumProperty<PipeFluid> PIPE_FLUID = EnumProperty.create("pipe_fluid", PipeFluid.class);

    private static final VoxelShape SHAPE_Y = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape SHAPE_X = Block.box(0.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape SHAPE_Z = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 16.0);

    public EPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.STRAIGHT).setValue(PIPE_FLUID, PipeFluid.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, TYPE, PIPE_FLUID);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = state.getValue(AXIS);
        if (axis == Direction.Axis.X) return SHAPE_X;
        if (axis == Direction.Axis.Z) return SHAPE_Z;
        return SHAPE_Y;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        Direction.Axis axis = context.getClickedFace().getAxis();

        PipeFluid fluidType = PipeFluid.NONE;
        Fluid f = fluidState.getType();
        if (f == Fluids.WATER || f == Fluids.FLOWING_WATER) {
            fluidType = PipeFluid.WATER;
        } else if (f == ModKarakFluids.KR_WATER.get() || f == ModKarakFluids.FLOWING_KR_WATER.get()) {
            fluidType = PipeFluid.KR_WATER;
        } else if (f == ModKarakFluids.ENTROPY_WATER.get() || f == ModKarakFluids.FLOWING_ENTROPY_WATER.get()) {
            fluidType = PipeFluid.ENTROPY_WATER;
        }

        BlockState state = this.defaultBlockState().setValue(AXIS, axis).setValue(PIPE_FLUID, fluidType);

        return calculateShape(state, level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(PIPE_FLUID) != PipeFluid.NONE) {
            level.scheduleTick(currentPos, level.getFluidState(currentPos).getType(), level.getFluidState(currentPos).getType().getTickDelay(level));
        }
        if (!state.canSurvive(level, currentPos)) {
            level.scheduleTick(currentPos, this, 1);
        }

        return calculateShape(state, level, currentPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        PipeFluid fluid = state.getValue(PIPE_FLUID);
        if (fluid == PipeFluid.WATER) return Fluids.WATER.getSource(false);
        if (fluid == PipeFluid.KR_WATER) return ModKarakFluids.KR_WATER.get().getSource(false);
        if (fluid == PipeFluid.ENTROPY_WATER) return ModKarakFluids.ENTROPY_WATER.get().getSource(false);
        return super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.getValue(PIPE_FLUID) == PipeFluid.NONE && (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER || fluid == ModKarakFluids.KR_WATER.get() || fluid == ModKarakFluids.FLOWING_KR_WATER.get() || fluid == ModKarakFluids.ENTROPY_WATER.get() || fluid == ModKarakFluids.FLOWING_ENTROPY_WATER.get());
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (state.getValue(PIPE_FLUID) == PipeFluid.NONE && fluidState.isSource()) {
            if (!level.isClientSide()) {
                PipeFluid fluidType = PipeFluid.NONE;
                Fluid f = fluidState.getType();
                if (f == Fluids.WATER || f == Fluids.FLOWING_WATER) {
                    fluidType = PipeFluid.WATER;
                } else if (f == ModKarakFluids.KR_WATER.get() || f == ModKarakFluids.FLOWING_KR_WATER.get()) {
                    fluidType = PipeFluid.KR_WATER;
                } else if (f == ModKarakFluids.ENTROPY_WATER.get() || f == ModKarakFluids.FLOWING_ENTROPY_WATER.get()) {
                    fluidType = PipeFluid.ENTROPY_WATER;
                }

                level.setBlock(pos, state.setValue(PIPE_FLUID, fluidType), 3);
                level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            }
            return true;
        }
        return false;
    }

    private BlockState calculateShape(BlockState state, LevelAccessor level, BlockPos pos) {
        boolean u = level.getBlockState(pos.above()).getBlock() instanceof EPipeBlock;
        boolean d = level.getBlockState(pos.below()).getBlock() instanceof EPipeBlock;
        boolean n = level.getBlockState(pos.north()).getBlock() instanceof EPipeBlock;
        boolean s = level.getBlockState(pos.south()).getBlock() instanceof EPipeBlock;
        boolean e = level.getBlockState(pos.east()).getBlock() instanceof EPipeBlock;
        boolean w = level.getBlockState(pos.west()).getBlock() instanceof EPipeBlock;

        int count = (u ? 1 : 0) + (d ? 1 : 0) + (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);

        int countY = (n ? 1 : 0) + (e ? 1 : 0) + (s ? 1 : 0) + (w ? 1 : 0);
        int countZ = (u ? 1 : 0) + (d ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);
        int countX = (u ? 1 : 0) + (d ? 1 : 0) + (n ? 1 : 0) + (s ? 1 : 0);

        if (countZ >= 3) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.CROSS);
        if (countX >= 3) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.CROSS);
        if (countY >= 3) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.CROSS);
        if (count >= 3) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.CROSS);

        if (count == 2) {
            if (n && e) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.CORNER_0);
            if (e && s) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.CORNER_90);
            if (s && w) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.CORNER_180);
            if (w && n) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.CORNER_270);

            if (u && e) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.CORNER_0);
            if (e && d) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.CORNER_90);
            if (d && w) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.CORNER_180);
            if (w && u) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.CORNER_270);

            if (u && s) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.CORNER_0);
            if (s && d) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.CORNER_90);
            if (d && n) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.CORNER_180);
            if (n && u) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.CORNER_270);

            if (u && d) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.STRAIGHT);
            if (n && s) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.STRAIGHT);
            if (e && w) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.STRAIGHT);
        }

        if (count == 1) {
            if (u || d) return state.setValue(AXIS, Direction.Axis.Y).setValue(TYPE, EPipeType.STRAIGHT);
            if (n || s) return state.setValue(AXIS, Direction.Axis.Z).setValue(TYPE, EPipeType.STRAIGHT);
            if (e || w) return state.setValue(AXIS, Direction.Axis.X).setValue(TYPE, EPipeType.STRAIGHT);
        }

        return state.setValue(TYPE, EPipeType.STRAIGHT);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    public enum PipeFluid implements StringRepresentable {
        NONE("none"), WATER("water"), KR_WATER("kr_water"), ENTROPY_WATER("entropy_water");

        private final String name;

        PipeFluid(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum EPipeType implements StringRepresentable {
        STRAIGHT("straight"), CORNER_0("corner_0"), CORNER_90("corner_90"), CORNER_180("corner_180"), CORNER_270("corner_270"), CROSS("cross");

        private final String name;

        EPipeType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}