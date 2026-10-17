package com.benji.oasiso.common.flower;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

public final class FlowerData extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final int MAX_JSON = 262144;
    private static volatile Map<ResourceLocation, FlowerDefinition> definitions = Map.of();
    private static volatile String syncJson = "{}";

    public FlowerData() {
        super(new Gson(), "talking_flowers");
    }

    public static Map<ResourceLocation, FlowerDefinition> definitions() {
        return definitions;
    }

    public static String syncJson() {
        return syncJson;
    }

    public static FlowerDefinition forBlock(ResourceLocation block) {
        for (FlowerDefinition def : definitions.values()) if (def.block().equals(block)) return def;
        return null;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, FlowerDefinition> next = new LinkedHashMap<>();
        Set<ResourceLocation> blocks = new HashSet<>();
        JsonObject sync = new JsonObject();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (json.has("enabled") && !json.get("enabled").getAsBoolean()) return;
                FlowerDefinition def = FlowerDefinition.parse(entry.getKey(), json);
                if (!ForgeRegistries.BLOCKS.containsKey(def.block()))
                    throw new IllegalArgumentException("Unknown block: " + def.block());
                if (!ForgeRegistries.SOUND_EVENTS.containsKey(def.voice()))
                    throw new IllegalArgumentException("Unknown voice: " + def.voice());
                if (blocks.contains(def.block())) throw new IllegalArgumentException("Duplicate block: " + def.block());
                if (next.size() >= 64) throw new IllegalArgumentException("Maximum 64 flower definitions");
                sync.add(entry.getKey().toString(), json);
                if (sync.toString().length() > MAX_JSON) {
                    sync.remove(entry.getKey().toString());
                    throw new IllegalArgumentException("Flower config exceeds network size limit");
                }
                blocks.add(def.block());
                next.put(entry.getKey(), def);
            } catch (RuntimeException ex) {
                LOG.error("Invalid talking flower {}: {}", entry.getKey(), ex.getMessage());
            }
        });
        for (FlowerDefinition def : next.values())
            for (var line : def.lines().values())
                for (var reply : line.replies()) {
                    FlowerDefinition target = next.get(reply.flower());
                    if (target == null || !target.lines().containsKey(reply.line()))
                        LOG.warn("Talking flower {} has unresolved reply {} / {}", def.id(), reply.flower(), reply.line());
                }
        definitions = Collections.unmodifiableMap(next);
        syncJson = sync.toString();
        LOG.info("Loaded {} talking flower definitions", next.size());
    }
}
