package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.CrusaderSkelBlockEntity;
import com.benji.oasiso.registry.ModKarakFluids;

import java.util.function.Supplier;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class CrusaderSkelBlock extends BaseEntityBlock implements LiquidBlockContainer {
    public enum Content implements StringRepresentable {
        EMPTY("empty", () -> Fluids.EMPTY), WATER("water", () -> Fluids.WATER), LAVA("lava", () -> Fluids.LAVA), KARAK("kr_water", ModKarakFluids.KR_WATER), ENTROPY("entropy_water", ModKarakFluids.ENTROPY_WATER), RELAX("relax_water", ModKarakFluids.RELAX_WATER);

        private final String name;
        private final Supplier<? extends Fluid> source;

        Content(String name, Supplier<? extends Fluid> source) {
            this.name = name;
            this.source = source;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static Content of(Fluid fluid) {
            if (fluid == Fluids.EMPTY) return EMPTY;
            for (Content kind : values()) {
                if (kind != EMPTY && kind.source.get().isSame(fluid)) return kind;
            }
            return EMPTY;
        }
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    public static final IntegerProperty PILE = IntegerProperty.create("pile", 1, 4);

    public CrusaderSkelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CONTENT, Content.EMPTY).setValue(PILE, 1).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT, PILE, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var random = context.getLevel().getRandom();
        var fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(CONTENT, Content.of(fluid.getType())).setValue(PILE, 1 + random.nextInt(4)).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from2DDataValue(random.nextInt(4)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(CONTENT).source.get().defaultFluidState();
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.getValue(CONTENT) == Content.EMPTY && Content.of(fluid) != Content.EMPTY;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState incoming) {
        if (!canPlaceLiquid(level, pos, state, incoming.getType())) return false;
        if (!level.isClientSide()) {
            Content content = Content.of(incoming.getType());
            level.setBlock(pos, state.setValue(CONTENT, content), Block.UPDATE_ALL);
            Fluid fluid = content.source.get();
            level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
        }
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusaderSkelBlockEntity(pos, state);
    }
}
