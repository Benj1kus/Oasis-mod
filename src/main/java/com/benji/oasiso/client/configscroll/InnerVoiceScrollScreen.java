package com.benji.oasiso.client.configscroll;

import com.benji.oasiso.client.voice.InnerVoiceClient;
import com.benji.oasiso.client.voice.InnerVoicePreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static com.benji.oasiso.client.configscroll.ConfigScrollDraw.alpha;
import static com.benji.oasiso.client.configscroll.ConfigScrollDraw.mix;


public class InnerVoiceScrollScreen extends Screen {
    private static final double TAU = Math.PI * 2;
    private static final int TEXT = 0xFFE6EFED, MUTED = 0xFF93A8AA;
    private final ConfigScrollTheme theme = ConfigScrollTheme.load();
    private final ConfigScrollEffects effects = new ConfigScrollEffects();
    private final long openedAt = System.nanoTime();
    private long lastFrame = openedAt, hoverAt;
    private int voice, rotationTarget, hoveredDot = -1;
    private double ringPosition, ringX, ringY, ringRadius;
    private int left, right, top, rowHeight;
    private EditBox vignette, text;
    private ConfigScrollButton save, selector;
    private boolean error;

    public InnerVoiceScrollScreen() {
        super(Component.translatable("gui.oasiso_voice.title"));
        InnerVoicePreferences.load();
        voice = Math.floorMod(InnerVoicePreferences.voice, InnerVoicePreferences.VOICES.length);
        rotationTarget = voice;
        ringPosition = voice;
    }

    @Override
    protected void init() {
        String oldV = vignette == null ? hex(InnerVoicePreferences.vignette) : vignette.getValue();
        String oldT = text == null ? hex(InnerVoicePreferences.text) : text.getValue();
        left = Math.max(110, (int) (width * .405));
        right = width - 18;
        top = Math.min(87, height / 3);
        rowHeight = Math.min(66, Math.max(38, (height - 52 - top) / 3));
        top = Math.min(top, height - 52 - rowHeight * 3);
        ringRadius = Math.max(height * .64, width * .31);
        ringX = width * .30 - ringRadius;
        ringY = height * .49;
        vignette = field(top, oldV, "gui.oasiso_voice.vignette");
        text = field(top + rowHeight, oldT, "gui.oasiso_voice.text");
        int y = top + rowHeight * 2 + 17;
        button(left, y, 21, 19, Component.literal("<"), () -> cycle(-1));
        selector = button(left + 26, y, right - left - 52, 19, Component.literal(InnerVoicePreferences.VOICES[voice]), () -> InnerVoiceClient.preview(voice));
        selector.setTooltip(Tooltip.create(tr("preview")));
        button(right - 21, y, 21, 19, Component.literal(">"), () -> cycle(1));
        int half = (right - left - 7) / 2;
        button(left, height - 28, half, 19, Component.translatable("gui.cancel"), this::onClose);
        save = button(left + half + 7, height - 28, right - left - half - 7, 19, Component.translatable("gui.oasiso_voice.save"), this::save);
        button(12, height - 29, 23, 19, Component.literal("<"), () -> cycle(-1));
        button(40, height - 29, 23, 19, Component.literal(">"), () -> cycle(1));
        updateSave();
    }

    private static Component tr(String key) {
        return Component.translatable("gui.oasiso_voice_ui." + key);
    }

    private static String hex(int value) {
        return String.format("%06X", value & 0xFFFFFF);
    }

    private static boolean valid(EditBox box) {
        return box != null && box.getValue().length() == 6;
    }

    private static int color(EditBox box, int fallback) {
        return valid(box) ? Integer.parseInt(box.getValue(), 16) : fallback;
    }

    private double age() {
        return (System.nanoTime() - openedAt) / 1.0e9;
    }

    private static double ease(double t) {
        return 1 - Math.pow(1 - Math.max(0, Math.min(1, t)), 3);
    }

    private ConfigScrollButton button(int x, int y, int w, int h, Component label, Runnable action) {
        ConfigScrollButton b = new ConfigScrollButton(x, y, w, h, label.getString(), () -> theme.accent, action);
        b.setMessage(label);
        return addRenderableWidget(b);
    }

    private EditBox field(int y, String value, String label) {
        EditBox box = new EditBox(font, left + 15, y + 22, right - left - 49, 12, Component.translatable(label));
        box.setBordered(false);
        box.setTextColor(TEXT);
        box.setMaxLength(6);
        box.setFilter(s -> s.matches("[0-9a-fA-F]{0,6}"));
        box.setValue(value);
        box.setTooltip(Tooltip.create(Component.translatable("gui.oasiso_voice.hex")));
        box.setResponder(s -> {
            error = false;
            updateSave();
        });
        return addRenderableWidget(box);
    }

    private void updateSave() {
        if (save != null) save.active = valid(vignette) && valid(text);
    }

    private void save() {
        if (!valid(vignette) || !valid(text)) return;
        if (InnerVoicePreferences.save(color(vignette, 0), color(text, 0), voice)) onClose();
        else error = true;
    }

    private void cycle(int delta) {
        rotationTarget += delta;
        voice = Math.floorMod(rotationTarget, InnerVoicePreferences.VOICES.length);
        selector.setMessage(Component.literal(InnerVoicePreferences.VOICES[voice]));
        ConfigScrollSound.turn(delta);
        InnerVoiceClient.preview(voice);
    }

    @Override
    public void tick() {
        vignette.tick();
        text.tick();
        updateSave();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        effects.close();
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        long now = System.nanoTime();
        double dt = Math.max(0, Math.min(.08, (now - lastFrame) / 1.0e9));
        lastFrame = now;
        ringPosition += (rotationTarget - ringPosition) * (1 - Math.exp(-dt * 12));
        float time = (float) age();
        effects.render(g, width, height, time, Math.min(1, time / .85F), theme.accent, partialTick);
        drawRing(g, mx, my, time);
        double fade = ease((time - .17) / .45);
        if (fade > .025) {
            int shift = (int) ((1 - fade) * 12);
            g.drawString(font, "OSIRIS' REALM", left - 5, 19 + shift, alpha(theme.primary, fade * .9), false);
            float scale = width >= 600 ? 1.45F : 1.1F;
            g.pose().pushPose();
            g.pose().translate(left - 5, 38 + shift, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, fit(title.getString(), (int) ((right - left + 5) / scale)), 0, 0, alpha(0xF1F1DF, fade), false);
            g.pose().popPose();
            if (top >= 74)
                g.drawString(font, fit(tr("local").getString(), right - left), left - 5, top - 17, alpha(0x93A8AA, fade), false);
        }
        colorRow(g, vignette, top, "gui.oasiso_voice.vignette", InnerVoicePreferences.vignette);
        colorRow(g, text, top + rowHeight, "gui.oasiso_voice.text", InnerVoicePreferences.text);
        int y = top + rowHeight * 2;
        g.drawString(font, Component.translatable("gui.oasiso_voice.voice"), left, y + 3, TEXT, false);
        if (rowHeight >= 53) {
            g.drawString(font, fit(tr("preview").getString(), right - left), left, y + 42, MUTED, false);
        }
        Component status = error ? Component.translatable("gui.oasiso_voice.error") : !valid(vignette) || !valid(text) ? Component.translatable("gui.oasiso_voice.hex") : tr("footer");
        g.drawString(font, fit(status.getString(), right - left), left, height - 43, error ? 0xFFFF9B8C : MUTED, false);
        g.drawString(font, fit(tr("wheel").getString(), left - 24), 12, height - 43, MUTED, false);
        super.render(g, mx, my, partialTick);
        if (hoveredDot >= 0) g.renderTooltip(font, Component.literal(InnerVoicePreferences.VOICES[hoveredDot]), mx, my);
    }

    private void colorRow(GuiGraphics g, EditBox box, int y, String label, int fallback) {
        int c = color(box, fallback);
        g.drawString(font, fit(Component.translatable(label).getString(), right - left), left, y + 3, TEXT, false);
        g.fill(left, y + 17, right - 29, y + 36, alpha(0x08161B, .90));
        g.fill(left, y + 35, right - 29, y + 36, alpha(valid(box) ? theme.accent : 0xFF9B8C, box.isFocused() ? .95 : .38));
        g.drawString(font, "#", left + 4, y + 22, MUTED, false);
        g.fill(right - 24, y + 17, right, y + 36, alpha(c, 1));
        if (rowHeight >= 53) {
            g.fillGradient(left, y + 42, right, y + 46, alpha(c, .65), alpha(c, .04));
        }
    }

    private String fit(String s, int pixels) {
        if (font.width(s) <= pixels) return s;
        return font.plainSubstrByWidth(s, Math.max(0, pixels - font.width("..."))) + "...";
    }

    private double ringCenterX() {
        return ringX - (1 - ease(age() / .65)) * 55;
    }

    private int dotAt(double mx, double my) {
        if (age() < .48 || mx >= left - 8 || my >= height - 48) return -1;
        for (int i = 0; i < InnerVoicePreferences.VOICES.length; i++) {
            double a = (i - ringPosition) * TAU / InnerVoicePreferences.VOICES.length;
            if (Math.hypot(mx - ringCenterX() - Math.cos(a) * ringRadius, my - ringY - Math.sin(a) * ringRadius) < 16)
                return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int hit = dotAt(mx, my);
            if (hit >= 0) {
                int count = InnerVoicePreferences.VOICES.length;
                int delta = Math.floorMod(hit - voice + count / 2, count) - count / 2;
                if (delta == 0) InnerVoiceClient.preview(voice);
                else cycle(delta);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < left - 8 && delta != 0) {
            cycle(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void drawRing(GuiGraphics g, int mx, int my, float time) {
        double reveal = ease(time / .9), end = -Math.PI / 2 + TAU * reveal, cx = ringCenterX();
        int over = dotAt(mx, my);
        if (over != hoveredDot && over >= 0) {
            hoverAt = System.nanoTime();
            ConfigScrollSound.hover();
        }
        hoveredDot = over;
        g.enableScissor(0, 0, left - 8, height);
        try (ConfigScrollDraw mesh = new ConfigScrollDraw(g)) {
            mesh.ring(cx, ringY, ringRadius, 13, -Math.PI / 2, end, alpha(theme.primary, .055 * reveal));
            mesh.ring(cx, ringY, ringRadius, 5, -Math.PI / 2, end, alpha(mix(theme.primary, 0x080D12, .58), reveal));
            mesh.ring(cx, ringY, ringRadius + 4, 1.15, -Math.PI / 2, end, alpha(theme.primary, .92 * reveal));
            mesh.ring(cx, ringY, ringRadius - 4, .85, -Math.PI / 2, end, alpha(theme.primary, .50 * reveal));
            mesh.ring(cx, ringY, ringRadius - 14, .65, -Math.PI / 2, end, alpha(theme.primary, .24 * reveal));
            for (int i = 0; i < 100 * reveal; i++) {
                double a = -Math.PI / 2 + i * TAU / 100, c = Math.cos(a), s = Math.sin(a);
                if (i % 5 == 0) {
                    mesh.diamond(cx + c * (ringRadius - 9), ringY + s * (ringRadius - 9), 2.7, alpha(theme.primary, .65 * reveal));
                    mesh.line(cx + c * (ringRadius + 6), ringY + s * (ringRadius + 6), cx + c * (ringRadius + 11), ringY + s * (ringRadius + 11), .7, alpha(theme.primary, .65 * reveal));
                } else
                    mesh.line(cx + c * (ringRadius - 2), ringY + s * (ringRadius - 2), cx + c * (ringRadius + 2), ringY + s * (ringRadius + 2), .55, alpha(theme.primary, .35 * reveal));
            }
            for (int i = 0; i < InnerVoicePreferences.VOICES.length; i++) {
                double a = (i - ringPosition) * TAU / InnerVoicePreferences.VOICES.length;
                double x = cx + Math.cos(a) * ringRadius, y = ringY + Math.sin(a) * ringRadius;
                if (x < -25 || y < -25 || y > height + 25) continue;
                double show = ease((time - .24 - Math.abs(Math.sin(a)) * .13) / .46);
                double selected = Math.pow(Math.max(0, Math.cos(a)), 28);
                double elapsed = (System.nanoTime() - hoverAt) / 1.0e9;
                double hover = i == hoveredDot ? .08 + .10 * Math.sin(elapsed * 23) * Math.exp(-elapsed * 7) : 0;
                double radius = (9.5 + selected * (3.1 + Math.sin(time * 3.6) * .65)) * show * (1 + hover);
                if (radius <= .01) continue;
                mesh.circle(x, y, radius + 5, alpha(theme.accent, (.025 + selected * .07) * show));
                mesh.circle(x, y, radius + 2, alpha(theme.accent, (.07 + selected * .13) * show));
                mesh.circle(x, y, radius, alpha(0x06141B, .98 * show));
                mesh.ring(x, y, radius, 1.15, 0, TAU, alpha(theme.accent, (.55 + selected * .45) * show));
                mesh.circle(x, y, radius * .38, alpha(theme.accent, (.50 + selected * .5) * show));
                if (selected > .15)
                    mesh.ring(x, y, radius + 4, .6, -.6 + time * .16, 1.5 + time * .16, alpha(theme.primary, selected * .9));
            }
        }
        g.disableScissor();
        g.drawString(font, String.format("%02d / %02d", voice + 1, InnerVoicePreferences.VOICES.length), 73, height - 23, alpha(theme.primary, reveal), false);
    }
}
