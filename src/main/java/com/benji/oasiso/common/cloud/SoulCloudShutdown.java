package com.benji.oasiso.common.cloud;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.SoulCloudBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;


@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class SoulCloudShutdown extends SavedData {
    public static final int RADIUS = 50;
    public static final double BLOCKS_PER_TICK = 1.5;
    public static final int CHANGE_BUDGET = 2048;
    private static final String SAVE_NAME = "oasiso_soul_cloud_shutdown";
    private final List<Wave> waves = new ArrayList<>();

    private static final class Wave {
        final Vec3 origin;
        final long started;
        final Set<Long> pendingChunks = new HashSet<>();
        final Set<Long> queued = new HashSet<>();
        final PriorityQueue<Long> blocks;

        Wave(Vec3 origin, long started) {
            this.origin = origin;
            this.started = started;
            blocks = new PriorityQueue<>(Comparator.<Long>comparingDouble(this::distanceSqr).thenComparingLong(Long::longValue));
        }

        double distanceSqr(long packed) {
            return BlockPos.of(packed).getCenter().distanceToSqr(origin);
        }

        void add(long pos) {
            if (queued.add(pos)) blocks.add(pos);
        }

        long poll() {
            long pos = blocks.remove();
            queued.remove(pos);
            return pos;
        }
    }

    private static SoulCloudShutdown get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SoulCloudShutdown::load, SoulCloudShutdown::new, SAVE_NAME);
    }

    public static void begin(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SoulCloudShutdown data = get(level);
        Wave wave = new Wave(player.position(), level.getGameTime());
        int minX = (int) Math.floor((wave.origin.x - RADIUS) / 16);
        int maxX = (int) Math.floor((wave.origin.x + RADIUS) / 16);
        int minZ = (int) Math.floor((wave.origin.z - RADIUS) / 16);
        int maxZ = (int) Math.floor((wave.origin.z + RADIUS) / 16);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) wave.pendingChunks.add(ChunkPos.asLong(x, z));
                else collect(level, chunk, wave);
            }
        }
        if (!wave.blocks.isEmpty() || !wave.pendingChunks.isEmpty()) {
            data.waves.add(wave);
            data.setDirty();
        }
    }

    private static void collect(ServerLevel level, LevelChunk chunk, Wave wave) {
        var sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            int baseY = (level.getMinSection() + sectionIndex) << 4;
            if (baseY > wave.origin.y + RADIUS || baseY + 16 < wave.origin.y - RADIUS || section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() instanceof SoulCloudBlock && !state.getValue(SoulCloudBlock.OFF)))
                continue;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        var state = section.getBlockState(x, y, z);
                        if (!(state.getBlock() instanceof SoulCloudBlock) || state.getValue(SoulCloudBlock.OFF))
                            continue;
                        pos.set(baseX + x, baseY + y, baseZ + z);
                        if (pos.getCenter().distanceToSqr(wave.origin) <= RADIUS * RADIUS) wave.add(pos.asLong());
                    }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        SoulCloudShutdown data = get(level);
        if (data.waves.isEmpty()) return;
        boolean changed = false;
        int budget = CHANGE_BUDGET;
        int deferredScanBudget = 2;
        Iterator<Wave> iterator = data.waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            if (level.getGameTime() % 10 == 0 && deferredScanBudget > 0) {
                Iterator<Long> chunks = wave.pendingChunks.iterator();
                while (chunks.hasNext() && deferredScanBudget > 0) {
                    ChunkPos p = new ChunkPos(chunks.next());
                    LevelChunk chunk = level.getChunkSource().getChunkNow(p.x, p.z);
                    if (chunk == null) continue;
                    collect(level, chunk, wave);
                    chunks.remove();
                    deferredScanBudget--;
                    changed = true;
                }
            }
            double radius = Math.min(RADIUS, Math.max(0, level.getGameTime() - wave.started) * BLOCKS_PER_TICK);
            while (budget > 0 && !wave.blocks.isEmpty() && wave.distanceSqr(wave.blocks.peek()) <= radius * radius) {
                BlockPos pos = BlockPos.of(wave.poll());
                changed = true;
                budget--;
                if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
                    wave.pendingChunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
                    continue;
                }
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof SoulCloudBlock && !state.getValue(SoulCloudBlock.OFF))
                    level.setBlock(pos, state.setValue(SoulCloudBlock.OFF, true), 3);
            }
            if (wave.blocks.isEmpty() && wave.pendingChunks.isEmpty()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) data.setDirty();
    }

    public static SoulCloudShutdown load(CompoundTag root) {
        SoulCloudShutdown data = new SoulCloudShutdown();
        ListTag list = root.getList("Waves", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Wave wave = new Wave(new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")), tag.getLong("Started"));
            for (long p : tag.getLongArray("Blocks")) wave.add(p);
            for (long p : tag.getLongArray("Chunks")) wave.pendingChunks.add(p);
            data.waves.add(wave);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (Wave wave : waves) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("X", wave.origin.x);
            tag.putDouble("Y", wave.origin.y);
            tag.putDouble("Z", wave.origin.z);
            tag.putLong("Started", wave.started);
            tag.putLongArray("Blocks", wave.blocks.stream().mapToLong(Long::longValue).toArray());
            tag.putLongArray("Chunks", wave.pendingChunks.stream().mapToLong(Long::longValue).toArray());
            list.add(tag);
        }
        root.put("Waves", list);
        return root;
    }
}
