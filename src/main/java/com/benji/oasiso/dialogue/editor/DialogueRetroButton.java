package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class DialogueRetroButton extends Button {

    private static final long HOVER_JELLY_MS = 310L;
    private static final long PRESS_JELLY_MS = 430L;

    private final boolean selected;
    private final boolean tabStyle;
    private final OnPress actualOnPress;

    private boolean pressActionPending;

    private boolean wasMouseHovered;
    private long hoverStartedAt = -1L;
    private long pressStartedAt = -1L;
    private long lastRenderAt = -1L;
    private float hoverAmount;

    private DialogueRetroButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean selected, boolean tabStyle) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);

        this.selected = selected;
        this.tabStyle = tabStyle;
        this.actualOnPress = onPress;
    }

    public static Builder retroBuilder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void onPress() {
        this.pressStartedAt = System.currentTimeMillis();

        if (this.pressActionPending) {
            return;
        }

        this.pressActionPending = true;

        Minecraft minecraft = Minecraft.getInstance();

        Screen screenAtPress = minecraft.screen;
        CompletableFuture.delayedExecutor(105L, TimeUnit.MILLISECONDS).execute(() -> minecraft.execute(() -> {
            this.pressActionPending = false;
            if (minecraft.screen != screenAtPress) {
                return;
            }

            this.actualOnPress.onPress(this);
        }));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();

        boolean mouseHover = this.active && this.isMouseOver(mouseX, mouseY);

        if (mouseHover && !this.wasMouseHovered) {

            this.hoverStartedAt = now;
            DialogueJuiceSound.hoverClick();
        }

        this.wasMouseHovered = mouseHover;

        updateHoverAmount(now, mouseHover);

        float hoverJelly = hoverJelly(now);
        float pressJelly = pressJelly(now);

        float scaleX = 1.0F + this.hoverAmount * 0.010F + hoverJelly + pressJelly;
        float scaleY = 1.0F + this.hoverAmount * 0.007F - hoverJelly * 0.32F + pressJelly * 0.72F;

        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;

        float centerX = x0 + width * 0.5F;

        float centerY = y0 + height * 0.5F;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scaleX, scaleY, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);

        boolean hover = isHoveredOrFocused();

        boolean enabled = this.active;

        int shadowGrow = mouseHover ? 1 : 0;

        graphics.fill(x0 + 2 - shadowGrow, y0 + 2, x1 + 2 + shadowGrow, y1 + 2 + shadowGrow, 0xA8000000);

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

        drawPressFlash(graphics, x0, y0, x1, y1, now);

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

        int textJellyY = Math.round(-hoverJelly * 18.0F - pressJelly * 12.0F);
        int shadowAlpha = alphaByte | 0xFF07690C;

        graphics.drawString(minecraft.font, shown, textX + 1, textY + 1 + textJellyY, shadowAlpha, false);

        graphics.drawString(minecraft.font, shown, textX, textY + textJellyY, textColor, false);

        graphics.pose().popPose();
    }

    private void updateHoverAmount(long now, boolean hovered) {
        if (this.lastRenderAt < 0L) {
            this.lastRenderAt = now;
        }

        long elapsed = Math.min(60L, Math.max(0L, now - this.lastRenderAt));

        this.lastRenderAt = now;
        float target = hovered ? 1.0F : 0.0F;
        float response = 1.0F - (float) Math.exp(-elapsed * 0.020D);

        this.hoverAmount += (target - this.hoverAmount) * response;
    }

    private float hoverJelly(long now) {
        if (this.hoverStartedAt < 0L) {
            return 0.0F;
        }

        float t = (now - this.hoverStartedAt) / (float) HOVER_JELLY_MS;

        if (t < 0.0F || t >= 1.0F) {

            return 0.0F;
        }

        float decay = (float) Math.exp(-3.8F * t);
        return Mth.sin(t * Mth.PI * 3.2F) * decay * 0.030F;
    }

    private float pressJelly(long now) {
        if (this.pressStartedAt < 0L) {
            return 0.0F;
        }

        float t = (now - this.pressStartedAt) / (float) PRESS_JELLY_MS;

        if (t < 0.0F || t >= 1.0F) {

            return 0.0F;
        }

        float decay = (float) Math.exp(-4.8F * t);
        return (0.032F + Mth.sin(t * Mth.PI * 4.0F) * 0.052F) * decay;
    }

    private void drawPressFlash(GuiGraphics graphics, int x0, int y0, int x1, int y1, long now) {
        if (this.pressStartedAt < 0L) {
            return;
        }

        float t = (now - this.pressStartedAt) / 235.0F;

        if (t < 0.0F || t >= 1.0F) {

            return;
        }

        float fade = 1.0F - t;

        fade *= fade;

        int alpha = Mth.clamp(Math.round(fade * 118.0F), 0, 118);
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, alpha << 24 | 0x00FFFFFF);
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
