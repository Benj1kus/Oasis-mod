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

    public static boolean activate(ServerLevel level, BlockPos origin, Direction.Axis axis) {
        if (!valid(level, origin, axis)) return false;
        withoutChecks(() -> {
            BlockState portal = ModBlocks.SOUL_PORTAL.get().defaultBlockState().setValue(SoulPortalBlock.AXIS, axis);
            for (int y = 1; y <= 4; y++)
                for (int x = 1; x <= 3; x++)
                    level.setBlock(at(origin, axis, x, y), portal, 3);
        });
        return true;
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
                    BlockState portal = ModBlocks.SOUL_PORTAL.get().defaultBlockState().setValue(SoulPortalBlock.AXIS, axis);
                    for (int iy = 1; iy <= 4; iy++)
                        for (int ix = 1; ix <= 3; ix++) {
                            BlockPos p = at(origin, axis, ix, iy);
                            if (!level.getBlockState(p).equals(portal)) level.setBlock(p, portal, 3);
                        }
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
