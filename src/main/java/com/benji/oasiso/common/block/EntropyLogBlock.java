package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class EntropyLogBlock extends RotatedPillarBlock {

    public enum LogPart implements StringRepresentable {
        BOTTOM("bottom"), MIDDLE("middle"), TOP("top");

        private final String name;

        LogPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<LogPart> PART = EnumProperty.create("part", LogPart.class);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 8);

    public EntropyLogBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(PART, LogPart.MIDDLE).setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, VARIANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        LevelAccessor level = context.getLevel();

        BlockState belowState = level.getBlockState(pos.below());
        BlockState aboveState = level.getBlockState(pos.above());

        LogPart part;

        boolean isBottom = !belowState.is(this) && belowState.isSolid();
        boolean isTop = !aboveState.is(this);

        if (isBottom) {
            part = LogPart.BOTTOM;
        } else if (isTop) {
            part = LogPart.TOP;
        } else {
            part = LogPart.MIDDLE;
        }

        int variant = context.getLevel().getRandom().nextInt(9);

        return super.getStateForPlacement(context).setValue(PART, part).setValue(VARIANT, variant);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            BlockState belowState = level.getBlockState(currentPos.below());
            BlockState aboveState = level.getBlockState(currentPos.above());

            boolean isBottom = !belowState.is(this) && belowState.isSolid();
            boolean isTop = !aboveState.is(this);

            LogPart part = state.getValue(PART);
            if (isBottom) {
                part = LogPart.BOTTOM;
            } else if (isTop) {
                part = LogPart.TOP;
            } else {
                part = LogPart.MIDDLE;
            }
            return state.setValue(PART, part);
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
}