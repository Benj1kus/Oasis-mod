package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class DialogueRetroSlider extends AbstractSliderButton {

    protected DialogueRetroSlider(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;

        graphics.fill(x0 + 2, y0 + 2, x1 + 2, y1 + 2, 0x99000000);
        graphics.fill(x0, y0, x1, y1, DialogueRetroTheme.BLACK);
        graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, DialogueRetroTheme.CREAM_LIGHT, DialogueRetroTheme.BEIGE);

        int trackY = y1 - 6;
        graphics.fill(x0 + 5, trackY, x1 - 5, trackY + 2, 0xFF3F4837);
        graphics.fill(x0 + 5, trackY + 2, x1 - 5, trackY + 3, 0xFFFFFFFF);

        int travel = Math.max(1, width - 12);
        int knobX = x0 + 5 + Mth.clamp((int) Math.round(this.value * travel), 0, travel);
        int knobColor = this.active ? (isHoveredOrFocused() ? DialogueRetroTheme.LIME : DialogueRetroTheme.LIME_SOFT) : 0xFF8E8B75;

        graphics.fill(knobX - 3, trackY - 3, knobX + 4, trackY + 5, DialogueRetroTheme.BLACK);
        graphics.fill(knobX - 2, trackY - 2, knobX + 3, trackY + 4, knobColor);
        graphics.fill(knobX - 2, trackY - 2, knobX + 3, trackY - 1, 0xFFFFFFFF);

        Minecraft minecraft = Minecraft.getInstance();
        String shown = minecraft.font.plainSubstrByWidth(getMessage().getString(), Math.max(8, width - 10));
        graphics.drawCenteredString(minecraft.font, shown, x0 + width / 2, y0 + 3, this.active ? DialogueRetroTheme.TEXT_ACCENT : DialogueRetroTheme.TEXT_DARK_MUTED);
    }
}
