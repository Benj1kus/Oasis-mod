package com.benji.oasiso.common.block.entity;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.GraveBlock;
import com.benji.oasiso.registry.ModBlockEntities;
import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.Set;

public class KrColumnCoreBlockEntity extends BlockEntity {
    public static final int MAX_GRAVES = 10;
    public static final int RADIUS = 15;
    public static final int INTERVAL_TICKS = 20 * 60;
    private static final int MAX_COLUMN_ATTEMPTS = 256;
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private final Set<BlockPos> graves = new LinkedHashSet<>();
    private long nextPulse = -1;

    public KrColumnCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KR_COLUMN_CORE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KrColumnCoreBlockEntity core) {
        if (!(level instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        if (core.nextPulse < 0 || core.nextPulse > now + INTERVAL_TICKS) {
            core.nextPulse = now + INTERVAL_TICKS;
            core.setChanged();
            return;
        }
        if (now < core.nextPulse) return;
        core.nextPulse = now + INTERVAL_TICKS;
        core.refill(server);
        core.setChanged();
    }

    private void refill(ServerLevel level) {
        // An unloaded grave still occupies its slot. Do not force-load neighbouring chunks.
        graves.removeIf(pos -> level.hasChunkAt(pos) && !level.getBlockState(pos).is(ModBlocks.GRAVE.get()));
        for (int attempt = 0; graves.size() < MAX_GRAVES && attempt < MAX_COLUMN_ATTEMPTS; attempt++) {
            int dx = level.random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int dz = level.random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int horizontal = dx * dx + dz * dz;
            if (horizontal > RADIUS * RADIUS) continue;
            BlockPos chosen = null;
            int choices = 0;
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                if (horizontal + dy * dy > RADIUS * RADIUS) continue;
                BlockPos candidate = worldPosition.offset(dx, dy, dz);
                if (candidate.equals(worldPosition) || graves.contains(candidate) || !canPlace(level, candidate))
                    continue;
                if (level.random.nextInt(++choices) == 0) chosen = candidate;
            }
            if (chosen == null) continue;
            BlockState grave = ModBlocks.GRAVE.get().defaultBlockState().setValue(GraveBlock.FACING, FACINGS[level.random.nextInt(FACINGS.length)]).setValue(GraveBlock.TYPE, GraveBlock.GraveType.values()[level.random.nextInt(3)]);
            if (level.setBlock(chosen, grave, Block.UPDATE_ALL) && level.getBlockState(chosen).is(ModBlocks.GRAVE.get())) {
                graves.add(chosen.immutable());
                souls(level, chosen);
            }
        }
    }

    private static boolean canPlace(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(below) || !level.hasChunkAt(pos) || !level.hasChunkAt(below) || !level.getWorldBorder().isWithinBounds(pos))
            return false;
        if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) return false;
        BlockState support = level.getBlockState(below);
        if (!support.isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(below).isEmpty()) return false;
        if (support.is(ModBlocks.GRAVE.get()) || support.is(ModBlocks.KR_COLUMN_CORE.get())) return false;
        return level.getEntities((Entity) null, new AABB(pos), entity -> !entity.isSpectator()).isEmpty();
    }

    private static void souls(ServerLevel level, BlockPos pos) {
        for (int i = 0, count = 5 + level.random.nextInt(4); i < count; i++) {
            level.sendParticles(Oasiso.ENT_SOUL.get(), pos.getX() + .1 + level.random.nextDouble() * .8, pos.getY() + .08, pos.getZ() + .1 + level.random.nextDouble() * .8, 0, (level.random.nextDouble() - .5) * .012, .018 + level.random.nextDouble() * .016, (level.random.nextDouble() - .5) * .012, 1.0);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("NextGravePulse", nextPulse);
        tag.putLongArray("OwnedGraves", graves.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nextPulse = tag.contains("NextGravePulse") ? tag.getLong("NextGravePulse") : -1;
        graves.clear();
        for (long packed : tag.getLongArray("OwnedGraves")) {
            BlockPos pos = BlockPos.of(packed);
            if (pos.distSqr(worldPosition) <= RADIUS * RADIUS && !pos.equals(worldPosition)) graves.add(pos);
            if (graves.size() >= MAX_GRAVES) break;
        }
    }
}
