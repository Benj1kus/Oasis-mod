package com.benji.oasiso.client.voice;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.*;
import java.io.*;

public final class InnerVoicePreferences {
    public static final String[] VOICES = {"PALADIN", "OSIRIS", "SOUL", "SCIENCE", "HAPPY", "SAD", "ANGER", "BEAST", "SPRING", "GRANDPA", "EH", "BOB", "BONG", "MECHANICAL","TUN", "ANGRY SOUL", "SAD SOUL", "CHILD'S SOUL"};
    public static int vignette = 0x00FFB9, text = 0x03FFF2, voice = 13;
    private static boolean loaded;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("oasiso-inner-voice.json");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(file())) return;
        try (Reader r = Files.newBufferedReader(file())) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            vignette = Integer.parseInt(o.get("vignette").getAsString(), 16) & 0xFFFFFF;
            text = Integer.parseInt(o.get("text").getAsString(), 16) & 0xFFFFFF;
            String name = o.get("voice").getAsString();
            for (int i = 0; i < VOICES.length; i++) if (VOICES[i].equals(name)) voice = i;
        } catch (Exception ex) {
            LogUtils.getLogger().warn("Cannot read inner voice preferences", ex);
        }
    }

    public static boolean save(int v, int t, int sound) {
        try {
            Files.createDirectories(file().getParent());
            JsonObject o = new JsonObject();
            o.addProperty("vignette", String.format("%06X", v));
            o.addProperty("text", String.format("%06X", t));
            o.addProperty("voice", VOICES[sound]);
            try (Writer w = Files.newBufferedWriter(file())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(o, w);
            }
            vignette = v;
            text = t;
            voice = sound;
            return true;
        } catch (IOException ex) {
            LogUtils.getLogger().warn("Cannot save inner voice preferences", ex);
            return false;
        }
    }
}
