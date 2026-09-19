package com.benji.oasiso.common.util;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class OasisoTextFx {
    private OasisoTextFx() {
    }

    public record Palette(int first, int second, int third, int breath, int shine) {
    }

    public static final Palette CYAN_PURPLE = new Palette(0x42F2E1, 0xB76CFF, 0x6495FF, 0x9AEFFF, 0xFFFFFF);
    public static final Palette GOLD_ORANGE = new Palette(0xFFD45A, 0xFF982E, 0xF5B846, 0xFFE47A, 0xFFF3D8);

    public static double time() {
        return Util.getMillis() / 50.0D;
    }

    public static int color(Palette palette, double time, int index, boolean shine) {
        double phase = positiveMod(time * .042D + index * .081D, 3.0D);
        int color;
        if (phase < 1.0D) color = blend(palette.first(), palette.second(), sineEase(phase));
        else if (phase < 2.0D) color = blend(palette.second(), palette.third(), sineEase(phase - 1.0D));
        else color = blend(palette.third(), palette.first(), sineEase(phase - 2.0D));

        double breath = .5D + .5D * Math.sin(time * .080D + index * .23D);
        color = blend(color, palette.breath(), .10D * sineEase(breath));
        if (shine) {
            double crest = Math.pow(.5D + .5D * Math.sin(time * .158D - index * .73D), 15.0D);
            color = blend(color, palette.shine(), crest * .70D);
        }
        return color;
    }

    public static MutableComponent component(Component text, Palette palette, boolean shine) {
        MutableComponent out = Component.empty();
        double time = time();
        int[] index = {0};
        text.visit((Style style, String run) -> {
            for (int offset = 0; offset < run.length(); ) {
                int cp = run.codePointAt(offset);
                out.append(Component.literal(new String(Character.toChars(cp))).setStyle(style.withColor(color(palette, time, index[0]++, shine))));
                offset += Character.charCount(cp);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    public static double hash01(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (x & 0x1FFFFFFFFFFFFFL) / (double) 0x20000000000000L;
    }

    public static double positiveMod(double value, double mod) {
        double r = value % mod;
        return r < 0 ? r + mod : r;
    }

    public static double sineEase(double value) {
        double t = Math.max(0, Math.min(1, value));
        return .5D - .5D * Math.cos(Math.PI * t);
    }

    public static double easeOutCubic(double value) {
        double q = 1.0D - Math.max(0, Math.min(1, value));
        return 1.0D - q * q * q;
    }

    public static int blend(int a, int b, double amount) {
        double t = Math.max(0, Math.min(1, amount));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
