package com.benji.oasiso.dialogue.editor;

import com.benji.oasiso.dialogue.data.DialogueDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DialogueConditionListScreen extends Screen {

    private static final List<String> TYPES = List.of("always", "player_tag", "source_tag", "score", "has_item", "dimension", "source_type", "mod_loaded", "quest_state");
    private static final List<String> OPERATORS = List.of(">=", ">", "==", "!=", "<=", "<");

    private final Screen parent;
    private final DialogueEditorProject project;
    private final List<DialogueDefinition.Condition> conditions;
    private final String heading;

    private int selected;

    private int panelW;
    private int left;
    private int innerW;
    private final List<FieldLabel> fieldLabels = new ArrayList<>();

    public DialogueConditionListScreen(Screen parent, DialogueEditorProject project, List<DialogueDefinition.Condition> conditions, String heading) {
        super(Component.literal("Dialogue Studio - Conditions"));

        this.parent = parent;
        this.project = project;
        this.conditions = conditions;
        this.heading = heading != null ? heading : "Conditions";

        if (this.conditions.isEmpty()) {
            this.conditions.add(new DialogueDefinition.Condition());
        }
    }

    @Override
    protected void init() {
        fieldLabels.clear();

        selected = Math.max(0, Math.min(selected, conditions.size() - 1));

        DialogueDefinition.Condition condition = conditions.get(selected);

        panelW = Math.min(620, width - 24);

        left = (width - panelW) / 2;

        innerW = panelW - 32;

        int y = 54;

        buildNavigator(y);
        y += 34;

        addRenderableWidget(Button.builder(Component.literal("Rule type: " + friendlyType(condition.type)), button -> {
            condition.type = next(condition.type, TYPES);

            normalizeForType(condition);
            rebuild();
        }).bounds(left + 16, y, innerW / 2 - 4, 20).build());

        addRenderableWidget(Button.builder(Component.literal(condition.invert ? "NOT / Invert: ON" : "NOT / Invert: OFF"), button -> {
            condition.invert = !condition.invert;

            rebuild();
        }).bounds(left + 20 + innerW / 2, y, innerW / 2 - 4, 20).build());

        y += 142;

        y = buildFieldsForType(condition, y);

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            DialogueEditorHistory.checkpoint(project);
            minecraft.setScreen(parent);
        }).bounds(left + 16, height - 30, innerW, 20).build());
    }

    private void buildNavigator(int y) {
        int x = left + 16;

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            selected = Math.max(0, selected - 1);
            rebuild();
        }).bounds(x, y, 40, 20).build());

        x += 44;

        addRenderableWidget(Button.builder(Component.literal("Rule " + (selected + 1) + " / " + conditions.size()), button -> {
        }).bounds(x, y, 146, 20).build());

        x += 150;

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            selected = Math.min(conditions.size() - 1, selected + 1);
            rebuild();
        }).bounds(x, y, 40, 20).build());

        x += 48;

        addRenderableWidget(Button.builder(Component.literal("+ Add rule"), button -> {
            conditions.add(new DialogueDefinition.Condition());

            selected = conditions.size() - 1;

            rebuild();
        }).bounds(x, y, 94, 20).build());

        x += 98;

        addRenderableWidget(Button.builder(Component.literal("- Remove"), button -> {
            if (conditions.size() > 1) {
                conditions.remove(selected);

                selected = Math.min(selected, conditions.size() - 1);

                rebuild();
            } else {
                DialogueDefinition.Condition only = conditions.get(0);

                only.type = "always";
                only.id = null;
                only.objective = null;
                only.invert = false;

                rebuild();
            }
        }).bounds(x, y, 94, 20).build());
    }

    private int buildFieldsForType(DialogueDefinition.Condition condition, int y) {
        String type = condition.type != null ? condition.type.toLowerCase(Locale.ROOT) : "always";

        switch (type) {
            case "score" -> {
                y = addField(y, "Scoreboard objective", condition.objective, 128, value -> condition.objective = blankToNull(value));

                int rowY = y;

                fieldLabels.add(new FieldLabel("Comparison operator", left + 16, rowY - 11));

                fieldLabels.add(new FieldLabel("Score value", left + 20 + innerW / 2, rowY - 11));

                addRenderableWidget(Button.builder(Component.literal("Compare: " + nullToDefault(condition.operator, ">=")), button -> {
                    condition.operator = next(nullToDefault(condition.operator, ">="), OPERATORS);

                    rebuild();
                }).bounds(left + 16, rowY, innerW / 2 - 4, 20).build());

                EditBox value = new EditBox(font, left + 20 + innerW / 2, rowY, innerW / 2 - 4, 20, Component.literal("Score value"));

                value.setValue(String.valueOf(condition.value));

                value.setResponder(text -> {
                    try {
                        condition.value = Integer.parseInt(text.trim());
                    } catch (Exception ignored) {
                    }
                });

                addRenderableWidget(value);
                y += 36;
            }

            case "has_item" -> {
                y = addField(y, "Item registry id or #item_tag", condition.id, 256, value -> condition.id = blankToNull(value));

                y = addIntegerField(y, "Minimum amount in player's inventory", condition.count, value -> condition.count = Math.max(1, value));
            }

            case "player_tag" ->
                    y = addField(y, "Player tag", condition.id, 256, value -> condition.id = blankToNull(value));

            case "source_tag" ->
                    y = addField(y, "Dialogue source entity tag", condition.id, 256, value -> condition.id = blankToNull(value));

            case "dimension" ->
                    y = addField(y, "Dimension id  (example: minecraft:overworld)", condition.id, 256, value -> condition.id = blankToNull(value));

            case "source_type" ->
                    y = addField(y, "Source entity id or #entity_type tag", condition.id, 256, value -> condition.id = blankToNull(value));

            case "mod_loaded" ->
                    y = addField(y, "Mod id  (example: netherman)", condition.id, 128, value -> condition.id = blankToNull(value));

            case "quest_state" -> {
                y = addField(y, "Quest id  (example: mydialogues:temple_quest)", condition.id, 256, value -> condition.id = blankToNull(value));

                int rowY = y;

                fieldLabels.add(new FieldLabel("Required quest state", left + 16, rowY - 11));

                addRenderableWidget(Button.builder(Component.literal("State: " + nullToDefault(condition.state, "active")), button -> {
                    condition.state = next(nullToDefault(condition.state, "active"), List.of("not_started", "active", "completed", "failed", "started"));

                    rebuild();
                }).bounds(left + 16, rowY, innerW, 20).build());

                y += 36;
            }

            default -> {
            }
        }

        return y;
    }

    private int addField(int y, String label, String value, int maxLength, java.util.function.Consumer<String> responder) {
        fieldLabels.add(new FieldLabel(label, left + 16, y - 11));

        EditBox box = new EditBox(font, left + 16, y, innerW, 20, Component.literal(label));

        box.setMaxLength(maxLength);
        box.setValue(value != null ? value : "");
        box.setResponder(responder);

        addRenderableWidget(box);

        return y + 36;
    }

    private int addIntegerField(int y, String label, int initial, java.util.function.IntConsumer responder) {
        fieldLabels.add(new FieldLabel(label, left + 16, y - 11));

        EditBox box = new EditBox(font, left + 16, y, innerW, 20, Component.literal(label));

        box.setValue(String.valueOf(initial));

        box.setResponder(text -> {
            try {
                responder.accept(Integer.parseInt(text.trim()));
            } catch (Exception ignored) {
            }
        });

        addRenderableWidget(box);

        return y + 36;
    }

    private void normalizeForType(DialogueDefinition.Condition condition) {
        if (condition.operator == null || condition.operator.isBlank()) {
            condition.operator = ">=";
        }

        condition.count = Math.max(1, condition.count);
    }

    private void rebuild() {
        minecraft.setScreen(this);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(left, 16, left + panelW, height - 8, 0xF010151D);

        graphics.drawString(font, heading, left + 16, 24, 0xFF6FF8E9, false);

        DialogueDefinition.Condition condition = conditions.get(Math.max(0, Math.min(selected, conditions.size() - 1)));

        int cardY = 112;
        int cardH = 66;

        graphics.fill(left + 16, cardY, left + 16 + innerW, cardY + cardH, 0xD0171D24);

        graphics.fill(left + 16, cardY, left + 20, cardY + cardH, 0xFFD8E36A);

        graphics.drawString(font, "THIS RULE MEANS:", left + 26, cardY + 8, 0xFFFFD45A, false);

        int y = drawWrapped(graphics, humanSentence(condition), left + 26, cardY + 24, innerW - 20, 0xFFFFFFFF);

        String type = condition.type != null ? condition.type.toLowerCase(Locale.ROOT) : "always";

        int helpY = cardY + cardH + 12;

        helpY = drawWrapped(graphics, typeHelp(type), left + 16, helpY, innerW, 0xFF87909D);

        if (conditions.size() > 1) {
            graphics.drawString(font, "AND: all " + conditions.size() + " rules must be true", left + 16, helpY + 6, 0xFF67F0E6, false);
        }

        for (FieldLabel label : fieldLabels) {
            graphics.drawString(font, label.text, label.x, label.y, 0xFF9EA8B5, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String humanSentence(DialogueDefinition.Condition condition) {
        if (condition == null) {
            return "ALWAYS TRUE";
        }

        String type = condition.type != null ? condition.type.toLowerCase(Locale.ROOT) : "always";

        String sentence = switch (type) {
            case "player_tag" -> "player has tag \"" + safe(condition.id) + "\"";

            case "source_tag" -> "dialogue source has tag \"" + safe(condition.id) + "\"";

            case "score" ->
                    "player score " + safe(condition.objective) + " " + nullToDefault(condition.operator, ">=") + " " + condition.value;

            case "has_item" -> "player has at least " + Math.max(1, condition.count) + " × " + safe(condition.id);

            case "dimension" -> "player is in " + safe(condition.id);

            case "source_type" -> "dialogue source entity is " + safe(condition.id);

            case "mod_loaded" -> "mod \"" + safe(condition.id) + "\" is loaded";

            case "quest_state" -> "quest " + safe(condition.id) + " is " + nullToDefault(condition.state, "active");

            default -> "always true";
        };

        return condition.invert ? "NOT ( " + sentence + " )" : sentence;
    }

    private String typeHelp(String type) {
        return switch (type) {
            case "player_tag" -> "PLAYER TAG: useful for story flags set with vanilla entity/scoreboard tags.";
            case "source_tag" -> "SOURCE TAG: checks the NPC/boss/entity that owns this dialogue session.";
            case "score" -> "SCORE: compares a vanilla scoreboard objective with the number you enter.";
            case "has_item" ->
                    "HAS ITEM: checks the player's full inventory. Registry ids and #item tags are supported.";
            case "dimension" -> "DIMENSION: true only in the specified dimension, e.g. minecraft:overworld.";
            case "source_type" -> "SOURCE TYPE: matches the dialogue source EntityType or #entity_type tag.";
            case "mod_loaded" -> "MOD LOADED: useful for compatibility branches when another mod is installed.";
            case "quest_state" ->
                    "QUEST STATE: checks Dialogue Engine's persistent quest lifecycle: not_started, active, completed, failed, or started.";
            default -> "ALWAYS: this rule never blocks the path. Use it when you want an unconditional option/branch.";
        };
    }

    private String friendlyType(String type) {
        if (type == null) return "Always";

        return switch (type.toLowerCase(Locale.ROOT)) {
            case "player_tag" -> "Player has tag";
            case "source_tag" -> "Source has tag";
            case "score" -> "Score comparison";
            case "has_item" -> "Player has item";
            case "dimension" -> "Player dimension";
            case "source_type" -> "Source entity type";
            case "mod_loaded" -> "Mod is installed";
            case "quest_state" -> "Quest state";
            default -> "Always true";
        };
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int pixelWidth, int color) {
        for (String line : wrap(text, pixelWidth)) {
            graphics.drawString(font, line, x, y, color, false);

            y += 12;
        }

        return y;
    }

    private List<String> wrap(String text, int pixelWidth) {
        List<String> result = new ArrayList<>();

        if (text == null || text.isBlank()) {
            result.add("");
            return result;
        }

        String[] words = text.split("\\s+");

        String current = "";

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;

            if (font.width(candidate) <= pixelWidth) {
                current = candidate;
                continue;
            }

            if (!current.isEmpty()) {
                result.add(current);
            }

            current = word;
        }

        if (!current.isEmpty()) {
            result.add(current);
        }

        return result;
    }

    private String trim(String value, int pixelWidth) {
        if (value == null) return "";

        if (font.width(value) <= pixelWidth) {
            return value;
        }

        return font.plainSubstrByWidth(value, Math.max(0, pixelWidth - font.width("..."))) + "...";
    }

    private static String next(String current, List<String> values) {
        int index = values.indexOf(current);

        return values.get(index < 0 ? 0 : (index + 1) % values.size());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static String safe(String value) {
        return value != null && !value.isBlank() ? value : "?";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record FieldLabel(String text, int x, int y) {
    }
}
