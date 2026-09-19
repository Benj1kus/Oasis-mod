package com.benji.oasiso.client.configscroll;

import com.benji.oasiso.config.OsirisRealmConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.benji.oasiso.client.configscroll.ConfigScrollDraw.alpha;
import static com.benji.oasiso.client.configscroll.ConfigScrollDraw.mix;

public final class ConfigScrollScreen extends Screen {
    private static final double TAU = Math.PI * 2;
    private static final int TEXT = 0xFFE6EFED, MUTED = 0xFF93A8AA, ERROR = 0xFFFF9B8C;
    private enum Modal { NONE, LIST, COLOR, DISCARD, RESTART }
    private final ConfigScrollModel model = new ConfigScrollModel(OsirisRealmConfig.SPEC);
    private final ConfigScrollTheme theme = ConfigScrollTheme.load();
    private final ConfigScrollEffects effects = new ConfigScrollEffects();
    private final List<Row> rows = new ArrayList<>();
    private final List<EditBox> modalFields = new ArrayList<>();
    private final long openedAt = System.nanoTime();
    private long lastFrame = openedAt, categoryAt = openedAt;
    private int rotationTarget;
    private double ringPosition, scroll, scrollTarget;
    private int left, right, top, bottom, rowHeight;
    private double ringX, ringY, ringRadius;
    private int hoveredDot = -1;
    private long dotHoverAt;
    private Row dragging;
    private Modal modal = Modal.NONE;
    private String status = "";
    private ConfigScrollModel.Entry editingList;
    private List<String> listDraft;
    private int listPage, pageSize, panelX, panelY, panelW, panelH;
    private boolean editingPrimary;
    private int colorBefore;
    private String colorText = "", colorError = "";
    private EditBox colorField;
    private boolean restartPending;

    public ConfigScrollScreen() { super(Component.literal("Config Scroll")); }

    @Override protected void init() {
        left = Math.max(110, (int) (width * .405));
        right = width - 18;
        top = Math.min(87, height / 3);
        bottom = height - 51;
        rowHeight = width >= 620 ? 69 : 62;
        ringRadius = Math.max(height * .64, width * .31);
        ringX = width * .30 - ringRadius;
        ringY = height * .49;
        panelW = Math.min(440, width - 30);
        panelH = Math.min(280, height - 28);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        pageSize = Math.max(1, (panelH - 100) / 25);
        if (modal == Modal.NONE) buildRows(); else buildModal();
    }

    private int selected() { return model.categories.isEmpty() ? 0 : Math.floorMod(rotationTarget, model.categories.size()); }
    private ConfigScrollModel.Category category() { return model.categories.get(selected()); }

    private void buildRows() {
        clearWidgets();
        modalFields.clear();
        dragging = null;
        rows.clear();
        if (!model.categories.isEmpty()) {
            int index = 0;
            for (var entry : category().entries) rows.add(new Row(entry, index++));
        }
        int available = right - left;
        int colorWidth = Math.min(70, (available - 64) / 2);
        button(left, height - 28, 48, 19, "Discard", () -> showModal(Modal.DISCARD));
        button(right - colorWidth * 2 - 6, height - 28, colorWidth, 19, "Ring", () -> editColor(true)).setTooltip(Tooltip.create(Component.literal("Primary colour: the golden ring")));
        button(right - colorWidth, height - 28, colorWidth, 19, "Accent", () -> editColor(false)).setTooltip(Tooltip.create(Component.literal("Secondary colour: dots, sliders and background glow")));
        button(12, height - 29, 23, 19, "<", () -> turn(-1));
        button(40, height - 29, 23, 19, ">", () -> turn(1));
        button(right - 18, 17, 18, 18, "?", () -> {}).setTooltip(Tooltip.create(Component.literal(
                "Wheel over the ring: change category\nWheel over settings: scroll values\nClick a dot to select it\nDrag a slider or type an exact number\nR: restore that setting's default\nEsc: save and close\nOnly your local config is edited; remote servers keep their own settings.")));
        layoutRows();
    }

    private ConfigScrollButton button(int x, int y, int w, int h, String text, Runnable action) {
        return addRenderableWidget(new ConfigScrollButton(x, y, w, h, text, () -> theme.accent, action));
    }

    private void turn(int amount) {
        if (model.categories.size() < 2 || modal != Modal.NONE) return;
        rotationTarget += amount;
        categoryAt = System.nanoTime();
        scroll = scrollTarget = 0;
        status = "";
        ConfigScrollSound.turn(amount);
        buildRows();
    }

    private double maxScroll() { return Math.max(0, rows.size() * rowHeight - (bottom - top)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static double ease(double value) { double t = clamp(value, 0, 1); return 1 - Math.pow(1 - t, 3); }
    private double age() { return (System.nanoTime() - openedAt) / 1.0e9; }

    private void layoutRows() {
        double opened = age();
        double categoryAge = (System.nanoTime() - categoryAt) / 1.0e9;
        int visibleIndex = Math.max(0, (int) (scroll / rowHeight));
        for (Row row : rows) {
            double stage = Math.min(ease((opened - .30 - Math.max(0, row.index - visibleIndex) * .035) / .5), ease(categoryAge / .22));
            row.reveal = stage;
            row.y = top + row.index * rowHeight - (int) scroll + (int) ((1 - stage) * 17);
            boolean full = row.y >= top && row.y + 49 <= bottom && stage > .9 && modal == Modal.NONE;
            if (row.field != null) {
                row.field.setX(right - 96);
                row.field.setY(row.y + 31);
                row.field.visible = full;
                row.field.active = full;
                if (!full) row.field.setFocused(false);
            }
            row.reset.setX(right - 18);
            row.reset.setY(row.y + 27);
            row.reset.visible = full;
            row.reset.active = full;
        }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        long now = System.nanoTime();
        double dt = clamp((now - lastFrame) / 1.0e9, 0, .08);
        lastFrame = now;
        ringPosition += (rotationTarget - ringPosition) * (1 - Math.exp(-dt * 12));
        scrollTarget = clamp(scrollTarget, 0, maxScroll());
        scroll += (scrollTarget - scroll) * (1 - Math.exp(-dt * 17));
        if (Math.abs(scroll - scrollTarget) < .1) scroll = scrollTarget;
        layoutRows();
        float t = (float) age();
        effects.render(g, width, height, t, (float) clamp(t / .85, 0, 1), theme.accent, partialTick);
        drawRing(g, modal == Modal.NONE ? mx : -1000, modal == Modal.NONE ? my : -1000, t);
        drawHeading(g, t);
        g.enableScissor(left - 4, top, right + 3, bottom);
        for (Row row : rows) if (row.y + rowHeight >= top && row.y <= bottom) row.render(g, mx, my, partialTick);
        g.disableScissor();
        if (maxScroll() > 0) {
            int track = bottom - top;
            int size = Math.max(16, (int) (track * (double) track / (rows.size() * rowHeight)));
            int y = top + (int) ((track - size) * scroll / maxScroll());
            g.fill(right + 8, top, right + 9, bottom, alpha(theme.accent, .15));
            g.fill(right + 7, y, right + 10, y + size, alpha(theme.accent, .7));
        }
        g.drawString(font, "Wheel: categories", 12, height - 43, MUTED, false);
        String footer = !status.isEmpty() ? status : (model.dirty() ? "Unsaved changes  /  Esc to save" : "Esc to close");
        g.drawString(font, fit(footer, right - left), left, height - 43, !status.isEmpty() ? ERROR : MUTED, false);
        if (modal != Modal.NONE) drawModal(g);
        if (age() > .48 || modal != Modal.NONE) super.render(g, mx, my, partialTick);
        if (modal == Modal.NONE && dragging == null) renderHints(g, mx, my);
    }

    private void drawHeading(GuiGraphics g, float time) {
        double fade = ease((time - .17) / .45);
        if (fade < .025) return;
        int x = left - 5;
        int shift = (int) ((1 - fade) * 12);
        g.drawString(font, "OSIRIS' REALM", x, 19 + shift, alpha(theme.primary, fade * .9), false);
        String name = model.categories.isEmpty() ? "No settings found" : category().name;
        float scale = width >= 600 ? 1.45F : 1.1F;
        g.pose().pushPose();
        g.pose().translate(x, 38 + shift, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, fit(name, (int) ((right - x) / scale)), 0, 0, alpha(0xF1F1DF, fade), false);
        g.pose().popPose();
        boolean remote = minecraft != null && !minecraft.hasSingleplayerServer();
        String note = remote ? "LOCAL ONLY / Server settings stay on the server" : "LOCAL CONFIG / Gameplay changes require restart";
        g.drawString(font, fit(note, right - x), x, top - 17, alpha(0x93A8AA, fade), false);
    }

    private void drawRing(GuiGraphics g, int mx, int my, float time) {
        double reveal = ease(time / .9), end = -Math.PI / 2 + TAU * reveal;
        double cx = ringX - (1 - ease(time / .65)) * 55;
        int count = model.categories.size();
        int over = dotAt(mx, my);
        if (over != hoveredDot && over >= 0) { dotHoverAt = System.nanoTime(); ConfigScrollSound.hover(); }
        hoveredDot = over;
        try (ConfigScrollDraw mesh = new ConfigScrollDraw(g)) {
            mesh.ring(cx, ringY, ringRadius, 13, -Math.PI / 2, end, alpha(theme.primary, .055 * reveal));
            mesh.ring(cx, ringY, ringRadius, 5, -Math.PI / 2, end, alpha(mix(theme.primary, 0x080D12, .58), reveal));
            mesh.ring(cx, ringY, ringRadius + 4, 1.15, -Math.PI / 2, end, alpha(theme.primary, .92 * reveal));
            mesh.ring(cx, ringY, ringRadius - 4, .85, -Math.PI / 2, end, alpha(theme.primary, .50 * reveal));
            mesh.ring(cx, ringY, ringRadius - 14, .65, -Math.PI / 2, end, alpha(theme.primary, .24 * reveal));
            for (int i = 0; i < 100 * reveal; i++) {
                double a = -Math.PI / 2 + i * TAU / 100;
                double c = Math.cos(a), s = Math.sin(a);
                if (i % 5 == 0) {
                    mesh.diamond(cx + c * (ringRadius - 9), ringY + s * (ringRadius - 9), 2.7, alpha(theme.primary, .65 * reveal));
                    mesh.line(cx + c * (ringRadius + 6), ringY + s * (ringRadius + 6), cx + c * (ringRadius + 11), ringY + s * (ringRadius + 11), .7, alpha(theme.primary, .65 * reveal));
                } else mesh.line(cx + c * (ringRadius - 2), ringY + s * (ringRadius - 2), cx + c * (ringRadius + 2), ringY + s * (ringRadius + 2), .55, alpha(theme.primary, .35 * reveal));
            }
            for (int i = 0; i < count; i++) {
                double a = (i - ringPosition) * TAU / count;
                double x = cx + Math.cos(a) * ringRadius, y = ringY + Math.sin(a) * ringRadius;
                if (x < -25 || y < -25 || y > height + 25) continue;
                double delay = .24 + Math.abs(Math.sin(a)) * .13;
                double show = ease((time - delay) / .46);
                double selectedAmount = Math.pow(Math.max(0, Math.cos(a)), 28);
                double hover = i == hoveredDot ? .08 + .10 * Math.sin((System.nanoTime() - dotHoverAt) / 1.0e9 * 23) * Math.exp(-(System.nanoTime() - dotHoverAt) / 1.0e9 * 7) : 0;
                double radius = (9.5 + selectedAmount * (3.1 + Math.sin(time * 3.6) * .65)) * show * (1 + hover);
                if (radius <= .01) continue;
                mesh.circle(x, y, radius + 5, alpha(theme.accent, .025 + selectedAmount * .07));
                mesh.circle(x, y, radius + 2, alpha(theme.accent, .07 + selectedAmount * .13));
                mesh.circle(x, y, radius, alpha(0x06141B, .98 * show));
                mesh.ring(x, y, radius, 1.15, 0, TAU, alpha(theme.accent, (.55 + selectedAmount * .45) * show));
                mesh.circle(x, y, radius * .38, alpha(theme.accent, (.50 + selectedAmount * .5) * show));
                if (selectedAmount > .15) mesh.ring(x, y, radius + 4, .6, -.6 + time * .16, 1.5 + time * .16, alpha(theme.primary, selectedAmount * .9));
            }
        }
        String page = count == 0 ? "0 / 0" : String.format("%02d / %02d", selected() + 1, count);
        g.drawString(font, page, 73, height - 23, alpha(theme.primary, reveal), false);
    }

    private int dotAt(double mx, double my) {
        if (age() < .48) return -1;
        int count = model.categories.size();
        for (int i = 0; i < count; i++) {
            double a = (i - ringPosition) * TAU / count;
            double x = ringX + Math.cos(a) * ringRadius, y = ringY + Math.sin(a) * ringRadius;
            if (Math.hypot(mx - x, my - y) < 16) return i;
        }
        return -1;
    }

    private void renderHints(GuiGraphics g, int mx, int my) {
        if (hoveredDot >= 0) g.renderTooltip(font, Component.literal(model.categories.get(hoveredDot).name), mx, my);
        else if (mx >= left && mx <= right && my >= top && my < bottom) {
            for (Row row : rows) {
                if (my < row.y || my >= row.y + rowHeight) continue;
                if (row.reset.isMouseOver(mx, my)) {
                    g.renderTooltip(font, Component.literal("Restore default: " + row.entry.format(row.entry.definition.getDefault())), mx, my);
                } else if (my < row.y + 24 || row.entry.error != null) {
                    String hint = row.entry.label + "\n" + row.entry.comment;
                    if (row.entry.numeric) hint += "\nRange: " + ConfigScrollModel.number(row.entry.min) + " ... " + ConfigScrollModel.number(row.entry.max)
                            + (row.entry.curved ? "\nCurved slider scale. Use the number field for an exact value." : "");
                    if (row.entry.error != null) hint += "\n" + row.entry.error;
                    g.renderTooltip(font, font.split(Component.literal(hint), Math.min(310, width - 24)), mx, my);
                }
                break;
            }
        }
    }

    private String fit(String text, int pixels) {
        if (font.width(text) <= pixels) return text;
        return font.plainSubstrByWidth(text, Math.max(0, pixels - font.width("..."))) + "...";
    }

    private final class Row {
        final ConfigScrollModel.Entry entry;
        final int index;
        final EditBox field;
        final ConfigScrollButton reset;
        int y;
        double trail = -1;
        double reveal;
        boolean hovering;
        long hoverAt;

        Row(ConfigScrollModel.Entry entry, int index) {
            this.entry = entry;
            this.index = index;
            if (entry.numeric || (!entry.list && !entry.bool)) {
                int fieldWidth = entry.numeric ? 66 : Math.max(50, right - left - 36);
                field = new EditBox(font, right - 96, top, fieldWidth, 13, Component.literal(entry.label));
                field.setMaxLength(entry.numeric ? 64 : 4096);
                field.setBordered(false);
                field.setTextColor(TEXT);
                field.setValue(entry.text);
                field.setResponder(entry::setText);
                addWidget(field);
            } else field = null;
            reset = addWidget(new ConfigScrollButton(right - 18, top, 18, 19, "R", () -> theme.primary, () -> {
                entry.reset();
                if (field != null) field.setValue(entry.text);
            }));
        }

        int sliderEnd() { return right - 111; }
        boolean sliderHit(double mx, double my) {
            return entry.numeric && mx >= left && mx <= sliderEnd() + 5 && my >= y + 25 && my <= y + 49 && my >= top && my < bottom;
        }
        void slide(double mx) {
            Object previous = entry.draft;
            entry.fromSlider((mx - left - 4) / Math.max(1, sliderEnd() - left - 8));
            field.setValue(entry.text);
            if (!previous.equals(entry.draft)) ConfigScrollSound.slider(entry.fraction());
        }
        void render(GuiGraphics g, int mx, int my, float partialTick) {
            if (reveal < .025) return;
            boolean hover = modal == Modal.NONE && mx >= left && mx <= right && my >= Math.max(top, y) && my < Math.min(bottom, y + 50);
            if (hover && !hovering) { hoverAt = System.nanoTime(); ConfigScrollSound.hover(); }
            hovering = hover;
            int textColor = alpha(entry.error != null ? ERROR : TEXT, reveal);
            g.drawString(font, fit(entry.label, right - left - 9), left, y + 1, textColor, false);
            if (entry.dirty()) g.fill(left - 4, y + 1, left - 2, y + 8, alpha(theme.accent, .95));
            String description = entry.error != null ? entry.error : entry.comment.lines().findFirst().orElse("");
            g.drawString(font, fit(description, right - left - 2), left, y + 13, alpha(entry.error != null ? ERROR : MUTED, reveal), false);
            if (entry.numeric) {
                int start = left + 4, end = sliderEnd() - 4, cy = y + 37;
                double knob = start + (end - start) * entry.fraction();
                if (trail < 0) trail = knob;
                trail += (knob - trail) * .14;
                g.fill(start, cy - 1, end, cy + 1, alpha(theme.accent, .16));
                g.fill(start, cy - 1, (int) knob, cy + 1, alpha(theme.accent, .95));
                g.fill((int) Math.min(knob, trail), cy - 3, (int) Math.max(knob, trail) + 1, cy + 3, alpha(theme.accent, .24));
                double elapsed = (System.nanoTime() - hoverAt) / 1.0e9;
                double bounce = hover ? .65 + 1.0 * Math.sin(elapsed * 24) * Math.exp(-elapsed * 8) : 0;
                try (ConfigScrollDraw mesh = new ConfigScrollDraw(g)) {
                    mesh.circle(knob, cy, 6 + bounce, alpha(theme.accent, .16));
                    mesh.circle(knob, cy, 3.5 + bounce, alpha(theme.accent, 1));
                    mesh.circle(knob, cy, 1.4, alpha(0xE4FFFF, 1));
                }
                g.fill(right - 100, y + 26, right - 26, y + 47, 0xC008171D);
                g.fill(right - 100, y + 46, right - 26, y + 47, alpha(entry.error != null ? 0xFF8E80 : theme.accent, .55));
            } else if (entry.list || entry.bool) {
                String value = entry.list ? "Edit list  /  " + ((List<?>) entry.draft).size() + " entries" : ((Boolean) entry.draft ? "ON" : "OFF");
                g.fill(left, y + 26, right - 26, y + 47, alpha(theme.accent, hover ? .18 : .08));
                g.drawString(font, fit(value, right - left - 32), left + 6, y + 32, alpha(theme.accent, 1), false);
            }
            if (field != null) {
                field.setTextColor(textColor);
                if (!entry.numeric) field.setX(left + 4);
                if (modal == Modal.NONE && field.visible) field.render(g, mx, my, partialTick);
                else g.drawString(font, fit(entry.text, entry.numeric ? 66 : right - left - 36), entry.numeric ? right - 96 : left + 4, y + 31, textColor, false);
            }
            if (modal == Modal.NONE && reset.visible) reset.render(g, mx, my, partialTick);
            g.fill(left, y + rowHeight - 5, right, y + rowHeight - 4, alpha(theme.primary, .10));
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (modal != Modal.NONE) return super.mouseClicked(mx, my, button);
        if (age() < .48) return true;
        if (button == 0 && age() > .45) {
            int dot = dotAt(mx, my);
            if (dot >= 0) {
                int difference = dot - selected(), count = model.categories.size();
                if (difference > count / 2) difference -= count;
                if (difference < -count / 2) difference += count;
                if (difference != 0) turn(difference);
                return true;
            }
            for (Row row : rows) {
                if (row.sliderHit(mx, my)) {
                    setFocused(null);
                    dragging = row;
                    row.slide(mx);
                    return true;
                }
                if ((row.entry.list || row.entry.bool) && mx >= left && mx < right - 26 && my >= Math.max(top, row.y + 26) && my <= Math.min(bottom - 1, row.y + 47)) {
                    ConfigScrollSound.click();
                    if (row.entry.list) editList(row.entry); else row.entry.set(!((Boolean) row.entry.draft));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && button == 0) { dragging.slide(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; return true; }
        return super.mouseReleased(mx, my, button);
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta == 0) return false;
        if (modal == Modal.LIST) { changeListPage(delta > 0 ? -1 : 1); return true; }
        if (modal != Modal.NONE || dragging != null) return true;
        if (mx < left - 12) turn(delta > 0 ? -1 : 1);
        else scrollTarget = clamp(scrollTarget - delta * rowHeight * .7, 0, maxScroll());
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (modal == Modal.NONE && !(getFocused() instanceof EditBox box && box.isFocused())) {
            if (key == GLFW.GLFW_KEY_LEFT) { turn(-1); return true; }
            if (key == GLFW.GLFW_KEY_RIGHT) { turn(1); return true; }
            if (key == GLFW.GLFW_KEY_PAGE_DOWN) { scrollTarget += bottom - top; return true; }
            if (key == GLFW.GLFW_KEY_PAGE_UP) { scrollTarget -= bottom - top; return true; }
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void tick() {
        if (modal == Modal.NONE) for (Row row : rows) { if (row.field != null) row.field.tick(); }
        else for (EditBox field : modalFields) field.tick();
    }
    @Override public boolean isPauseScreen() { return true; }
    @Override public void removed() { effects.close(); }

    @Override public void onClose() {
        if (modal == Modal.RESTART) { minecraft.setScreen(null); return; }
        if (modal == Modal.COLOR) {
            if (editingPrimary) theme.primary = colorBefore; else theme.accent = colorBefore;
            closeModal();
            return;
        }
        if (modal != Modal.NONE) { closeModal(); return; }
        var invalid = model.firstInvalid();
        if (invalid != null) {
            for (int i = 0; i < model.categories.size(); i++) if (model.categories.get(i).entries.contains(invalid)) {
                rotationTarget = i;
                categoryAt = System.nanoTime() - 1_000_000_000L;
                buildRows();
                int row = category().entries.indexOf(invalid);
                scroll = scrollTarget = clamp(row * rowHeight, 0, maxScroll());
                layoutRows();
                if (rows.get(row).field != null) setFocused(rows.get(row).field);
                break;
            }
            status = "Fix the highlighted value, or Discard changes.";
            return;
        }
        boolean gameplayChanged = model.dirty();
        try {
            model.save();
            restartPending |= gameplayChanged;
            theme.save();
            if (restartPending) showModal(Modal.RESTART); else minecraft.setScreen(null);
        } catch (Exception ex) {
            status = "Could not save: " + (ex.getMessage() == null ? "see latest.log" : ex.getMessage());
            LogUtils.getLogger().error("Config save failed", ex);
        }
    }

    private void showModal(Modal next) { modal = next; status = ""; dragging = null; buildModal(); }
    private void closeModal() { modal = Modal.NONE; status = ""; buildRows(); }
    private void editList(ConfigScrollModel.Entry entry) {
        editingList = entry;
        listDraft = new ArrayList<>();
        for (Object value : (List<?>) entry.draft) listDraft.add(String.valueOf(value));
        listPage = 0;
        showModal(Modal.LIST);
    }
    private void changeListPage(int direction) {
        int page = Math.max(0, Math.min(Math.max(0, (listDraft.size() - 1) / pageSize), listPage + direction));
        if (page != listPage) { listPage = page; ConfigScrollSound.turn(direction); buildModal(); }
    }
    private void editColor(boolean primary) {
        editingPrimary = primary;
        colorBefore = primary ? theme.primary : theme.accent;
        colorText = ConfigScrollTheme.hex(colorBefore);
        colorError = "";
        showModal(Modal.COLOR);
    }
    private EditBox modalField(int x, int y, int w, String value, java.util.function.Consumer<String> onChange) {
        EditBox field = new EditBox(font, x, y, w, 18, Component.literal("Value"));
        field.setMaxLength(4096);
        field.setValue(value);
        field.setResponder(onChange);
        modalFields.add(field);
        return addRenderableWidget(field);
    }

    private void buildModal() {
        clearWidgets();
        modalFields.clear();
        for (Row row : rows) {
            if (row.field != null) row.field.setFocused(false);
        }
        int x = panelX + 14, w = panelW - 28, controls = panelY + panelH - 31;
        if (modal == Modal.LIST) {
            listPage = Math.min(listPage, Math.max(0, (listDraft.size() - 1) / pageSize));
            int first = listPage * pageSize, end = Math.min(listDraft.size(), first + pageSize);
            for (int i = first; i < end; i++) {
                final int index = i;
                int y = panelY + 48 + (i - first) * 25;
                modalField(x, y, w - 26, listDraft.get(i), text -> listDraft.set(index, text));
                button(x + w - 21, y, 21, 18, "-", () -> { listDraft.remove(index); buildModal(); });
            }
            button(x, controls, 34, 20, "Add", () -> { listDraft.add(""); listPage = (listDraft.size() - 1) / pageSize; buildModal(); });
            button(x + 39, controls, 20, 20, "<", () -> changeListPage(-1));
            button(x + 63, controls, 20, 20, ">", () -> changeListPage(1));
            button(x + w - 103, controls, 49, 20, "Cancel", this::closeModal);
            button(x + w - 49, controls, 49, 20, "Apply", () -> {
                List<String> candidate = new ArrayList<>(listDraft);
                if (editingList.set(candidate)) closeModal();
                else {
                    status = "Invalid list entry. Check the format and limits.";
                    for (int i = 0; i < candidate.size(); i++) if (!editingList.accepts(List.of(candidate.get(i)))) {
                        listPage = i / pageSize;
                        status = "Entry " + (i + 1) + ": check entity_id| min | max | group (counts 0..128).";
                        buildModal();
                        break;
                    }
                }
            });
        } else if (modal == Modal.COLOR) {
            colorField = modalField(x, panelY + 50, w - 40, colorText, text -> {
                colorText = text;
                try {
                    int color = ConfigScrollTheme.parse(text);
                    if (editingPrimary) theme.primary = color; else theme.accent = color;
                    colorError = "";
                } catch (IllegalArgumentException ex) { colorError = "Use #RRGGBB (six hexadecimal digits)."; }
            });
            colorField.setMaxLength(7);
            int[] colors = {0xE7C777, 0x37DBD3, 0x42A7F5, 0xD59BFF, 0x5BE1A3, 0xFFAF87, 0xECE8D9, 0x6486C9};
            int columns = 4, gap = 5, sw = (w - gap * (columns - 1)) / columns;
            for (int i = 0; i < colors.length; i++) {
                final int color = colors[i];
                addRenderableWidget(new ConfigScrollButton(x + (i % columns) * (sw + gap), panelY + 82 + (i / columns) * 26,
                        sw, 21, ConfigScrollTheme.hex(color), () -> color, () -> colorField.setValue(ConfigScrollTheme.hex(color))));
            }
            button(x, controls, 59, 20, "Default", () -> colorField.setValue(ConfigScrollTheme.hex(editingPrimary ? ConfigScrollTheme.GOLD : ConfigScrollTheme.CYAN)));
            button(x + w - 103, controls, 49, 20, "Cancel", this::onClose);
            button(x + w - 49, controls, 49, 20, "Done", () -> { if (colorError.isEmpty()) closeModal(); });
        } else if (modal == Modal.DISCARD) {
            button(x, controls, 88, 20, "Keep editing", this::closeModal);
            button(x + w - 88, controls, 88, 20, "Discard & exit", () -> {
                if (restartPending) showModal(Modal.RESTART); else minecraft.setScreen(null);
            });
        } else if (modal == Modal.RESTART) {
            button(panelX + (panelW - 126) / 2, controls, 126, 20, "Back to game", () -> minecraft.setScreen(null));
        }
    }

    private void drawModal(GuiGraphics g) {
        g.fill(0, 0, width, height, 0x99010810);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF407131C);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, alpha(theme.primary, 1));
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, alpha(theme.accent, .65));
        String title = switch (modal) {
            case LIST -> editingList.label;
            case COLOR -> editingPrimary ? "Primary colour/Ring" : "Secondary colour/Accent";
            case DISCARD -> "Discard changes?";
            case RESTART -> "Restart the game";
            default -> "";
        };
        g.drawString(font, fit(title, panelW - 28), panelX + 14, panelY + 15, alpha(theme.primary, 1), false);
        String subtitle = switch (modal) {
            case LIST -> "entity_id| min | max  / optional fourth value: group";
            case COLOR -> "Live preview. Enter a colour or choose a preset.";
            case DISCARD -> "Your unsaved gameplay and colour edits will be discarded.";
            case RESTART -> "Config saved. Restart Minecraft to apply all gameplay changes.";
            default -> "";
        };
        if (modal == Modal.DISCARD || modal == Modal.RESTART) {
            int y = panelY + 53;
            for (var line : font.split(Component.literal(subtitle), panelW - 36)) {
                g.drawString(font, line, panelX + 18, y, TEXT, false); y += 13;
            }
            if (modal == Modal.RESTART) {
                for (var line : font.split(Component.literal("Some settings may already affect new actions. A full restart ensures all settings use the saved values."), panelW - 36)) {
                    g.drawString(font, line, panelX + 18, y + 14, MUTED, false); y += 12;
                }
            }
        } else g.drawString(font, fit(subtitle, panelW - 28), panelX + 14, panelY + 32, MUTED, false);
        if (modal == Modal.COLOR) {
            int color = editingPrimary ? theme.primary : theme.accent;
            g.fill(panelX + panelW - 40, panelY + 50, panelX + panelW - 14, panelY + 68, alpha(color, 1));
        }
        if (modal == Modal.LIST) {
            String page = (listPage + 1) + " / " + Math.max(1, (listDraft.size() + pageSize - 1) / pageSize);
            g.drawString(font, page, panelX + panelW - font.width(page) - 14, panelY + 15, MUTED, false);
        }
        String error = modal == Modal.COLOR ? colorError : status;
        if (!error.isEmpty()) g.drawString(font, fit(error, panelW - 28), panelX + 14, panelY + panelH - 45, ERROR, false);
    }
}
