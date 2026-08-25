package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class DialogueRetroButton extends Button {

    private final boolean selected;
    private final boolean tabStyle;

    private DialogueRetroButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean selected, boolean tabStyle) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.selected = selected;
        this.tabStyle = tabStyle;
    }

    public static Builder retroBuilder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;

        boolean hover = isHoveredOrFocused();
        boolean enabled = this.active;

        graphics.fill(x0 + 2, y0 + 2, x1 + 2, y1 + 2, 0xA8000000);
        graphics.fill(x0, y0, x1, y1, DialogueRetroTheme.BLACK);

        if (!enabled) {
            graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFAAA58C, 0xFF85816D);
        } else if (tabStyle) {
            drawTabFace(graphics, x0, y0, x1, y1, hover);
        } else {
            drawActionFace(graphics, x0, y0, x1, y1, hover);
        }

        int hi = enabled ? 0xFFFFFFFF : 0xFFD0C9AC;
        int lo = enabled ? 0xFF575344 : 0xFF686554;

        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, hi);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, hi);
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, lo);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, lo);

        int alphaByte = Mth.ceil(this.alpha * 255.0F) << 24;
        int rgb;

        if (!enabled) {
            rgb = 0x555247;
        } else if (tabStyle) {
            rgb = selected ? 0xFFFFFF : 0xFF09E62D;
        } else {
            rgb = 0xFFFFFF;
        }

        int textColor = alphaByte | rgb;

        Minecraft minecraft = Minecraft.getInstance();
        String shown = minecraft.font.plainSubstrByWidth(getMessage().getString(), Math.max(8, width - 8));

        int textWidth = minecraft.font.width(shown);
        int textX = x0 + (width - textWidth) / 2;
        int textY = y0 + (height - 8) / 2;

        int shadowAlpha = alphaByte | 0xFF07690C;
        graphics.drawString(minecraft.font, shown, textX + 1, textY + 1, shadowAlpha, false);
        graphics.drawString(minecraft.font, shown, textX, textY, textColor, false);
    }

    private void drawTabFace(GuiGraphics graphics, int x0, int y0, int x1, int y1, boolean hover) {
        if (selected) {
            graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, DialogueRetroTheme.LIME, 0xFF79AF50);
            return;
        }

        if (hover) {
            graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFF5F1D8, 0xFFC9E6A1);
            return;
        }

        graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, DialogueRetroTheme.CREAM_LIGHT, DialogueRetroTheme.BEIGE);
    }

    private void drawActionFace(GuiGraphics graphics, int x0, int y0, int x1, int y1, boolean hover) {
        if (hover) {
            graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF8FC866, 0xFF527B3C);
        } else {
            graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, DialogueRetroTheme.GREEN_MID, 0xFF35552D);
        }
    }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private boolean selected;
        private boolean tabStyle;

        private Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public Builder tabStyle(boolean tabStyle) {
            this.tabStyle = tabStyle;
            return this;
        }

        public DialogueRetroButton build() {
            return new DialogueRetroButton(x, y, width, height, message, onPress, selected, tabStyle);
        }
    }
}
