package com.benji.oasiso.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

public class ProtoBlock extends HorizontalDirectionalBlock {

    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 16, 13);
    private static final double PLAYER_RANGE = 8.0;
    private static final double DUST_RADIUS = 5.0;
    private static final int DUST_INTERVAL = 3;

    private static final Map<Level, Long> LAST_DUST_TICK = new WeakHashMap<>();

    public ProtoBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FeralDrying.DRY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FeralDrying.DRY, FACING);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        long now = level.getGameTime();
        Long last = LAST_DUST_TICK.get(level);
        if (last != null && now - last < DUST_INTERVAL) return;

        if (level.getNearestPlayer(cx, cy, cz, PLAYER_RANGE, false) == null) return;

        for (int attempt = 0; attempt < 6; attempt++) {
            double dx = (random.nextDouble() * 2 - 1) * DUST_RADIUS;
            double dy = (random.nextDouble() * 2 - 1) * DUST_RADIUS;
            double dz = (random.nextDouble() * 2 - 1) * DUST_RADIUS;

            if (dx * dx + dy * dy + dz * dz > DUST_RADIUS * DUST_RADIUS) continue;

            double x = cx + dx;
            double y = cy + dy;
            double z = cz + dz;
            if (!level.isEmptyBlock(BlockPos.containing(x, y, z))) continue;

            level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.0, 0.0);
            LAST_DUST_TICK.put(level, now);
            break;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
