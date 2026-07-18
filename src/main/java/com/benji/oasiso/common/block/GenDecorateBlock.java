package com.benji.oasiso.common.block;

import com.benji.oasiso.common.entity.CaserEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class GenDecorateBlock extends HorizontalDirectionalBlock {

    private static final VoxelShape VASE_SHAPE = box(2, 0, 2, 14, 17, 14);

    public GenDecorateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VASE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VASE_SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            AABB checkArea = new AABB(pos).move(0, 1.5, 0).inflate(1.5);
            List<CaserEntity> existing = level.getEntitiesOfClass(CaserEntity.class, checkArea);

            if (existing.isEmpty()) {
                CaserEntity caser = com.benji.oasiso.Oasiso.ENTITIES.getEntries().stream()
                        .filter(e -> e.getId().getPath().equals("caser"))
                        .findFirst().map(e -> (CaserEntity) e.get().create(level)).orElse(null);

                if (caser != null) {
                    caser.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

                    caser.setFixedYaw(state.getValue(FACING).toYRot());

                    level.addFreshEntity(caser);

                    ((ServerLevel) level).sendParticles(
                            new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                            pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                            40, 0.4D, 0.5D, 0.4D, 0.1D
                    );
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                AABB checkArea = new AABB(pos).move(0, 1.5, 0).inflate(2.0);
                List<CaserEntity> existing = level.getEntitiesOfClass(CaserEntity.class, checkArea);

                for (CaserEntity caser : existing) {
                    ((ServerLevel) level).sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            caser.getX(), caser.getY() + 1.0D, caser.getZ(),
                            30, 0.4D, 0.6D, 0.4D, 0.05D
                    );
                    caser.discard();
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}