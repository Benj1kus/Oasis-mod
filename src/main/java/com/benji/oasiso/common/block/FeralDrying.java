package com.benji.oasiso.common.block;

import com.benji.oasiso.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso")
public final class FeralDrying extends SavedData {
    public static final IntegerProperty DRY = IntegerProperty.create("dry", 0, 3);
    private static final TagKey<Block> TARGETS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("oasiso", "feral_dryable"));
    public static final int RADIUS = 96;
    private static final int CELLS_PER_TICK = 192;
    private final ArrayDeque<Wave> waves = new ArrayDeque<>();

    private static FeralDrying get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FeralDrying::load, FeralDrying::new, "oasiso_feral_drying");
    }

    public static void start(ServerLevel level, BlockPos center) {
        FeralDrying data = get(level);
        for (int stage = 1; stage <= 3; stage++) {
            data.waves.add(new Wave(center.immutable(), stage, level.getGameTime() + (stage - 1) * 7L));
        }
        data.setDirty();
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        FeralDrying data = get(level);
        int remaining = CELLS_PER_TICK;
        int count = data.waves.size();
        for (int i = 0; i < count && remaining > 0; i++) {
            Wave wave = data.waves.remove();
            int used = wave.advance(level, Math.min(remaining, Math.max(8, CELLS_PER_TICK / count)));
            remaining -= used;
            if (!wave.finished()) data.waves.add(wave);
            if (used > 0) data.setDirty();
        }
    }

    private record Cell(int x, int y, int z) {
        int distanceSquared() {
            return x * x + y * y + z * z;
        }
    }

    private static final class Grid {
        static final List<Cell> CELLS = build();

        static List<Cell> build() {
            List<Cell> cells = new ArrayList<>();
            int limit = (RADIUS + 3) / 4;
            for (int y = -limit; y <= limit; y++) {
                for (int z = -limit; z <= limit; z++) {
                    for (int x = -limit; x <= limit; x++) {
                        int dx = Math.max(0, 4 * Math.abs(x) - 3);
                        int dy = Math.max(0, 4 * Math.abs(y) - 3);
                        int dz = Math.max(0, 4 * Math.abs(z) - 3);
                        if (dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS) cells.add(new Cell(4 * x, 4 * y, 4 * z));
                    }
                }
            }
            cells.sort(Comparator.comparingInt(Cell::distanceSquared));
            return cells;
        }
    }

    private static final class Wave {
        final BlockPos center;
        final Map<Long, ArrayDeque<Integer>> deferred = new LinkedHashMap<>();
        int cursor;
        long nextRetry;

        final int stage;
        final long startsAt;

        Wave(BlockPos center, int stage, long startsAt) {
            this.center = center;
            this.stage = stage;
            this.startsAt = startsAt;
        }

        boolean finished() {
            return cursor == Grid.CELLS.size() && deferred.isEmpty();
        }

        int advance(ServerLevel level, int budget) {
            if (level.getGameTime() < startsAt) return 0;
            int used = 0;
            while (used < budget && cursor < Grid.CELLS.size()) {
                int id = cursor++;
                Cell cell = Grid.CELLS.get(id);
                int x = (center.getX() & ~3) + cell.x;
                int y = (center.getY() & ~3) + cell.y;
                int z = (center.getZ() & ~3) + cell.z;
                used++;
                if (level.isOutsideBuildHeight(y)) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk != null) dryCell(level, chunk, x, y, z);
                else deferred.computeIfAbsent(ChunkPos.asLong(x >> 4, z >> 4), k -> new ArrayDeque<>()).add(id);
            }
            if (used == budget || level.getGameTime() < nextRetry) return used;
            var entries = deferred.entrySet().iterator();
            while (entries.hasNext() && used < budget) {
                var entry = entries.next();
                ChunkPos cp = new ChunkPos(entry.getKey());
                LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
                if (chunk == null) continue;
                var cells = entry.getValue();
                while (used < budget && !cells.isEmpty()) {
                    Cell cell = Grid.CELLS.get(cells.remove());
                    dryCell(level, chunk, (center.getX() & ~3) + cell.x, (center.getY() & ~3) + cell.y, (center.getZ() & ~3) + cell.z);
                    used++;
                }
                if (cells.isEmpty()) entries.remove();
            }
            if (used < budget) nextRetry = level.getGameTime() + 20;
            return used;
        }

        void dryCell(ServerLevel level, LevelChunk chunk, int x, int y, int z) {
            var section = chunk.getSection(chunk.getSectionIndex(y));
            if (section.hasOnlyAir() || !section.maybeHas(this::affected)) return;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int by = y; by < y + 4; by++) {
                for (int bz = z; bz < z + 4; bz++) {
                    for (int bx = x; bx < x + 4; bx++) {
                        BlockState state = section.getBlockState(bx & 15, by & 15, bz & 15);
                        if (!affected(state)) continue;
                        int dx = bx - center.getX(), dy = by - center.getY(), dz = bz - center.getZ();
                        if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue;
                        pos.set(bx, by, bz);
                        if (state.is(ModBlocks.MUSCIPULA.get())) {
                            level.destroyBlock(pos, false);
                            continue;
                        }
                        level.setBlock(pos, state.setValue(DRY, stage), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }

        boolean affected(BlockState state) {
            if (!state.is(TARGETS)) return false;
            if (state.is(ModBlocks.MUSCIPULA.get())) return stage == 1;
            return state.hasProperty(DRY) && state.getValue(DRY) < stage;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Wave wave : waves) {
            CompoundTag w = new CompoundTag();
            w.putLong("Center", wave.center.asLong());
            w.putInt("Stage", wave.stage);
            w.putLong("StartsAt", wave.startsAt);
            w.putInt("Cursor", wave.cursor);
            ListTag pending = new ListTag();
            wave.deferred.forEach((chunk, cells) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Chunk", chunk);
                entry.putIntArray("Cells", cells.stream().mapToInt(Integer::intValue).toArray());
                pending.add(entry);
            });
            w.put("Deferred", pending);
            list.add(w);
        }
        tag.put("Waves", list);
        return tag;
    }

    private static FeralDrying load(CompoundTag tag) {
        FeralDrying data = new FeralDrying();
        for (Tag item : tag.getList("Waves", Tag.TAG_COMPOUND)) {
            CompoundTag w = (CompoundTag) item;
            Wave wave = new Wave(BlockPos.of(w.getLong("Center")), w.getInt("Stage"), w.getLong("StartsAt"));
            wave.cursor = w.getInt("Cursor");
            for (Tag part : w.getList("Deferred", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) part;
                ArrayDeque<Integer> cells = new ArrayDeque<>();
                for (int id : entry.getIntArray("Cells")) cells.add(id);
                wave.deferred.put(entry.getLong("Chunk"), cells);
            }
            data.waves.add(wave);
        }
        return data;
    }
}
