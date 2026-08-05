package com.benji.oasiso.common.block;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.KrombulEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.benji.oasiso.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class EntropyBlock extends Block {
    private static final int SPREAD_RADIUS = 10;
    private static final double ENTROPY_RADIUS = 10.0D;
    private static final double ENTROPY_RADIUS_SQR =
            ENTROPY_RADIUS * ENTROPY_RADIUS;
    private static final double KROMBUL_SPAWN_PLAYER_RADIUS = 30.0D;
    private static final int MAX_NEARBY_KROMBULS = 6;

    private static final double PULL_STRENGTH = 0.1D;

    public EntropyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (!level.isClientSide
                && !state.is(oldState.getBlock())) {

            level.scheduleTick(pos, this, 2);

            if (level instanceof ServerLevel serverLevel) {
                trySpawnKrombul(
                        serverLevel,
                        pos,
                        serverLevel.random
                );
            }
        }
    }

    private void trySpawnKrombul(
            ServerLevel level,
            BlockPos sourcePos,
            RandomSource random
    ) {
        AABB spawnArea = new AABB(sourcePos)
                .inflate(KROMBUL_SPAWN_PLAYER_RADIUS);

        double radiusSqr =
                KROMBUL_SPAWN_PLAYER_RADIUS
                        * KROMBUL_SPAWN_PLAYER_RADIUS;


        boolean hasNearbyPlayer =
                !level.getEntitiesOfClass(
                        Player.class,
                        spawnArea,
                        player ->
                                player.isAlive()
                                        && !player.isSpectator()
                                        && player.distanceToSqr(
                                        Vec3.atCenterOf(sourcePos)
                                ) <= radiusSqr
                ).isEmpty();

        if (!hasNearbyPlayer) {
            return;
        }

        int nearbyKrombulCount =
                level.getEntitiesOfClass(
                        KrombulEntity.class,
                        spawnArea,
                        krombul ->
                                krombul.isAlive()
                                        && krombul.distanceToSqr(
                                        Vec3.atCenterOf(sourcePos)
                                ) <= radiusSqr
                ).size();

        if (nearbyKrombulCount >= MAX_NEARBY_KROMBULS) {
            return;
        }

        if (random.nextFloat() >= 0.40F) {
            return;
        }

        KrombulEntity krombul =
                Oasiso.KROMBUL.get().spawn(
                        level,
                        sourcePos.above(2),
                        MobSpawnType.MOB_SUMMONED
                );

        if (krombul == null) {
            return;
        }

        krombul.setPersistenceRequired();

        level.sendParticles(
                Oasiso.PURPLE_STARS.get(),
                krombul.getX(),
                krombul.getY()
                        + krombul.getBbHeight() * 0.5D,
                krombul.getZ(),
                35,
                0.5D,
                0.7D,
                0.5D,
                0.08D
        );
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {

        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbourPos = pos.relative(direction);

                if (level.getBlockState(neighbourPos).is(Oasiso.ENTROPY_VEIN.get())) {
                    level.scheduleTick(
                            neighbourPos,
                            Oasiso.ENTROPY_VEIN.get(),
                            2 + level.random.nextInt(4)
                    );
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void tick(BlockState state, ServerLevel level,
                     BlockPos sourcePos, RandomSource random) {

        applyEntropyAura(level, sourcePos);

        if (random.nextInt(100) == 0) {
            playRandomEntropySound(level, sourcePos, random);
        }

        if (random.nextInt(5) == 0) {
            for (int i = 0; i < 2; i++) {
                trySpread(level, sourcePos, random);
            }
        }

        level.scheduleTick(sourcePos, this, 2);
    }

    private void playRandomEntropySound(
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        SoundEvent[] sounds = {
                ModSounds.ENTROPY1.get(),
                ModSounds.ENTROPY2.get(),
                ModSounds.ENTROPY3.get()
        };

        SoundEvent selectedSound =
                sounds[random.nextInt(sounds.length)];

        level.playSound(
                null,
                pos,
                selectedSound,
                SoundSource.BLOCKS,
                0.8F,
                0.9F + random.nextFloat() * 0.2F
        );
    }

    private void applyEntropyAura(ServerLevel level, BlockPos sourcePos) {
        Vec3 center = Vec3.atCenterOf(sourcePos);

        AABB area = new AABB(sourcePos)
                .inflate(ENTROPY_RADIUS);

        List<Entity> entities = level.getEntitiesOfClass(
                Entity.class,
                area,
                entity ->
                        entity.isAlive()
                                && !entity.isSpectator()
                                && !(entity instanceof KrombulEntity)
        );

        for (Entity entity : entities) {
            double distanceSqr =
                    entity.position().distanceToSqr(center);

            if (distanceSqr > ENTROPY_RADIUS_SQR) {
                continue;
            }

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(
                        Oasiso.ENTROPY_EFFECT.get(),
                        12,
                        0,
                        false,
                        false,
                        true
                ));
            }

            if (entity.isPassenger()) {
                continue;
            }

            Vec3 direction = center.subtract(entity.position());
            double distance = direction.length();

            if (distance < 0.6D) {
                continue;
            }

            Vec3 pull = direction.normalize()
                    .scale(PULL_STRENGTH);

            entity.addDeltaMovement(new Vec3(
                    pull.x,
                    pull.y * 0.35D,
                    pull.z
            ));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level,
                            BlockPos pos, RandomSource random) {

        if (random.nextInt(6) != 0) {
            return;
        }

        double x = pos.getX() + 0.15D
                + random.nextDouble() * 0.7D;

        double y = pos.getY() + 0.9D
                + random.nextDouble() * 0.35D;

        double z = pos.getZ() + 0.15D
                + random.nextDouble() * 0.7D;

        double velocityX =
                (random.nextDouble() - 0.5D) * 0.01D;

        double velocityY =
                0.005D + random.nextDouble() * 0.01D;

        double velocityZ =
                (random.nextDouble() - 0.5D) * 0.01D;

        level.addParticle(
                Oasiso.PURPLE_STARS.get(),
                x,
                y,
                z,
                velocityX,
                velocityY,
                velocityZ
        );
    }

    private void trySpread(ServerLevel level, BlockPos sourcePos, RandomSource random) {
        List<BlockPos> frontier = collectFrontier(level, sourcePos);

        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos start = frontier.get(random.nextInt(frontier.size()));
            Direction dir = getGrowthDirection(level, sourcePos, start, random);
            BlockPos target = start.relative(dir);

            if (!target.closerThan(sourcePos, SPREAD_RADIUS + 0.5D)) {
                continue;
            }

            if (canPlaceVein(level, target)) {
                level.setBlock(target, Oasiso.ENTROPY_VEIN.get().defaultBlockState()
                        .setValue(EntropyVeinBlock.FACING, dir)
                        .setValue(EntropyVeinBlock.STAGE, 0), 3);
                return;
            }

            BlockState targetState = level.getBlockState(target);
            if (targetState.is(Oasiso.ENTROPY_VEIN.get())
                    && targetState.getValue(EntropyVeinBlock.STAGE) == 0
                    && random.nextInt(3) == 0) {
                level.setBlock(target, targetState.setValue(EntropyVeinBlock.STAGE, 1), 3);
                return;
            }
        }
    }

    private List<BlockPos> collectFrontier(ServerLevel level, BlockPos sourcePos) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(sourcePos);

        BlockPos min = sourcePos.offset(-SPREAD_RADIUS, -2, -SPREAD_RADIUS);
        BlockPos max = sourcePos.offset(SPREAD_RADIUS, 2, SPREAD_RADIUS);

        for (BlockPos scanPos : BlockPos.betweenClosed(min, max)) {
            if (!scanPos.closerThan(sourcePos, SPREAD_RADIUS + 0.5D)) {
                continue;
            }

            BlockState scanState = level.getBlockState(scanPos);
            if (scanState.is(Oasiso.ENTROPY_VEIN.get())
                    && scanState.getValue(EntropyVeinBlock.STAGE) == 1) {
                positions.add(scanPos.immutable());
            }
        }

        return positions;
    }


    private Direction getGrowthDirection(ServerLevel level, BlockPos sourcePos, BlockPos start, RandomSource random) {
        if (start.equals(sourcePos)) {
            return Direction.Plane.HORIZONTAL.getRandomDirection(random);
        }

        Direction base = level.getBlockState(start).getValue(EntropyVeinBlock.FACING);
        int roll = random.nextInt(10);

        if (roll < 6) return base;
        if (roll < 8) return base.getClockWise();
        return base.getCounterClockWise();
    }

    private boolean canPlaceVein(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && EntropyVeinBlock.canExistAt(level, pos);
    }
}