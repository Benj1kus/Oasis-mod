package com.benji.oasiso.common.entity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ApollyonAttackTimeline {
    public static final int PREPARE, START, END, JUMP_LENGTH;

    static {
        int prepare = 9, start = 25, end = 92, jump = 21;
        try (var stream = ApollyonAttackTimeline.class.getResourceAsStream("/assets/oasiso/animations/apollyon.animation.json")) {
            if (stream == null) throw new IllegalStateException("Missing apollyon.animation.json");
            JsonObject animations = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
            var timeline = animations.getAsJsonObject("attack").getAsJsonObject("timeline");
            int p = key(timeline, "attack_prepare"), s = key(timeline, "attack_start"), e = key(timeline, "attack_end");
            int j = (int) Math.ceil(animations.getAsJsonObject("jump").get("animation_length").getAsDouble() * 20 - 1E-6);
            if (p < 0 || s <= p || e <= s || j <= 0 || e > 1200 || j > 1200)
                throw new IllegalArgumentException("Invalid attack timeline");
            prepare = p;
            start = s;
            end = e;
            jump = j;
        } catch (Exception error) {
            com.mojang.logging.LogUtils.getLogger().error("[Apollyon] Cannot read attack timeline; using 9/25/92/21 ticks", error);
        }
        PREPARE = prepare;
        START = start;
        END = end;
        JUMP_LENGTH = jump;
    }

    public static final int COMMON_PREPARE, COMMON_HIT, COMMON_LENGTH;

    static {
        int prepare = 5, hit = 15, length = 30;
        try (var stream = ApollyonAttackTimeline.class.getResourceAsStream("/assets/oasiso/animations/apollyon.animation.json")) {
            if (stream == null) throw new IllegalStateException("Missing apollyon.animation.json");
            JsonObject animation = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations").getAsJsonObject("attack_common");
            var timeline = animation.getAsJsonObject("timeline");
            int p = key(timeline, "common_prepare"), h = key(timeline, "common_hit");
            int end = (int) Math.ceil(animation.get("animation_length").getAsDouble() * 20 - 1E-6);
            if (p < 0 || h <= p || end <= h || end > 1200)
                throw new IllegalArgumentException("Invalid common timeline");
            prepare = p;
            hit = h;
            length = end;
        } catch (Exception error) {
            com.mojang.logging.LogUtils.getLogger().error("[Apollyon] Cannot read common timeline; using 5/15/30 ticks", error);
        }
        COMMON_PREPARE = prepare;
        COMMON_HIT = hit;
        COMMON_LENGTH = length;
    }

    public static final int DRILL_START, DRILL_END;

    static {
        int start = 31, end = 87;
        try (var stream = ApollyonAttackTimeline.class.getResourceAsStream("/assets/oasiso/animations/apollyon.animation.json")) {
            if (stream == null) throw new IllegalStateException("Missing apollyon.animation.json");
            JsonObject animation = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations").getAsJsonObject("attack_drill");
            var timeline = animation.getAsJsonObject("timeline");
            int s = key(timeline, "drill_start"), e = key(timeline, "drill_end");
            if (s < 0 || e <= s || e > 1200) throw new IllegalArgumentException("Invalid drill timeline");
            start = s;
            end = e;
        } catch (Exception error) {
            com.mojang.logging.LogUtils.getLogger().error("[Apollyon] Cannot read drill timeline; using 31/87 ticks", error);
        }
        DRILL_START = start;
        DRILL_END = end;
    }

    public static final int UP_SPEAR, SUMMON_LENGTH;

    static {
        int up = 15, length = 40;
        try (var stream = ApollyonAttackTimeline.class.getResourceAsStream("/assets/oasiso/animations/apollyon.animation.json")) {
            if (stream == null) throw new IllegalStateException("Missing apollyon.animation.json");
            JsonObject animation = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations").getAsJsonObject("summon");
            int u = key(animation.getAsJsonObject("timeline"), "up_spear");
            int l = (int) Math.ceil(animation.get("animation_length").getAsDouble() * 20 - 1E-6);
            if (u < 0 || l <= u || l > 1200) throw new IllegalArgumentException("Invalid summon timeline");
            up = u;
            length = l;
        } catch (Exception error) {
            com.mojang.logging.LogUtils.getLogger().error("[Apollyon] Cannot read summon timeline; using 15/40 ticks", error);
        }
        UP_SPEAR = up;
        SUMMON_LENGTH = length;
    }

    private static int key(JsonObject timeline, String key) {
        for (var entry : timeline.entrySet()) {
            var value = entry.getValue();
            if (value.isJsonArray()) {
                for (var part : value.getAsJsonArray())
                    if (matches(part.getAsString(), key)) return ticks(entry.getKey());
            } else if (matches(value.getAsString(), key)) return ticks(entry.getKey());
        }
        return -1;
    }

    private static boolean matches(String text, String key) {
        for (String instruction : text.split(";")) if (instruction.trim().equals(key)) return true;
        return false;
    }

    private static int ticks(String seconds) {
        return (int) Math.ceil(Double.parseDouble(seconds) * 20 - 1E-6);
    }

    private ApollyonAttackTimeline() {
    }
}
