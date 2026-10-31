package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;


@Mod.EventBusSubscriber(modid = "oasiso")
public final class SoulPortalFrames {
    private static final Map<ServerLevel, LinkedHashSet<BlockPos>> PENDING = new IdentityHashMap<>();
    private static boolean updating;

    public static void withoutChecks(Runnable work) {
        boolean previous = updating;
        updating = true;
        try {
            work.run();
        } finally {
            updating = previous;
        }
    }

    public static BlockPos findFrame(ServerLevel level, BlockPos portal, Direction.Axis axis) {
        for (int y = 1; y <= 4; y++)
            for (int x = 1; x <= 3; x++) {
                BlockPos origin = at(portal, axis, -x, -y);
                if (valid(level, origin, axis)) return origin;
            }
        return null;
    }

    public static final int STEP_TICKS = 2;

    private record Frame(BlockPos origin, Direction.Axis axis) {
    }

    private static final Map<ServerLevel, Map<Frame, Integer>> OPENING = new IdentityHashMap<>();

    public static boolean activate(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        if (!valid(level, origin, axis)) return false;
        Frame frame = new Frame(origin.immutable(), axis);
        var openings = OPENING.computeIfAbsent(level, k -> new LinkedHashMap<>());
        if (openings.containsKey(frame)) return true;
        boolean complete = true;
        for (int y = 1; y <= 4; y++)
            for (int x = 1; x <= 3; x++)
                if (!level.getBlockState(at(origin, axis, x, y)).is(ModBlocks.SOUL_PORTAL.get())) complete = false;
        if (complete) return true;
        openings.put(frame, 1);
        withoutChecks(() -> level.setBlock(at(origin, axis, 2, 1), ModBlocks.SOUL_PORTAL.get().defaultBlockState().setValue(SoulPortalBlock.AXIS, axis), 3));
        BlockPos centre = at(origin, axis, 2, 2);
        level.playSound(null, centre, com.benji.oasiso.ModSounds.APOL_SUMMON.get(), net.minecraft.sounds.SoundSource.BLOCKS, .8F, 1F);
        level.sendParticles(com.benji.oasiso.Oasiso.ENT_SOUL.get(), centre.getX() + .5, centre.getY() + .5, centre.getZ() + .5, 24, axis == Direction.Axis.X ? 1.1 : .15, 1.3, axis == Direction.Axis.Z ? 1.1 : .15, .035);
        return true;
    }

    private static void advance(ServerLevel level) {
        var map = OPENING.get(level);
        if (map == null || level.getGameTime() % STEP_TICKS != 0) return;
        for (var it = map.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Frame f = entry.getKey();
            if (!level.hasChunksAt(f.origin, at(f.origin, f.axis, 4, 5))) continue;
            if (!valid(level, f.origin, f.axis)) {
                it.remove();
                PENDING.computeIfAbsent(level, k -> new LinkedHashSet<>()).add(f.origin);
                continue;
            }
            int index = entry.getValue();
            int x = new int[]{2, 1, 3}[index % 3], y = 1 + index / 3;
            BlockPos p = at(f.origin, f.axis, x, y);
            withoutChecks(() -> level.setBlock(p, ModBlocks.SOUL_PORTAL.get().defaultBlockState().setValue(SoulPortalBlock.AXIS, f.axis), 3));
            level.sendParticles(com.benji.oasiso.Oasiso.ENT_SOUL.get(), p.getX() + .5, p.getY() + .5, p.getZ() + .5, 2, .20, .25, .20, .025);
            if (index == 11) it.remove();
            else entry.setValue(index + 1);
        }
        if (map.isEmpty()) OPENING.remove(level);
    }

    private static final Direction.Axis[] AXES = {Direction.Axis.X, Direction.Axis.Z};

    private static boolean material(BlockState s) {
        return s.is(ModBlocks.SOUL_FRAME.get()) || s.is(ModBlocks.SOUL_COLUMN.get()) || s.is(ModBlocks.SOUL_PCORE.get()) || s.is(ModBlocks.SOUL_PORTAL.get());
    }

    @SubscribeEvent
    public static void changed(BlockEvent.NeighborNotifyEvent e) {
        if (updating || !(e.getLevel() instanceof ServerLevel level)) return;
        boolean relevant = material(e.getState());
        for (Direction d : Direction.values()) {
            BlockPos p = e.getPos().relative(d);
            if (level.hasChunkAt(p) && material(level.getBlockState(p))) relevant = true;
        }
        if (relevant) PENDING.computeIfAbsent(level, k -> new LinkedHashSet<>()).add(e.getPos().immutable());
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload e) {
        PENDING.remove(e.getLevel());
        OPENING.remove(e.getLevel());
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        var chunk = e.getChunk();
        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(s -> s.is(ModBlocks.SOUL_PCORE.get()) || s.is(ModBlocks.SOUL_FRAME.get()) || s.is(ModBlocks.SOUL_PORTAL.get())))
                continue;
            int bottom = (chunk.getMinSection() + i) << 4;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState s = section.getBlockState(x, y, z);
                        if (!s.is(ModBlocks.SOUL_PCORE.get()) && !s.is(ModBlocks.SOUL_FRAME.get()) && !s.is(ModBlocks.SOUL_PORTAL.get()))
                            continue;
                        BlockPos p = new BlockPos(chunk.getPos().getMinBlockX() + x, bottom + y, chunk.getPos().getMinBlockZ() + z);
                        PENDING.computeIfAbsent(level, k -> new LinkedHashSet<>()).add(p);
                    }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level)) return;
        advance(level);
        LinkedHashSet<BlockPos> queue = PENDING.get(level);
        if (queue == null) return;
        for (int n = 0; n < 8 && !queue.isEmpty(); n++) {
            Iterator<BlockPos> it = queue.iterator();
            BlockPos changed = it.next();
            it.remove();
            updating = true;
            try {
                update(level, changed);
            } finally {
                updating = false;
            }
        }
        if (queue.isEmpty()) PENDING.remove(level);
    }

    static BlockPos at(BlockPos origin, Direction.Axis axis, int x, int y) {
        return axis == Direction.Axis.X ? origin.offset(x, y, 0) : origin.offset(0, y, x);
    }

    static int role(int x, int y) {
        if ((x == 0 || x == 4) && (y == 0 || y == 5)) return 1;
        if (x == 2 && y == 5) return 3;
        return x == 0 || x == 4 || y == 0 || y == 5 ? 2 : 0;
    }

    private static boolean valid(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        if (!level.hasChunksAt(origin, at(origin, axis, 4, 5))) return false;
        for (int y = 0; y < 6; y++)
            for (int x = 0; x < 5; x++) {
                BlockState s = level.getBlockState(at(origin, axis, x, y));
                int role = role(x, y);
                if (role == 0) {
                    if (!s.isAir() && !(s.is(ModBlocks.SOUL_PORTAL.get()) && s.getValue(SoulPortalBlock.AXIS) == axis))
                        return false;
                } else {
                    Block expected = role == 1 ? ModBlocks.SOUL_FRAME.get() : role == 2 ? ModBlocks.SOUL_COLUMN.get() : ModBlocks.SOUL_PCORE.get();
                    if (!s.is(expected)) return false;
                }
            }
        return true;
    }

    private static boolean belongs(ServerLevel level, BlockPos p, Direction.Axis axis) {
        for (int y = 1; y <= 4; y++)
            for (int x = 1; x <= 3; x++)
                if (valid(level, at(p, axis, -x, -y), axis)) return true;
        return false;
    }

    private static void update(ServerLevel level, BlockPos changed) {
        for (Direction.Axis axis : AXES) {
            for (int y = 0; y < 6; y++)
                for (int x = 0; x < 5; x++) {
                    BlockPos origin = at(changed, axis, -x, -y);
                    if (!valid(level, origin, axis)) continue;
                    activate(level, origin, axis);
                }
            for (int y = -5; y <= 5; y++)
                for (int x = -4; x <= 4; x++) {
                    BlockPos p = at(changed, axis, x, y);
                    if (!level.hasChunkAt(p)) continue;
                    BlockState s = level.getBlockState(p);
                    if (s.is(ModBlocks.SOUL_PORTAL.get()) && s.getValue(SoulPortalBlock.AXIS) == axis) {
                        if (!level.hasChunksAt(at(p, axis, -4, -5), at(p, axis, 4, 5))) continue;
                        if (!belongs(level, p, axis)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
        }
    }
}
