package com.benji.oasiso.client.configscroll;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

final class ConfigScrollTheme {
    static final int GOLD = 0xE7C777;
    static final int CYAN = 0x37DBD3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("oasiso-config-scroll-ui.json");
    int primary = GOLD;
    int accent = CYAN;
    private int savedPrimary = GOLD;
    private int savedAccent = CYAN;

    static ConfigScrollTheme load() {
        ConfigScrollTheme theme = new ConfigScrollTheme();
        if (Files.isRegularFile(FILE)) {
            try (var reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("primary")) theme.primary = parse(json.get("primary").getAsString());
                if (json.has("accent")) theme.accent = parse(json.get("accent").getAsString());
            } catch (Exception ex) { LogUtils.getLogger().warn("Could not read scroll colours", ex); }
        }
        theme.savedPrimary = theme.primary;
        theme.savedAccent = theme.accent;
        return theme;
    }

    static int parse(String hex) {
        String text = hex.startsWith("#") ? hex.substring(1) : hex;
        if (!text.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use six hex digits, e.g. #37DBD3");
        return Integer.parseInt(text, 16);
    }

    static String hex(int color) { return String.format("#%06X", color & 0xFFFFFF); }
    boolean dirty() { return primary != savedPrimary || accent != savedAccent; }

    void save() throws IOException {
        if (!dirty()) return;
        Files.createDirectories(FILE.getParent());
        Path temp = Files.createTempFile(FILE.getParent(), "oasiso-scroll-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(Map.of("primary", hex(primary), "accent", hex(accent))), StandardCharsets.UTF_8);
            try { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); }
            savedPrimary = primary;
            savedAccent = accent;
        } finally { Files.deleteIfExists(temp); }
    }
}
