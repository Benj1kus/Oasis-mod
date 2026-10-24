package com.benji.oasiso.common.cutscene;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public final class CutsceneData extends SimpleJsonResourceReloadListener {
    public static volatile Map<ResourceLocation, String> DATA = Map.of();

    public CutsceneData() {
        super(new Gson(), "cutscenes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, String> next = new HashMap<>();
        files.forEach((id, value) -> {
            try {
                JsonObject j = value.getAsJsonObject();
                if (j.has("enabled") && !j.get("enabled").getAsBoolean()) return;
                if (value.toString().length() > 60000) throw new IllegalArgumentException("Definition too large");
                if (!j.has("animation") || !j.has("camera"))
                    throw new IllegalArgumentException("Missing animation/camera");
                double duration = j.get("duration").getAsDouble();
                if (!Double.isFinite(duration) || duration <= 0 || duration > 60)
                    throw new IllegalArgumentException("duration: 0..60 seconds");
                for (String channel : List.of("position", "rotation")) {
                    JsonArray keys = j.getAsJsonObject("camera").getAsJsonArray(channel);
                    if (keys == null || keys.size() == 0 || keys.size() > 2048)
                        throw new IllegalArgumentException("Missing/large camera track");
                    double previous = -1;
                    for (JsonElement key : keys) {
                        JsonArray row = key.getAsJsonArray();
                        if (row.size() != 4) throw new IllegalArgumentException("Camera key must be [time,x,y,z]");
                        for (JsonElement n : row)
                            if (!Double.isFinite(n.getAsDouble()))
                                throw new IllegalArgumentException("Nonfinite camera value");
                        double t = row.get(0).getAsDouble();
                        if (t < 0 || t <= previous) throw new IllegalArgumentException("Camera times must increase");
                        previous = t;
                    }
                }
                if (j.getAsJsonObject("camera").has("pivot")) {
                    JsonArray pivot = j.getAsJsonObject("camera").getAsJsonArray("pivot");
                    if (pivot.size() != 3) throw new IllegalArgumentException("camera.pivot must be [x,y,z]");
                    for (JsonElement n : pivot)
                        if (!Double.isFinite(n.getAsDouble()))
                            throw new IllegalArgumentException("Nonfinite camera pivot");
                }
                range(j, "transition_seconds", .4, 4);
                range(j, "transition_black_hold", .05, 1);
                range(j, "skip_scale", .5, 3);
                range(j, "background_dim", 0, .9);
                range(j, "radius", 1, 32);
                range(j, "fov", 10, 150);
                range(j, "aspect", .5, 4);
                range(j, "model_scale", .01, 20);
                for (String name : List.of("title", "skip", "skip_hold"))
                    if (j.has(name) && ResourceLocation.tryParse(j.get(name).getAsString()) == null)
                        throw new IllegalArgumentException("Invalid texture " + name);
                next.put(id, value.toString());
            } catch (Exception e) {
                LogUtils.getLogger().error("Invalid cutscene {}", id, e);
            }
        });
        DATA = Map.copyOf(next);
    }

    private static void range(JsonObject j, String key, double min, double max) {
        if (!j.has(key)) return;
        double v = j.get(key).getAsDouble();
        if (!Double.isFinite(v) || v < min || v > max) throw new IllegalArgumentException("Invalid " + key);
    }

}
