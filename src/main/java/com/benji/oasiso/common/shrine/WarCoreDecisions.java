package com.benji.oasiso.common.shrine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WarCoreDecisions extends SavedData {
    private final Map<UUID, Integer> choices = new HashMap<>();

    public static WarCoreDecisions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WarCoreDecisions::load, WarCoreDecisions::new, "oasiso_war_core_decisions");
    }

    public int choice(UUID group) {
        return choices.getOrDefault(group, 0);
    }

    public boolean decide(UUID group, int choice) {
        if (choice < 1 || choice > 2 || choices.containsKey(group)) return false;
        choices.put(group, choice);
        setDirty();
        return true;
    }

    public static WarCoreDecisions load(CompoundTag tag) {
        WarCoreDecisions result = new WarCoreDecisions();
        CompoundTag entries = tag.getCompound("Choices");
        for (String key : entries.getAllKeys()) {
            try {
                int choice = entries.getInt(key);
                if (choice == 1 || choice == 2) result.choices.put(UUID.fromString(key), choice);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();
        choices.forEach((id, choice) -> entries.putInt(id.toString(), choice));
        tag.put("Choices", entries);
        return tag;
    }
}
