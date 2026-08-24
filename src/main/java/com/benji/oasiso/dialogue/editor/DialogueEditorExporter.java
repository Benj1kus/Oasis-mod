package com.benji.oasiso.dialogue.editor;

import com.benji.oasiso.dialogue.DialogueRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DialogueEditorExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DialogueEditorExporter() {
    }

    public record ExportResult(Path root, Path datapack, Path resourcepack) {
    }

    public static ExportResult export(DialogueEditorProject project) throws IOException {
        project.normalize();
        DialogueEditorWorkspace.save(project);

        String slug = sanitize(project.namespace + "_" + project.dialogue_path.replace('/', '_'));
        Path root = DialogueEditorWorkspace.exportsRoot().resolve(slug);
        Path datapack = root.resolve("datapack");
        Path resourcepack = root.resolve("resourcepack");

        deleteTree(root);
        Files.createDirectories(datapack);
        Files.createDirectories(resourcepack);

        writePackMcmeta(datapack, "Oasiso Dialogue - " + project.dialogueId());
        writePackMcmeta(resourcepack, "Oasiso Dialogue Resources - " + project.dialogueId());

        Path dialogue = datapack.resolve("data").resolve(project.namespace).resolve("dialogues").resolve(project.dialogue_path + ".json");
        Files.createDirectories(dialogue.getParent());
        Files.writeString(dialogue, DialogueRegistry.toJson(project.definition), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        Path namespaceAssets = resourcepack.resolve("assets").resolve(project.namespace);
        Files.createDirectories(namespaceAssets);

        Path imported = DialogueEditorWorkspace.assetRoot(project);
        if (Files.exists(imported)) copyTree(imported, namespaceAssets);

        writeLanguages(project, namespaceAssets);
        writeSounds(project, namespaceAssets);

        zipTree(datapack, root.resolve("datapack.zip"));
        zipTree(resourcepack, root.resolve("resourcepack.zip"));

        return new ExportResult(root, datapack, resourcepack);
    }

    public static ExportResult installToCurrentInstance(DialogueEditorProject project) throws IOException {
        ExportResult result = export(project);
        Minecraft minecraft = Minecraft.getInstance();

        String slug = sanitize(project.namespace + "_" + project.dialogue_path.replace('/', '_'));
        Path resourceTarget = minecraft.gameDirectory.toPath().resolve("resourcepacks").resolve("OasisoDialogue_" + slug);
        deleteTree(resourceTarget);
        copyTree(result.resourcepack, resourceTarget);

        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            Path dataTarget = worldRoot.resolve("datapacks").resolve("OasisoDialogue_" + slug);
            deleteTree(dataTarget);
            copyTree(result.datapack, dataTarget);
        }

        return result;
    }

    private static void writeLanguages(DialogueEditorProject project, Path namespaceAssets) throws IOException {
        if (project.languages == null || project.languages.isEmpty()) return;
        Path langDir = namespaceAssets.resolve("lang");
        Files.createDirectories(langDir);

        for (Map.Entry<String, ? extends Map<String, String>> locale : project.languages.entrySet()) {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, String> entry : locale.getValue().entrySet()) {
                root.addProperty(entry.getKey(), entry.getValue());
            }
            Files.writeString(langDir.resolve(locale.getKey() + ".json"), GSON.toJson(root), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static void writeSounds(DialogueEditorProject project, Path namespaceAssets) throws IOException {
        if (project.sounds == null || project.sounds.isEmpty()) return;

        JsonObject root = new JsonObject();
        for (Map.Entry<String, String> entry : project.sounds.entrySet()) {
            JsonObject sound = new JsonObject();
            JsonArray array = new JsonArray();
            JsonObject item = new JsonObject();
            item.addProperty("name", project.namespace + ":" + entry.getValue());
            item.addProperty("stream", false);
            array.add(item);
            sound.add("sounds", array);
            root.add(entry.getKey(), sound);
        }

        Files.writeString(namespaceAssets.resolve("sounds.json"), GSON.toJson(root), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void writePackMcmeta(Path root, String description) throws IOException {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 15);
        pack.addProperty("description", description);
        JsonObject wrapper = new JsonObject();
        wrapper.add("pack", pack);
        Files.writeString(root.resolve("pack.mcmeta"), GSON.toJson(wrapper), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


    private static void zipTree(Path source, Path zipFile) throws IOException {
        Files.deleteIfExists(zipFile);
        Files.createDirectories(zipFile.getParent());

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            try (var stream = Files.walk(source)) {
                for (Path path : stream.filter(Files::isRegularFile).toList()) {
                    String entryName = source.relativize(path).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        if (!Files.exists(source)) return;
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path out = target.resolve(relative.toString());
                if (Files.isDirectory(path)) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static String sanitize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
    }
}
