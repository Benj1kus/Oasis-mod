package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DialogueEditorConfirmScreen extends Screen {

    private final Screen parent;
    private final String heading;
    private final String message;
    private final Runnable confirm;

    public DialogueEditorConfirmScreen(Screen parent, String heading, String message, Runnable confirm) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.message = message;
        this.confirm = confirm;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = height / 2 + 32;

        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            if (confirm != null) confirm.run();
        }).bounds(centerX - 104, y, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> minecraft.setScreen(parent)).bounds(centerX + 6, y, 98, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelW = Math.min(430, width - 40);
        int panelH = 126;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        graphics.fill(x, y, x + panelW, y + panelH, 0xF0151921);
        graphics.fill(x + 1, y + 1, x + panelW - 1, y + panelH - 1, 0xFF0D1218);
        graphics.drawCenteredString(font, heading, width / 2, y + 18, 0xFFFFD45A);

        int textY = y + 42;
        for (var line : font.split(Component.literal(message), panelW - 30)) {
            graphics.drawCenteredString(font, line, width / 2, textY, 0xFFB7C0CC);
            textY += font.lineHeight + 2;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
