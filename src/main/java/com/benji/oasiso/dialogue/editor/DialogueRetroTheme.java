package com.benji.oasiso.dialogue.editor;

import net.minecraft.client.gui.GuiGraphics;

public final class DialogueRetroTheme {

    private DialogueRetroTheme() {
    }

    public static final int BLACK = 0xFF080A07;
    public static final int BLACK_SOFT = 0xFF10150D;
    public static final int DARK_GREEN = 0xFF1B2A17;
    public static final int GREEN = 0xFF436B35;
    public static final int GREEN_MID = 0xFF668F48;
    public static final int LIME = 0xFFB8FF72;
    public static final int LIME_SOFT = 0xFFA7E76B;

    public static final int CREAM = 0xFFF2ECD2;
    public static final int CREAM_LIGHT = 0xFFFFF9E6;
    public static final int BEIGE = 0xFFD5CCAA;
    public static final int BEIGE_DARK = 0xFFA89E7F;
    public static final int DESKTOP = 0xFF777B69;
    public static final int DESKTOP_DARK = 0xFF656A59;

    public static final int TEXT_LIGHT = 0xFFF3EDD4;
    public static final int TEXT_MUTED = 0xFFC6BFA2;
    public static final int TEXT_DARK = 0xFF151812;
    public static final int TEXT_DARK_MUTED = 0xFF4D5446;

    public static final int ERROR = 0xFFFF746A;
    public static final int WARNING = 0xFFFFD86A;

    public static void drawDesktop(GuiGraphics graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, DESKTOP, DESKTOP_DARK);

        // Old-workstation style sparse pinstripe/checker texture.
        for (int y = 0; y < height; y += 8) {
            graphics.fill(0, y, width, y + 1, 0x1F000000);
        }
        for (int x = 0; x < width; x += 16) {
            graphics.fill(x, 0, x + 1, height, 0x0FFFFFFF);
        }
    }

    public static void drawPanel(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        if (x1 <= x0 || y1 <= y0) return;

        graphics.fill(x0 + 3, y0 + 3, x1 + 3, y1 + 3, 0xB8000000);
        graphics.fill(x0, y0, x1, y1, BLACK);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, BEIGE_DARK);
        graphics.fillGradient(x0 + 2, y0 + 2, x1 - 2, y1 - 2, CREAM_LIGHT, BEIGE);

        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y0 + 3, 0xFFFFFFFF);
        graphics.fill(x0 + 2, y0 + 2, x0 + 3, y1 - 2, 0xFFFFFFFF);
        graphics.fill(x0 + 2, y1 - 3, x1 - 2, y1 - 2, 0xFF77705A);
        graphics.fill(x1 - 3, y0 + 2, x1 - 2, y1 - 2, 0xFF77705A);
    }

    public static void drawDarkInset(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        if (x1 <= x0 || y1 <= y0) return;

        graphics.fill(x0, y0, x1, y1, BLACK);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFF293522);
        graphics.fillGradient(x0 + 2, y0 + 2, x1 - 2, y1 - 2, 0xFF11170E, 0xFF080B07);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0xFF090B08);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, 0xFF090B08);
    }

    public static void drawTitleBar(GuiGraphics graphics, int x0, int y0, int x1, int height) {
        int y1 = y0 + Math.max(10, height);
        graphics.fill(x0, y0, x1, y1, BLACK);
        graphics.fillGradient(x0 + 1, y0 + 1, x1 - 1, y1 - 1, DARK_GREEN, 0xFF0A0D08);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, GREEN_MID);
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, 0xFF000000);
    }

    public static void drawSeparator(GuiGraphics graphics, int x0, int y, int x1) {
        graphics.fill(x0, y, x1, y + 1, BLACK);
        graphics.fill(x0, y + 1, x1, y + 2, 0xFF87906F);
    }

    public static void drawRetroScrollTrack(GuiGraphics graphics, int x, int y0, int y1, int thumbY, int thumbH) {
        graphics.fill(x - 1, y0, x + 3, y1, BLACK);
        graphics.fill(x, y0 + 1, x + 2, y1 - 1, 0xFF556046);
        graphics.fill(x - 2, thumbY, x + 4, thumbY + thumbH, BLACK);
        graphics.fill(x - 1, thumbY + 1, x + 3, thumbY + thumbH - 1, LIME_SOFT);
    }
}
