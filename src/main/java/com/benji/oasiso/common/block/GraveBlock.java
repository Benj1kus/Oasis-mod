package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GraveBlock extends HorizontalDirectionalBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<GraveType> TYPE = EnumProperty.create("type", GraveType.class);

    private static final VoxelShape SHAPE = box(3.5, 0, 3.5, 12.5, 13, 12.5);

    public GraveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, GraveType.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        GraveType randomType = switch (context.getLevel().random.nextInt(3)) {
            case 1 -> GraveType.BIG;
            case 2 -> GraveType.SMALL;
            default -> GraveType.NORMAL;
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !moving && level instanceof ServerLevel server) {
            level.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 2.0F, 0.8F + level.random.nextFloat() * 0.4F);
            for (int i = 0, count = 5 + level.random.nextInt(4); i < count; i++) {
                server.sendParticles(Oasiso.ENT_SOUL.get(),
                        pos.getX() + 0.2 + level.random.nextDouble() * 0.6,
                        pos.getY() + 0.08, pos.getZ() + 0.2 + level.random.nextDouble() * 0.6,
                        0, (level.random.nextDouble() - 0.5) * 0.012,
                        0.018 + level.random.nextDouble() * 0.016,
                        (level.random.nextDouble() - 0.5) * 0.012, 1.0);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    public enum GraveType implements StringRepresentable {
        NORMAL("normal"), BIG("big"), SMALL("small");

        private final String name;

        GraveType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}