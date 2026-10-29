package com.benji.oasiso.common.tooltip;

import com.google.gson.*;

import java.util.*;

public final class TooltipData {
    public static final int MAX_JSON_CHARS = 262144;

    public record Palette(int first, int second, int third, int breath, int shine) {
    }

    public record Sparks(int auraDark, int auraBright, int coreDark, int coreBright, int flash) {
    }

    public record Fx(Palette palette, boolean shine, boolean glow, boolean italic, boolean bold, Sparks sparks) {
    }

    public record Text(String literal, String translate) {
    }

    public record Part(Text text, String style) {
    }

    public record Highlight(Text match, String fallback, String emptyFallback, String style, boolean all) {
    }

    public record Line(Text text, String textStyle, List<Part> parts, List<Highlight> highlights, String missingStyle,
                       boolean requireAll) {
    }

    public record Definition(String id, List<String> items, List<String> tags, int priority, Map<String, Fx> styles,
                             List<Line> lines, List<String> removeTranslations, boolean removeMatchingLines) {
    }

    private static final Set<String> PALETTE_FIELDS = Set.of("first", "second", "third", "breath", "shine");
    private static final Set<String> SPARK_FIELDS = Set.of("aura_dark", "aura_bright", "core_dark", "core_bright", "flash");

    public static List<Definition> parse(JsonObject files) {
        if (files.size() > 512) throw error("Too many tooltip files (maximum 512)");
        Map<String, Palette> palettes = new HashMap<>();
        Map<String, Sparks> sparks = new HashMap<>();
        for (var entry : files.entrySet()) {
            String id = resourceId(entry.getKey());
            try {
                String path = id.substring(id.indexOf(':') + 1);
                if (path.startsWith("palettes/")) palettes.put(id, palette(object(entry.getValue())));
                else if (path.startsWith("sparks/")) sparks.put(id, sparks(object(entry.getValue())));
                else if (!path.startsWith("items/")) throw error("Expected palettes/, sparks/ or items/ directory");
            } catch (RuntimeException e) {
                throw error(id + ": " + e.getMessage());
            }
        }
        List<Definition> result = new ArrayList<>();
        for (var entry : files.entrySet()) {
            String id = entry.getKey();
            if (!id.substring(id.indexOf(':') + 1).startsWith("items/")) continue;
            try {
                JsonObject o = object(entry.getValue());
                if (!bool(o, "enabled", true)) continue;
                fields(o, Set.of("enabled", "items", "tags", "priority", "styles", "lines", "remove_translations", "remove_matching_lines"));
                List<String> items = strings(o, "items", 256).stream().map(TooltipData::resourceId).toList();
                List<String> tags = strings(o, "tags", 256).stream().map(TooltipData::resourceId).toList();
                if (items.isEmpty() && tags.isEmpty()) throw error("Specify items or tags");
                Map<String, Fx> styles = new LinkedHashMap<>();
                JsonObject styleObjects = object(required(o, "styles"));
                if (styleObjects.size() > 64) throw error("Too many styles");
                for (var style : styleObjects.entrySet()) {
                    JsonObject s = object(style.getValue());
                    fields(s, Set.of("palette", "shine", "glow", "italic", "bold", "sparks"));
                    JsonElement p = required(s, "palette");
                    Palette palette = p.isJsonObject() ? palette(p.getAsJsonObject()) : palettes.get(resourceId(string(p)));
                    if (palette == null) throw error("Unknown palette for style " + style.getKey());
                    Sparks spark = null;
                    if (s.has("sparks") && !s.get("sparks").isJsonNull()) {
                        JsonElement v = s.get("sparks");
                        spark = v.isJsonObject() ? sparks(v.getAsJsonObject()) : sparks.get(resourceId(string(v)));
                        if (spark == null) throw error("Unknown sparks for style " + style.getKey());
                    }
                    styles.put(style.getKey(), new Fx(palette, bool(s, "shine", true), bool(s, "glow", false), bool(s, "italic", false), bool(s, "bold", false), spark));
                }
                List<Line> lines = new ArrayList<>();
                for (JsonElement element : array(required(o, "lines"), 32)) {
                    JsonObject line = object(element);
                    fields(line, Set.of("text", "text_style", "parts", "highlights", "missing_highlight_style", "require_all_highlights"));
                    List<Part> parts = new ArrayList<>();
                    List<Highlight> highlights = new ArrayList<>();
                    if (line.has("parts")) {
                        if (line.has("text") || line.has("highlights") || line.has("text_style") || line.has("missing_highlight_style") || line.has("require_all_highlights"))
                            throw error("parts cannot be combined with text/highlights settings");
                        for (JsonElement e : array(line.get("parts"), 32)) {
                            JsonObject part = object(e);
                            fields(part, Set.of("text", "style"));
                            parts.add(new Part(text(required(part, "text")), style(part, "style", styles)));
                        }
                        if (parts.isEmpty()) throw error("parts cannot be empty");
                        lines.add(new Line(null, null, List.copyOf(parts), List.of(), null, true));
                    } else {
                        Text text = text(required(line, "text"));
                        String base = style(line, "text_style", styles);
                        String missing = line.has("missing_highlight_style") ? style(line, "missing_highlight_style", styles) : base;
                        for (JsonElement e : optionalArray(line, "highlights", 32)) {
                            JsonObject h = object(e);
                            fields(h, Set.of("match", "fallback", "empty_fallback", "style", "all"));
                            highlights.add(new Highlight(text(required(h, "match")), h.has("fallback") ? string(h.get("fallback")) : "", h.has("empty_fallback") ? string(h.get("empty_fallback")) : "", style(h, "style", styles), bool(h, "all", false)));
                        }
                        lines.add(new Line(text, base, List.of(), List.copyOf(highlights), missing, bool(line, "require_all_highlights", true)));
                    }
                }
                int priority = o.has("priority") ? integer(o.get("priority")) : 0;
                result.add(new Definition(id, items, tags, priority, Map.copyOf(styles), List.copyOf(lines), strings(o, "remove_translations", 64), bool(o, "remove_matching_lines", false)));
            } catch (RuntimeException e) {
                throw error(id + ": " + e.getMessage());
            }
        }
        result.sort(Comparator.comparingInt(Definition::priority).reversed().thenComparing(Definition::id));
        return List.copyOf(result);
    }

    private static Palette palette(JsonObject o) {
        fields(o, PALETTE_FIELDS);
        return new Palette(color(o, "first"), color(o, "second"), color(o, "third"), color(o, "breath"), color(o, "shine"));
    }

    private static Sparks sparks(JsonObject o) {
        fields(o, SPARK_FIELDS);
        return new Sparks(color(o, "aura_dark"), color(o, "aura_bright"), color(o, "core_dark"), color(o, "core_bright"), color(o, "flash"));
    }

    private static Text text(JsonElement value) {
        JsonObject o = object(value);
        fields(o, Set.of("text", "translate"));
        if (o.has("text") == o.has("translate")) throw error("Text must contain exactly one of text or translate");
        return o.has("text") ? new Text(string(o.get("text")), null) : new Text(null, string(o.get("translate")));
    }

    private static String style(JsonObject o, String key, Map<String, Fx> styles) {
        String name = string(required(o, key));
        if (!styles.containsKey(name)) throw error("Unknown style: " + name);
        return name;
    }

    private static int color(JsonObject o, String key) {
        String hex = string(required(o, key));
        if (!hex.matches("#[0-9a-fA-F]{6}")) throw error(key + " must be #RRGGBB");
        return Integer.parseInt(hex.substring(1), 16);
    }

    private static List<String> strings(JsonObject o, String key, int maximum) {
        List<String> list = new ArrayList<>();
        for (JsonElement e : optionalArray(o, key, maximum)) list.add(string(e));
        return List.copyOf(list);
    }

    private static JsonArray optionalArray(JsonObject o, String key, int max) {
        return o.has(key) ? array(o.get(key), max) : new JsonArray();
    }

    private static JsonArray array(JsonElement e, int max) {
        if (!e.isJsonArray() || e.getAsJsonArray().size() > max)
            throw error("Expected an array of at most " + max + " entries");
        return e.getAsJsonArray();
    }

    private static JsonObject object(JsonElement e) {
        if (e == null || !e.isJsonObject()) throw error("Expected an object");
        return e.getAsJsonObject();
    }

    private static JsonElement required(JsonObject o, String name) {
        if (!o.has(name)) throw error("Missing field: " + name);
        return o.get(name);
    }

    private static String string(JsonElement e) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw error("Expected a string");
        String s = e.getAsString();
        if (s.length() > 2048) throw error("String exceeds 2048 characters");
        return s;
    }

    private static int integer(JsonElement e) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw error("Expected an integer");
        try {
            return e.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException ex) {
            throw error("Expected a 32-bit integer");
        }
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        if (!o.has(key)) return fallback;
        JsonElement e = o.get(key);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) throw error(key + " must be boolean");
        return e.getAsBoolean();
    }

    private static void fields(JsonObject o, Set<String> allowed) {
        for (String key : o.keySet()) if (!allowed.contains(key)) throw error("Unknown field: " + key);
    }

    private static String resourceId(String s) {
        if (!s.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw error("Expected namespace:path, got " + s);
        return s;
    }

    private static JsonParseException error(String message) {
        return new JsonParseException(message);
    }

    private TooltipData() {
    }
}
