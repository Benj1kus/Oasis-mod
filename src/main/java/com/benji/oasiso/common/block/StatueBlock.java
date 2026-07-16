package com.benji.oasiso.common.block;

import com.benji.oasiso.common.block.entity.StatueBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StatueBlock extends Block implements EntityBlock {
    private final VoxelShape shape;

    public StatueBlock(VoxelShape shape, Properties properties) {
        super(properties);
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            double height = this.shape.max(Direction.Axis.Y);
            double y = pos.getY() + random.nextDouble() * height;

            double x = pos.getX() + 0.5D;
            double z = pos.getZ() + 0.5D;

            double radiusX = (this.shape.max(Direction.Axis.X) - this.shape.min(Direction.Axis.X)) / 2.0D;
            double radiusZ = (this.shape.max(Direction.Axis.Z) - this.shape.min(Direction.Axis.Z)) / 2.0D;

            if (random.nextBoolean()) {
                x += random.nextBoolean() ? radiusX : -radiusX;
                z += (random.nextDouble() - 0.5D) * (radiusZ * 2);
            } else {
                z += random.nextBoolean() ? radiusZ : -radiusZ;
                x += (random.nextDouble() - 0.5D) * (radiusX * 2);
            }

            level.addParticle(
                    new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    x, y, z,
                    0.0D, 0.0D, 0.0D
            );
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide ? (lvl, p, st, be) -> {
            if (be instanceof StatueBlockEntity statue) {
                StatueBlockEntity.tick(lvl, p, st, statue);
            }
        } : null;
    }
}