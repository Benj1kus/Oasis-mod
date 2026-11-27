package com.benji.oasiso.common.cowrit;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mod.EventBusSubscriber(modid = Oasiso.MODID)
public final class FatCoatings extends SavedData {
    private static final ConcurrentMap<Level, FatCoatings> LEVELS = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<Long>> chunks = new ConcurrentHashMap<>();

    public static boolean has(Level level, BlockPos pos) {
        FatCoatings data = LEVELS.get(level);
        if (data == null || data.chunks.isEmpty()) return false;
        Set<Long> chunk = data.chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return chunk != null && chunk.contains(pos.asLong());
    }

    private void put(BlockPos pos, boolean coated) {
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        if (coated) {
            chunks.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(pos.asLong());
        } else {
            var chunk = chunks.get(key);
            if (chunk != null && chunk.remove(pos.asLong()) && chunk.isEmpty()) chunks.remove(key);
        }
    }

    public static void change(ServerLevel level, BlockPos pos, boolean coated) {
        var data = LEVELS.get(level);
        if (data == null || has(level, pos) == coated) return;
        data.put(pos, coated);
        data.setDirty();
        level.getChunkSource().getLightEngine().checkBlock(pos);
        var packet = new FatNetwork.Mark(level.dimension().location(), pos.immutable(), coated);
        FatNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), packet);
    }

    static void receive(Level level, FatNetwork.Mark mark) {
        if (!level.dimension().location().equals(mark.dimension())) return;
        LEVELS.computeIfAbsent(level, ignored -> new FatCoatings()).put(mark.pos(), mark.coated());
        level.getChunkSource().getLightEngine().checkBlock(mark.pos());
    }

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.put(level, level.getDataStorage().computeIfAbsent(FatCoatings::read, FatCoatings::new, "oasiso_cowrit_fat"));
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) LEVELS.remove(level);
    }

    @SubscribeEvent
    public static void forgetChunk(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide) {
            var data = LEVELS.get(level);
            if (data != null) data.chunks.remove(event.getChunk().getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void watch(ChunkWatchEvent.Watch event) {
        var data = LEVELS.get(event.getLevel());
        if (data == null) return;
        var marks = data.chunks.get(event.getPos().toLong());
        if (marks == null) return;
        for (long pos : marks) {
            FatNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), new FatNetwork.Mark(event.getLevel().dimension().location(), BlockPos.of(pos), true));
        }
    }

    private static FatCoatings read(CompoundTag tag) {
        FatCoatings data = new FatCoatings();
        for (long pos : tag.getLongArray("Coated")) data.put(BlockPos.of(pos), true);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Coated", chunks.values().stream().flatMap(Set::stream).mapToLong(Long::longValue).toArray());
        return tag;
    }
}
