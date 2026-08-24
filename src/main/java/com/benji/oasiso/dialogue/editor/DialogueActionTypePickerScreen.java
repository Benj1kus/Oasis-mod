package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class DialogueActionTypePickerScreen extends Screen {

    private record Entry(String id, String title, String help, int color) {
    }

    private static final List<Entry> ENTRIES = List.of(new Entry("give_item", "Give item", "Give an item stack to the player.", 0xFF4F8C5C), new Entry("take_item", "Take item", "Remove items from the player's inventory.", 0xFF4F8C5C),

            new Entry("quest_start", "Start quest", "Mark a Dialogue Engine quest as ACTIVE.", 0xFFB88A37), new Entry("quest_complete", "Complete quest", "Mark a quest as COMPLETED.", 0xFFB88A37), new Entry("quest_fail", "Fail quest", "Mark a quest as FAILED.", 0xFFB88A37), new Entry("quest_reset", "Reset quest", "Return a quest to NOT STARTED.", 0xFFB88A37),
            new Entry("add_player_tag", "Add tag", "Add a vanilla entity tag to player or dialogue source.", 0xFF547EAA), new Entry("remove_player_tag", "Remove tag", "Remove a vanilla entity tag.", 0xFF547EAA), new Entry("set_score", "Set score", "Set a scoreboard objective to an exact value.", 0xFF547EAA), new Entry("add_score", "Add score", "Add or subtract from a scoreboard objective.", 0xFF547EAA), new Entry("teleport", "Teleport", "Move player or dialogue source.", 0xFF547EAA), new Entry("kill", "Kill entity/player", "Kill player or dialogue source.", 0xFFB14E55),
            new Entry("play_sound", "Play sound", "Play a sound for the dialogue player.", 0xFF704C86), new Entry("particle", "Particle", "Spawn particles around player or dialogue source.", 0xFF704C86),
            new Entry("fire_external", "Fire external event", "Notify Java/another mod with DialogueNodeExternalEvent.", 0xFF2F8C8C), new Entry("run_command", "Run command", "Run a trusted server command from the dialogue graph.", 0xFF8C5F2F));

    private final Screen parent;
    private final Consumer<String> callback;

    public DialogueActionTypePickerScreen(Screen parent, Consumer<String> callback) {
        super(Component.literal("Dialogue Studio - Action Type"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int panelW = Math.min(760, width - 24);
        int left = (width - panelW) / 2;
        int inner = panelW - 32;
        int colW = (inner - 8) / 2;

        int y = 58;

        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);

            int col = i % 2;
            int row = i / 2;

            int x = left + 16 + col * (colW + 8);
            int by = y + row * 34;

            addRenderableWidget(Button.builder(Component.literal(entry.title), button -> {
                callback.accept(entry.id);
                minecraft.setScreen(parent);
            }).bounds(x, by, colW, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> minecraft.setScreen(parent)).bounds(left + 16, height - 30, inner, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelW = Math.min(760, width - 24);
        int left = (width - panelW) / 2;
        int inner = panelW - 32;
        int colW = (inner - 8) / 2;

        graphics.fill(left, 12, left + panelW, height - 8, 0xF010151D);
        graphics.drawString(font, "CHOOSE WHAT THIS ACTION DOES", left + 16, 22, 0xFF6FF8E9, false);
        graphics.drawString(font, "Items / quests / entity state / world effects / integration", left + 16, 36, 0xFF8E9AA8, false);

        int y = 58;

        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);

            int col = i % 2;
            int row = i / 2;

            int x = left + 16 + col * (colW + 8);
            int by = y + row * 34;

            graphics.fill(x, by + 21, x + colW, by + 32, 0xA0141820);
            graphics.fill(x, by + 21, x + 3, by + 32, entry.color);

            graphics.drawString(font, trim(entry.help, colW - 10), x + 6, by + 23, 0xFF8E9AA8, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String trim(String value, int width) {
        if (font.width(value) <= width) return value;

        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
