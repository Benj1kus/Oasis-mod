package com.benji.oasiso.client.store;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ScienceHeartItem;
import com.benji.oasiso.common.store.ScienceStoreNetwork;
import com.benji.oasiso.common.store.ScienceStoreOffers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class ScienceStoreScreen extends Screen {
    private static final ResourceLocation DOWN = texture("store_bg_down"), UP = texture("store_bg_up"), CHAR = texture("store_bg_char"), LAYER1 = texture("store_bg_char_layer1"), LAYER2 = texture("store_bg_char_layer2"), OVERLAY = texture("store_bg_overlay"), SELECTED = texture("card_selected"), UNSELECTED = texture("card_notselected");
    private final UUID session;
    private final InteractionHand hand;
    private final ScienceStoreIcons icons = new ScienceStoreIcons();
    private final long opened = Util.getMillis();
    private final Card[] cards = {new Card(), new Card(), new Card()};
    private int selected = 1, sequence, balance;
    private boolean holding, awaiting, retry, releaseRequired = true, closed;
    private double holdStarted, nextRetry, flashAt = -100, messageUntil;
    private boolean closing;
    private double closeStarted, closeFrom;
    private static final double INTRO_END = 2.2;
    private Component message = Component.empty();
    private int messageColor = 0xF6B7E8;
    private float scale, originX, originY;
    private ScienceStoreMusic music;

    private static final class Card {
        float x, y, angle, fromX, fromY, fromAngle, toX, toY, toAngle;
        double changed = -100;
    }

    public ScienceStoreScreen(UUID session, InteractionHand hand, int balance) {
        super(Component.translatable("gui.oasiso.science_store.title"));
        this.session = session;
        this.hand = hand;
        this.balance = balance;
        layout(true);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/gui/science_store/" + name + ".png");
    }

    private double time() {
        return (Util.getMillis() - opened) / 1000.0;
    }

    private double animationTime() {
        return closing ? Math.max(0, closeFrom - (time() - closeStarted)) : time();
    }

    private static float clamp(double n) {
        return (float) Math.max(0, Math.min(1, n));
    }

    private static float smooth(double n) {
        float v = clamp(n);
        return v * v * (3 - 2 * v);
    }

    private static float out(double n) {
        float v = 1 - clamp(n);
        return 1 - v * v * v;
    }

    @Override
    protected void init() {
        scale = Math.min(width / 192F, height / 108F);
        originX = (width - 192 * scale) / 2F;
        originY = (height - 108 * scale) / 2F;

        if (minecraft != null && minecraft.getSoundManager() != null) {
            if (music == null || !minecraft.getSoundManager().isActive(music)) {
                music = new ScienceStoreMusic(0.35f);
                minecraft.getSoundManager().play(music);
            }
        }
    }

    private void layout(boolean initial) {
        double t = time();
        for (int i = 0; i < cards.length; i++) {
            Card c = cards[i];
            update(c, t);
            int slot = (i - selected + 3) % 3;
            c.fromX = c.x;
            c.fromY = c.y;
            c.fromAngle = c.angle;
            c.toX = slot == 0 ? 61 : slot == 1 ? 96 : 29;
            c.toY = slot == 0 ? 35 : 43;
            c.toAngle = slot == 0 ? 0 : slot == 1 ? 23 : -25;
            c.changed = initial ? -100 : t;
            if (initial) {
                c.x = c.toX;
                c.y = c.toY;
                c.angle = c.toAngle;
            }
        }
    }

    private static void update(Card c, double t) {
        float f = smooth((t - c.changed) / .42);
        c.x = c.fromX + (c.toX - c.fromX) * f;
        c.y = c.fromY + (c.toY - c.fromY) * f;
        c.angle = c.fromAngle + (c.toAngle - c.fromAngle) * f;
    }

    private float bob(int i, double t) {
        return (float) Math.sin(t * (1.35 + i * .31) + i * 2.15) * .85F;
    }

    private boolean ready() {
        return !closing && time() >= INTRO_END && time() - cards[selected].changed >= .42;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (closing) {
            if (animationTime() <= 0 || minecraft == null || minecraft.player == null || !minecraft.player.isAlive())
                finishClose();
            return;
        }
        if (minecraft == null || minecraft.player == null || !minecraft.player.isAlive() || !(minecraft.player.getItemInHand(hand).getItem() instanceof ScienceHeartItem)) {
            onClose();
            return;
        }
        balance = com.benji.oasiso.common.store.ScienceStoreTransaction.balance(minecraft.player.getInventory(), ScienceStoreOffers.currency());
        boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (!down) {
            releaseRequired = false;
            if (holding) cancel();
        }
        if (holding && (!minecraft.isWindowActive() || !ready())) cancel();
        double t = time();
        if (holding && t - holdStarted >= 1.05 && !awaiting && (!retry || t >= nextRetry)) {
            awaiting = true;
            retry = false;
            ScienceStoreNetwork.send(session, sequence, ScienceStoreNetwork.BUY, selected);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        double t = time();
        double animation = animationTime();
        float uiOpacity = smooth((animation - 2.02) / .18);
        icons.update(g, t);
        drawOverlay(g, animation);
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        g.pose().scale(scale, scale, 1);
        float left = -originX / scale;
        float right = (width - originX) / scale - 192;
        float top = -originY / scale;
        float bottom = (height - originY) / scale - 108;
        layer(g, UP, left, top - 112 * (1 - out((animation - .92) / .43)) - (float) (1 - Math.cos(t * .77)) * .5F, animation, .92);
        layer(g, LAYER2, 205 * (1 - out((animation - .72) / .44)) + (float) Math.sin(t * .92 + 1.4) * 1.3F, (float) Math.sin(t * .66 + 2) * .65F, animation, .72);
        layer(g, LAYER1, 205 * (1 - out((animation - .56) / .44)) + (float) Math.sin(t * 1.17) * .95F, (float) Math.sin(t * .83 + .7) * .55F, animation, .56);
        layer(g, CHAR, 205 * (1 - out((animation - .38) / .46)), 0, animation, .38);
        layer(g, DOWN, right, bottom + 112 * (1 - out((animation - .18) / .47)) + (float) (1 - Math.cos(t * .91)) * .6F, animation, .18);
        for (int i = 0; i < cards.length; i++) update(cards[i], t);
        for (int i = 0; i < cards.length; i++) if (i != selected) drawCard(g, i, t, animation);
        drawCard(g, selected, t, animation);
        if (uiOpacity > 0) labels(g, t, uiOpacity);
        g.pose().popPose();
        g.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (uiOpacity > 0) status(g, t, uiOpacity);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawOverlay(GuiGraphics g, double t) {
        g.flush();
        RenderSystem.setShaderTexture(0, OVERLAY);
        if (ScienceStoreShaders.reveal != null) {
            ScienceStoreShaders.reveal.safeGetUniform("Progress").set(clamp(t / .34));
            ScienceStoreShaders.reveal.safeGetUniform("Aspect").set((float) width / height);
            ScienceStoreDrawing.quad(g, ScienceStoreShaders.reveal, 0, 0, width, height, 0, 0, 1, 1, 1);
        } else {
            ScienceStoreDrawing.quad(g, GameRenderer.getPositionTexColorShader(), 0, 0, width, height, 0, 0, 1, 1, clamp(t / .34));
        }
    }

    private void layer(GuiGraphics g, ResourceLocation texture, float x, float y, double t, double start) {
        if (t < start) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        image(g, texture, 0, 0, 192, 108, clamp((t - start) / .12));
        g.pose().popPose();
    }

    private static void image(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, float alpha) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, alpha);
        g.blit(texture, x, y, 0F, 0F, w, h, w, h);
        g.flush();
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private void drawCard(GuiGraphics g, int i, double t, double animation) {
        double start = 1.40 + i * .13;
        if (animation < start) return;
        Card c = cards[i];
        float enter = out((animation - start) / .48);
        float amount = i == selected && holding ? clamp((t - holdStarted) / 1.05) : 0;
        float shakeX = (float) (Math.sin(t * 91) + Math.sin(t * 137) * .4) * amount * 1.1F;
        float shakeY = (float) Math.cos(t * 113) * amount * .65F;
        g.pose().pushPose();
        g.pose().translate(c.x - 150 * (1 - enter) + shakeX, c.y + bob(i, t) + shakeY, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(c.angle + (float) Math.sin(t * 97) * amount * 2));
        image(g, i == selected ? SELECTED : UNSELECTED, -17, -25, 34, 50, enter);
        if (i == selected) border(g, t, enter);
        float flash = i == selected ? clamp(1 - (t - flashAt) / .30) : 0;
        icons.draw(g, i, -11, -15, 22, t, flash, enter);
        if (amount > 0) {
            g.fill(-13, 21, 13, 23, 0xCC200D35);
            g.fill(-13, 21, -13 + (int) (26 * amount), 23, 0xFF5BFFF6);
        }
        if (flash > 0) g.fill(-17, -25, 17, 25, ((int) (flash * 235) << 24) | 0xFFFFFF);
        g.pose().popPose();
    }

    private static void border(GuiGraphics g, double t, float opacity) {
        float pulse = .5F + .5F * (float) Math.sin(t * 2.6);
        float thickness = .45F + .40F * pulse;
        int color = ((int) (opacity * (165 + 90 * pulse)) << 24) | 0x5BFFF6;
        rect(g, -17, -25, 34, thickness, color);
        rect(g, -17, 25 - thickness, 34, thickness, color);
        rect(g, -17, -25 + thickness, thickness, 50 - 2 * thickness, color);
        rect(g, 17 - thickness, -25 + thickness, thickness, 50 - 2 * thickness, color);
    }

    private static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(w, h, 1);
        g.fill(0, 0, 1, 1, color);
        g.pose().popPose();
    }

    private static int fade(int color, float opacity) {
        return ((int) ((color >>> 24) * opacity) << 24) | (color & 0xFFFFFF);
    }

    private void labels(GuiGraphics g, double t, float opacity) {
        Card c = cards[selected];
        float y = c.y + bob(selected, t) + 29;
        g.pose().pushPose();
        g.pose().translate(c.x, y, 0);
        Component name = ScienceStoreOffers.stack(selected).getHoverName();
        float nameScale = Math.min(.55F, 38F / Math.max(1, font.width(name)));
        float nameTop = -1 + (6 - (font.lineHeight - 1) * nameScale) * .5F;
        g.fill(-20, -1, 20, 5, fade(0xEA270D30, opacity));
        text(g, name, 0, nameTop, nameScale, fade(0xFFF080D7, opacity), 38);
        String price = Integer.toString(ScienceStoreOffers.price(selected));
        g.fill(-13, 8, 13, 19, fade(0xDB160F1E, opacity));
        icons.draw(g, 3, -11, 8, 11, t, 0, opacity);
        text(g, Component.literal(price), 6, 11, .75F, fade(balance >= ScienceStoreOffers.price(selected) ? 0xFFF080D7 : 0xFFFF7575, opacity), 12);
        g.pose().popPose();
        icons.draw(g, 3, 3, 2, 10, t, 0, opacity);
        text(g, Component.literal(Integer.toString(balance)), 18, 5, .6F, fade(0xFF5BFFF6, opacity), 20);
        float closeX = (width - originX) / scale - 6;
        float closeY = 5 - originY / scale;
        text(g, Component.literal("×"), closeX, closeY, .85F, fade(0xFF5BFFF6, opacity), 7);
    }

    private void status(GuiGraphics g, double t, float opacity) {
        Component hint = t < messageUntil ? message : Component.translatable("gui.oasiso.science_store.hold");
        int color = fade(0xFF000000 | (t < messageUntil ? messageColor : 0xD9A5D5), opacity);
        float margin = 5 * scale;
        float maxWidth = Math.min(112 * scale, width - margin * 2);
        float size = Math.min(.48F * scale, maxWidth / Math.max(1, font.width(hint)));
        g.pose().pushPose();
        g.pose().translate(width - margin, height - margin - font.lineHeight * size, 0);
        g.pose().scale(size, size, 1);
        if ((color >>> 24) >= 4) g.drawString(font, hint, -font.width(hint), 0, color, true);
        g.pose().popPose();
    }

    private void text(GuiGraphics g, Component text, float x, float y, float size, int color, float maxWidth) {
        float actual = Math.min(size, maxWidth / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(actual, actual, 1);
        if ((color >>> 24) >= 4) g.drawString(font, text, -font.width(text) / 2, 0, color, false);
        g.pose().popPose();
    }

    private int hit(double mouseX, double mouseY) {
        double t = time();
        if (contains(selected, mouseX, mouseY, t)) return selected;
        for (int i = 2; i >= 0; i--) if (i != selected && contains(i, mouseX, mouseY, t)) return i;
        return -1;
    }

    private boolean contains(int i, double mouseX, double mouseY, double t) {
        Card c = cards[i];
        update(c, t);
        double dx = (mouseX - originX) / scale - c.x;
        double dy = (mouseY - originY) / scale - c.y - bob(i, t);
        double rad = Math.toRadians(c.angle);
        double x = dx * Math.cos(rad) + dy * Math.sin(rad);
        double y = -dx * Math.sin(rad) + dy * Math.cos(rad);
        return Math.abs(x) <= 17 && Math.abs(y) <= 25;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (closing) return true;
        if (button != 0 && button != 1) return super.mouseClicked(x, y, button);
        if (x >= width - 12 * scale && x <= width && y >= 0 && y <= 14 * scale) {
            onClose();
            return true;
        }
        if (!ready() || holding || awaiting) return true;
        int card = hit(x, y);
        if (card < 0) return true;
        if (card != selected) {
            selected = card;
            layout(false);
            releaseRequired = button == 1;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1F));
        } else if (button == 1 && !releaseRequired) {
            releaseRequired = true;
            if (balance < ScienceStoreOffers.price(selected)) {
                show("gui.oasiso.science_store.not_enough", 0xFF7575);
                return true;
            }
            sequence++;
            holding = true;
            retry = false;
            holdStarted = time();
            ScienceStoreNetwork.send(session, sequence, ScienceStoreNetwork.BEGIN, selected);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 1) {
            releaseRequired = false;
            cancel();
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (holding && !contains(selected, x, y, time())) cancel();
        super.mouseMoved(x, y);
    }

    private void cancel() {
        if (holding || retry) ScienceStoreNetwork.send(session, sequence, ScienceStoreNetwork.CANCEL, selected);
        holding = false;
        retry = false;
    }

    public void reply(ScienceStoreNetwork.Reply packet) {
        if (closing || !session.equals(packet.session())) return;
        balance = packet.balance();
        if (packet.result() == ScienceStoreNetwork.INVALID) {
            onClose();
            return;
        }
        if (packet.sequence() != sequence) return;
        awaiting = false;
        if (packet.result() == ScienceStoreNetwork.WAIT) {
            if (holding) {
                retry = true;
                nextRetry = time() + .15;
            }
            return;
        }
        holding = false;
        retry = false;
        switch (packet.result()) {
            case ScienceStoreNetwork.OK -> {
                flashAt = time();
                show("gui.oasiso.science_store.purchased", 0x5BFFF6);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F));
            }
            case ScienceStoreNetwork.NOT_ENOUGH -> show("gui.oasiso.science_store.not_enough", 0xFF7575);
            case ScienceStoreNetwork.NO_SPACE -> show("gui.oasiso.science_store.no_space", 0xFF7575);
            default -> show("gui.oasiso.science_store.unavailable", 0xFF7575);
        }
    }

    private void show(String key, int color) {
        message = Component.translatable(key);
        messageUntil = time() + 2.5;
        messageColor = color;
    }

    @Override
    public void onClose() {
        if (closing) return;
        cancel();
        awaiting = false;
        closeSession();

        if (music != null && minecraft != null && minecraft.getSoundManager() != null) {
            music.fadeOut();
        }

        closeStarted = time();
        closeFrom = Math.min(INTRO_END, closeStarted);
        closing = true;
        if (closeFrom <= 0) finishClose();
    }

    private void closeSession() {
        if (closed) return;
        closed = true;
        if (minecraft != null && minecraft.getConnection() != null)
            ScienceStoreNetwork.send(session, sequence, ScienceStoreNetwork.CLOSE, selected);
    }

    private void finishClose() {
        if (music != null && minecraft != null && minecraft.getSoundManager() != null) {
            music.fadeOut();
        }
        if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        closeSession();
        icons.close();
        if (music != null && minecraft != null && minecraft.getSoundManager() != null) {
            music.fadeOut();
        }
        super.removed();
    }
}
