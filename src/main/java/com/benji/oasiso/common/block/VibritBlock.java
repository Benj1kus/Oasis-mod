package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.VibritBlockEntity;
import com.benji.oasiso.Oasiso;
import com.benji.oasiso.registry.ModBlockEntities;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VibritBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<VibritType> TYPE = EnumProperty.create("type", VibritType.class);

    private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 13.0, 25.0, 13.0);

    public VibritBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, VibritType.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        VibritType randomType = switch (context.getLevel().random.nextInt(3)) {
            case 1 -> VibritType.MID;
            case 2 -> VibritType.SMALL;
            default -> VibritType.NORMAL;
        };

        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(TYPE, randomType);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VibritBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.VIBRIT_BE.get(), VibritBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0 || !level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16.0)) return;
        double height = switch (state.getValue(TYPE)) {
            case NORMAL -> 25.0 / 16.0;
            case MID -> 46.0 / 16.0;
            case SMALL -> 17.0 / 16.0;
        };
        level.addParticle(Oasiso.SOUL_SMOKE.get(),
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.25,
                pos.getY() + height,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.25,
                (random.nextDouble() - 0.5) * 0.012, 0.009 + random.nextDouble() * 0.009,
                (random.nextDouble() - 0.5) * 0.012);
    }

    public enum VibritType implements StringRepresentable {
        NORMAL("normal"), MID("mid"), SMALL("small");

        private final String name;

        VibritType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}