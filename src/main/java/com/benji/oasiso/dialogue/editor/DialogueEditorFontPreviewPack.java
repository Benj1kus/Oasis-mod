package com.benji.oasiso.dialogue.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DialogueEditorFontPreviewPack {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_FOLDER = "Oasiso_Studio_Font_Preview";
    private static final String EXPECTED_PACK_ID = "file/" + PACK_FOLDER;
    private static String lastSignature = "";
    private static boolean reloadInFlight;
    private static String lastError;

    private DialogueEditorFontPreviewPack() {
    }

    public static void ensureLoaded(DialogueEditorProject project) {
        if (project == null) {
            return;
        }

        project.normalize();

        if (project.fonts == null || project.fonts.isEmpty()) {
            lastError = null;
            return;
        }

        if (reloadInFlight) {
            return;
        }

        try {
            String signature = signature(project);
            Minecraft minecraft = Minecraft.getInstance();
            PackRepository repository = minecraft.getResourcePackRepository();
            boolean packSelected = repository.getSelectedIds().stream().anyMatch(DialogueEditorFontPreviewPack::isPreviewPackId);

            if (signature.equals(lastSignature) && packSelected && fontsVisible(minecraft, project)) {
                return;
            }

            syncPackFiles(minecraft, project);

            repository.reload();

            String actualPackId = repository.getAvailableIds().stream().filter(DialogueEditorFontPreviewPack::isPreviewPackId).findFirst().orElse(null);

            if (actualPackId == null) {
                lastError = "Studio font preview pack was written but Minecraft did not discover it.";

                return;
            }

            List<String> selected = new ArrayList<>(repository.getSelectedIds());

            selected.removeIf(DialogueEditorFontPreviewPack::isPreviewPackId);
            selected.add(actualPackId);

            repository.setSelected(selected);

            minecraft.options.updateResourcePacks(repository);
            minecraft.options.save();
            reloadInFlight = true;
            lastError = null;

            String completedSignature = signature;

            minecraft.reloadResourcePacks().whenComplete((unused, throwable) -> minecraft.execute(() -> {
                reloadInFlight = false;

                if (throwable != null) {
                    lastError = "Live font reload failed: " + throwable.getClass().getSimpleName();

                    return;
                }

                lastSignature = completedSignature;

                lastError = null;
            }));

        } catch (Exception exception) {
            reloadInFlight = false;

            lastError = "Live font preview failed: " + (exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());
        }
    }

    public static void refresh(DialogueEditorProject project) {
        lastSignature = "";

        ensureLoaded(project);
    }


    public static String statusLine(DialogueEditorProject project) {
        if (project == null || project.fonts == null || project.fonts.isEmpty()) {

            return "Live preview: Minecraft default font";
        }

        if (reloadInFlight) {
            return "Live preview: loading imported fonts...";
        }

        if (lastError != null && !lastError.isBlank()) {

            return lastError;
        }

        return "Live preview fonts: READY";
    }


    private static void syncPackFiles(Minecraft minecraft, DialogueEditorProject project) throws IOException {
        Path root = minecraft.getResourcePackDirectory().resolve(PACK_FOLDER);

        deleteTree(root);

        Files.createDirectories(root);

        writePackMcmeta(root);

        Path namespaceAssets = root.resolve("assets").resolve(project.namespace);

        Path fontDir = namespaceAssets.resolve("font");

        Path textureFontDir = namespaceAssets.resolve("textures/font");

        Files.createDirectories(fontDir);

        Files.createDirectories(textureFontDir);

        for (Map.Entry<String, DialogueEditorProject.FontAsset> entry : project.fonts.entrySet()) {

            String key = sanitize(entry.getKey());

            DialogueEditorProject.FontAsset asset = entry.getValue();

            if (asset == null || asset.file == null || asset.file.isBlank()) {
                continue;
            }

            JsonObject provider = new JsonObject();

            if ("bitmap_msdf".equalsIgnoreCase(asset.type)) {
                Path source = DialogueEditorWorkspace.assetRoot(project).resolve("textures").resolve(asset.file);

                if (!Files.isRegularFile(source)) {
                    continue;
                }

                Path relativeTexture = Path.of(asset.file.replace('\\', '/'));

                Path target = namespaceAssets.resolve("textures").resolve(relativeTexture);

                Files.createDirectories(target.getParent());

                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                provider.addProperty("type", "bitmap");

                provider.addProperty("file", project.namespace + ":" + asset.file.replace('\\', '/'));

                provider.addProperty("ascent", Math.max(1, Math.min(asset.height, asset.ascent)));

                provider.addProperty("height", Math.max(1, asset.height));

                JsonArray chars = new JsonArray();

                if (asset.chars != null) {
                    for (String row : asset.chars) {

                        chars.add(row != null ? row : "");
                    }
                }

                provider.add("chars", chars);

            } else {
                Path source = DialogueEditorWorkspace.assetRoot(project).resolve(asset.file);

                if (!Files.isRegularFile(source)) {
                    continue;
                }

                String fileName = source.getFileName().toString();

                Files.copy(source, fontDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

                provider.addProperty("type", "ttf");
                provider.addProperty("file", project.namespace + ":" + fileName);

                provider.addProperty("size", Math.max(1.0F, asset.size));
                provider.addProperty("oversample", Math.max(8.0F, asset.oversample));

                JsonArray shift = new JsonArray();

                shift.add(0.0F);

                shift.add(0.0F);

                provider.add("shift", shift);
            }

            JsonArray providers = new JsonArray();

            providers.add(provider);

            JsonObject fontJson = new JsonObject();

            fontJson.add("providers", providers);

            Files.writeString(fontDir.resolve(key + ".json"), GSON.toJson(fontJson), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }


    private static boolean fontsVisible(Minecraft minecraft, DialogueEditorProject project) {
        for (String key : project.fonts.keySet()) {

            String clean = sanitize(key);

            var location = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(project.namespace, "font/" + clean + ".json");

            if (minecraft.getResourceManager().getResource(location).isEmpty()) {

                return false;
            }
        }

        return true;
    }


    private static String signature(DialogueEditorProject project) {
        StringBuilder builder = new StringBuilder();

        builder.append(project.workspace_id).append('|').append(project.namespace);

        List<String> keys = new ArrayList<>(project.fonts.keySet());

        keys.sort(String::compareTo);

        for (String key : keys) {

            DialogueEditorProject.FontAsset asset = project.fonts.get(key);

            if (asset == null) {
                continue;
            }

            builder.append('|').append(key).append(':').append(asset.type).append(':').append(asset.file).append(':').append(asset.size).append(':').append(asset.oversample).append(':').append(asset.height).append(':').append(asset.ascent);

            if (asset.chars != null) {
                builder.append(':').append(asset.chars.hashCode());
            }

            Path source = sourcePath(project, asset);

            if (source != null && Files.isRegularFile(source)) {

                try {
                    builder.append(':').append(Files.size(source)).append(':').append(Files.getLastModifiedTime(source).toMillis());

                } catch (Exception ignored) {
                }
            }
        }

        return builder.toString();
    }


    private static Path sourcePath(DialogueEditorProject project, DialogueEditorProject.FontAsset asset) {
        if (asset == null || asset.file == null || asset.file.isBlank()) {
            return null;
        }

        if ("bitmap_msdf".equalsIgnoreCase(asset.type)) {

            return DialogueEditorWorkspace.assetRoot(project).resolve("textures").resolve(asset.file);
        }

        return DialogueEditorWorkspace.assetRoot(project).resolve(asset.file);
    }


    private static void writePackMcmeta(Path root) throws IOException {
        JsonObject pack = new JsonObject();

        pack.addProperty("pack_format", 15);

        pack.addProperty("description", "Oasiso Studio - Live Font Preview");

        JsonObject wrapper = new JsonObject();

        wrapper.add("pack", pack);

        Files.writeString(root.resolve("pack.mcmeta"), GSON.toJson(wrapper), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


    private static boolean isPreviewPackId(String id) {
        if (id == null) {
            return false;
        }

        return EXPECTED_PACK_ID.equals(id) || id.endsWith(PACK_FOLDER);
    }


    private static String sanitize(String value) {
        if (value == null) {
            return "font";
        }

        String result = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");

        return result.isBlank() ? "font" : result;
    }


    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }

        try (var walk = Files.walk(root)) {

            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();

            for (Path path : paths) {

                Files.deleteIfExists(path);
            }
        }
    }
}
