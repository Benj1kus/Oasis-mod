package com.benji.oasiso.common.flower;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record FlowerDefinition(ResourceLocation id, ResourceLocation block, ResourceLocation voice, int color,
                               double activationRadius, double replyRadius, double viewRadius, double flameHeight,
                               double textHeight, float flameSize, float textScale, int frameTicks, float volume,
                               float pitch, List<ResourceLocation> frames, Map<String, Line> lines) {
    public record Reply(ResourceLocation flower, String line, int delayTicks, double chance) {
    }

    public record Line(String key, boolean random, int typingTicks, int holdTicks, int fadeTicks, List<Reply> replies) {
        public int duration() {
            return typingTicks + holdTicks + fadeTicks;
        }
    }

    public static FlowerDefinition parse(ResourceLocation id, JsonObject json) {
        ResourceLocation block = location(string(json, "block", null));
        ResourceLocation voice = location(string(json, "voice", null));
        String hex = string(json, "text_color", "#FFFFFF");
        if (!hex.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("text_color must be #RRGGBB");
        List<ResourceLocation> frames = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("flame_frames")) frames.add(location(e.getAsString()));
        if (frames.isEmpty() || frames.size() > 32) throw new IllegalArgumentException("1..32 flame_frames required");
        Map<String, Line> lines = new LinkedHashMap<>();
        JsonObject source = json.getAsJsonObject("lines");
        if (source == null || source.size() == 0 || source.size() > 64)
            throw new IllegalArgumentException("1..64 lines required");
        for (var entry : source.entrySet()) {
            if (entry.getKey().length() > 128) throw new IllegalArgumentException("Line ID too long");
            JsonObject line = entry.getValue().getAsJsonObject();
            List<Reply> replies = new ArrayList<>();
            if (line.has("replies")) for (JsonElement element : line.getAsJsonArray("replies")) {
                JsonObject reply = element.getAsJsonObject();
                replies.add(new Reply(location(string(reply, "flower", null)), string(reply, "line", null), integer(reply, "delay_ticks", 8, 0, 200), number(reply, "chance", 1, 0, 1)));
            }
            if (replies.size() > 16) throw new IllegalArgumentException("Too many replies");
            lines.put(entry.getKey(), new Line(string(line, "key", null), !line.has("random") || line.get("random").getAsBoolean(), integer(line, "typing_ticks", 60, 10, 1200), integer(line, "hold_ticks", 50, 0, 1200), integer(line, "fade_ticks", 12, 1, 100), List.copyOf(replies)));
        }
        if (lines.values().stream().noneMatch(Line::random))
            throw new IllegalArgumentException("At least one random line required");
        return new FlowerDefinition(id, block, voice, Integer.parseInt(hex.substring(1), 16), number(json, "activation_radius", 5, 1, 8), number(json, "reply_radius", 8, 1, 12), number(json, "view_radius", 24, 8, 48), number(json, "flame_height", 1.0, .1, 4), number(json, "text_height", 1.45, .1, 6), (float) number(json, "flame_size", .4, .05, 2), (float) number(json, "text_scale", .018, .005, .05), integer(json, "frame_ticks", 3, 1, 40), (float) number(json, "voice_volume", .35, 0, 2), (float) number(json, "voice_pitch", 1, .5, 2), List.copyOf(frames), Collections.unmodifiableMap(lines));
    }

    private static ResourceLocation location(String value) {
        ResourceLocation result = ResourceLocation.tryParse(value);
        if (result == null) throw new IllegalArgumentException("Invalid resource location: " + value);
        return result;
    }

    private static String string(JsonObject o, String name, String fallback) {
        String value = o.has(name) ? o.get(name).getAsString() : fallback;
        if (value == null || value.isBlank() || value.length() > 256)
            throw new IllegalArgumentException("Missing/invalid " + name);
        return value;
    }

    private static double number(JsonObject o, String name, double fallback, double min, double max) {
        double value = o.has(name) ? o.get(name).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(name + " must be in [" + min + ", " + max + "]");
        return value;
    }

    private static int integer(JsonObject o, String name, int fallback, int min, int max) {
        double value = number(o, name, fallback, min, max);
        if (value != Math.floor(value)) throw new IllegalArgumentException(name + " must be an integer");
        return (int) value;
    }
}
