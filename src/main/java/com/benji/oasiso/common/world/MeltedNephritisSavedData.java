package com.benji.oasiso.common.world;

import com.benji.oasiso.Oasiso;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class MeltedNephritisSavedData extends SavedData {

    private static final String DATA_NAME = Oasiso.MODID + "_melted_nephritis";
    private static final String POSITIONS_TAG = "CoatedPositions";

    private final Set<Long> coatedPositions = new HashSet<>();

    public MeltedNephritisSavedData() {
    }

    public static MeltedNephritisSavedData load(CompoundTag tag) {
        MeltedNephritisSavedData data = new MeltedNephritisSavedData();

        for (long packed : tag.getLongArray(POSITIONS_TAG)) {
            data.coatedPositions.add(packed);
        }

        return data;
    }

    public static MeltedNephritisSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MeltedNephritisSavedData::load, MeltedNephritisSavedData::new, DATA_NAME);
    }

    public boolean isCoated(BlockPos pos) {
        return this.coatedPositions.contains(pos.asLong());
    }

    public boolean add(BlockPos pos) {
        boolean changed = this.coatedPositions.add(pos.asLong());
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean remove(BlockPos pos) {
        boolean changed = this.coatedPositions.remove(pos.asLong());
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Set<Long> getPackedPositions() {
        return Collections.unmodifiableSet(this.coatedPositions);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] positions = new long[this.coatedPositions.size()];
        int index = 0;

        for (long packed : this.coatedPositions) {
            positions[index++] = packed;
        }

        tag.putLongArray(POSITIONS_TAG, positions);
        return tag;
    }
}
