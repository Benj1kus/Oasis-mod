package com.benji.oasiso.client.configscroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

final class ConfigScrollButton extends Button {
    private final IntSupplier color;
    private boolean hovering;
    private long hoverAt;
    ConfigScrollButton(int x, int y, int width, int height, String text, IntSupplier color, Runnable action) {
        super(x, y, width, height, Component.literal(text), b -> action.run(), DEFAULT_NARRATION);
        this.color = color;
    }
    @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
        boolean over = active && isMouseOver(mx, my);
        if (over && !hovering) { hoverAt = System.nanoTime(); ConfigScrollSound.hover(); }
        hovering = over;
        double elapsed = (System.nanoTime() - hoverAt) / 1.0e9;
        float bounce = over ? (float) (.04 * Math.sin(elapsed * 23) * Math.exp(-elapsed * 7) + .025) : 0;
        g.pose().pushPose();
        g.pose().translate(getX() + width * .5F, getY() + height * .5F, 0);
        g.pose().scale(1 + bounce, 1 + bounce * .5F, 1);
        g.pose().translate(-getX() - width * .5F, -getY() - height * .5F, 0);
        int accent = color.getAsInt();
        g.fill(getX(), getY(), getX() + width, getY() + height, ConfigScrollDraw.alpha(0x08161B, .90));
        if (over || isFocused()) g.fill(getX(), getY(), getX() + width, getY() + height, ConfigScrollDraw.alpha(accent, .16));
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, ConfigScrollDraw.alpha(accent, active ? .9 : .25));
        var font = Minecraft.getInstance().font;
        String text = font.plainSubstrByWidth(getMessage().getString(), width - 8);
        g.drawCenteredString(font, text, getX() + width / 2, getY() + (height - 8) / 2, active ? 0xFFE8F2EF : 0xFF687578);
        g.pose().popPose();
    }
    @Override public void playDownSound(SoundManager soundManager) { ConfigScrollSound.click(); }
}
