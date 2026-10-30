package com.benji.oasiso.common.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class SoulStorageData extends SavedData {
    final Map<UUID, Visit> visits = new LinkedHashMap<>();

    public static SoulStorageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SoulStorageData::load, SoulStorageData::new, "oasiso_soul_storage");
    }

    static final class Visit {
        final UUID id;
        final BlockPos source, origin, size;
        final Direction.Axis sourceAxis;
        BlockPos exit;
        Direction.Axis exitAxis = Direction.Axis.X;
        final Set<UUID> visitors = new HashSet<>();
        boolean closing;
        int cursor;

        Visit(UUID id, BlockPos source, Direction.Axis sourceAxis, BlockPos origin, BlockPos size) {
            this.id = id;
            this.source = source;
            this.sourceAxis = sourceAxis;
            this.origin = origin;
            this.size = size;
        }

        BlockPos max() {
            return origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        }

        AABB bounds() {
            return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size.getX(), origin.getY() + size.getY(), origin.getZ() + size.getZ());
        }

        int volume() {
            return size.getX() * size.getY() * size.getZ();
        }

        BlockPos cell(int index) {
            int layer = size.getX() * size.getZ();
            return origin.offset(index % size.getX(), size.getY() - 1 - index / layer, index / size.getX() % size.getZ());
        }
    }

    static SoulStorageData load(CompoundTag root) {
        SoulStorageData data = new SoulStorageData();
        ListTag list = root.getList("Visits", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (!t.hasUUID("Id")) continue;
            BlockPos size = BlockPos.of(t.getLong("Size"));
            if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1 || size.getX() > 96 || size.getY() > 64 || size.getZ() > 96)
                continue;
            Visit v = new Visit(t.getUUID("Id"), BlockPos.of(t.getLong("Source")), axis(t.getString("SourceAxis")), BlockPos.of(t.getLong("Origin")), size);
            if (t.contains("Exit", Tag.TAG_LONG)) v.exit = BlockPos.of(t.getLong("Exit"));
            v.exitAxis = axis(t.getString("ExitAxis"));
            v.closing = t.getBoolean("Closing");
            v.cursor = Math.max(0, Math.min(v.volume(), t.getInt("Cursor")));
            ListTag people = t.getList("Visitors", Tag.TAG_COMPOUND);
            for (int j = 0; j < people.size(); j++)
                if (people.getCompound(j).hasUUID("Id")) v.visitors.add(people.getCompound(j).getUUID("Id"));
            data.visits.put(v.id, v);
        }
        return data;
    }

    private static Direction.Axis axis(String s) {
        return "z".equals(s) ? Direction.Axis.Z : Direction.Axis.X;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (Visit v : visits.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", v.id);
            t.putLong("Source", v.source.asLong());
            t.putString("SourceAxis", v.sourceAxis.getName());
            t.putLong("Origin", v.origin.asLong());
            t.putLong("Size", v.size.asLong());
            if (v.exit != null) t.putLong("Exit", v.exit.asLong());
            t.putString("ExitAxis", v.exitAxis.getName());
            t.putBoolean("Closing", v.closing);
            t.putInt("Cursor", v.cursor);
            ListTag people = new ListTag();
            for (UUID id : v.visitors) {
                CompoundTag p = new CompoundTag();
                p.putUUID("Id", id);
                people.add(p);
            }
            t.put("Visitors", people);
            list.add(t);
        }
        root.put("Visits", list);
        return root;
    }
}
