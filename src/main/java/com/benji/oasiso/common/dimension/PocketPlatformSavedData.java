package com.benji.oasiso.common.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PocketPlatformSavedData extends SavedData {

    private static final String DATA_NAME = "oasiso_pocket_platforms";
    private final Map<UUID, PlatformRecord> platforms = new HashMap<>();
    private long nextSlot;


    public PocketPlatformSavedData() {
    }


    public static PocketPlatformSavedData load(CompoundTag tag) {
        PocketPlatformSavedData data = new PocketPlatformSavedData();
        data.nextSlot = tag.getLong("NextSlot");
        ListTag list = tag.getList("Platforms", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {

            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Owner")) {
                continue;
            }
            UUID owner = entry.getUUID("Owner");
            long slot = entry.getLong("Slot");
            BlockPos origin = BlockPos.of(entry.getLong("Origin"));
            boolean generated = entry.getBoolean("Generated");
            BlockPos marker = entry.contains("Marker", Tag.TAG_LONG) ? BlockPos.of(entry.getLong("Marker")) : null;

            data.platforms.put(owner, new PlatformRecord(slot, origin, generated, marker));
        }
        return data;
    }

    public static PocketPlatformSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PocketPlatformSavedData::load, PocketPlatformSavedData::new, DATA_NAME);
    }

    @Nullable
    public PlatformRecord getPlatform(UUID owner) {
        return this.platforms.get(owner);
    }

    public long allocateSlot() {
        long slot = this.nextSlot++;
        this.setDirty();
        return slot;
    }

    public PlatformRecord createPlatform(UUID owner, long slot, BlockPos origin) {
        PlatformRecord record = new PlatformRecord(slot, origin, false, null);

        this.platforms.put(owner, record);
        this.setDirty();
        return record;
    }

    public void markGenerated(UUID owner, BlockPos marker) {
        PlatformRecord old = this.platforms.get(owner);

        if (old == null) {
            return;
        }

        this.platforms.put(owner, new PlatformRecord(old.slot(), old.origin(), true, marker.immutable()));
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextSlot", this.nextSlot);
        ListTag list = new ListTag();

        for (Map.Entry<UUID, PlatformRecord> entry : this.platforms.entrySet()) {

            CompoundTag platformTag = new CompoundTag();
            platformTag.putUUID("Owner", entry.getKey());
            PlatformRecord record = entry.getValue();
            platformTag.putLong("Slot", record.slot());
            platformTag.putLong("Origin", record.origin().asLong());
            platformTag.putBoolean("Generated", record.generated());

            if (record.marker() != null) {

                platformTag.putLong("Marker", record.marker().asLong());
            }

            list.add(platformTag);
        }

        tag.put("Platforms", list);

        return tag;
    }

    public record PlatformRecord(long slot, BlockPos origin, boolean generated, @Nullable BlockPos marker) {
    }
}