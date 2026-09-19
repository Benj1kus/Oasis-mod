package com.benji.oasiso.client.tooltip;

import com.benji.oasiso.common.util.OasisoTextFx;
import com.benji.oasiso.common.util.OasisoTextFx.Palette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record OasisoAnimatedTooltip(List<Part> parts) implements TooltipComponent {
    public OasisoAnimatedTooltip {
        parts = List.copyOf(parts);
    }

    public static OasisoAnimatedTooltip line(Part... parts) {
        return new OasisoAnimatedTooltip(List.of(parts));
    }

    public static Part part(Component text, Palette palette) {
        return new Part(text, palette, true, false, null);
    }

    public record Part(Component text, Palette palette, boolean shine, boolean glowing, Sparks particles) {
        public Part glow() {
            return new Part(text, palette, shine, true, particles);
        }

        public Part sparks(Sparks colors) {
            return new Part(text, palette, shine, glowing, colors);
        }

        public Part withoutShine() {
            return new Part(text, palette, false, glowing, particles);
        }
    }

    //SPARK COLORS
    public record Sparks(int auraDark, int auraBright, int coreDark, int coreBright, int flash) {
        public static final Sparks RED = new Sparks(0x6C091B, 0xF23B4D, 0xA00E28, 0xFF5360, 0xFFD4C7);
        public static final Sparks BLUE = new Sparks(0x00FFC3, 0x69FFDC, 0x33DDFF, 0x2FF588, 0x45E0FF);
        public static final Sparks GOLD = new Sparks(0x6C4009, 0xF2BB3B, 0xA0660E, 0xFFD553, 0xFFF2C7);
    }

    private record Glyph(Component component, int width) {
    }

    private record Run(Part part, List<Glyph> glyphs, int width) {
    }

    private static Run run(Font font, Part part) {
        List<Glyph> glyphs = new ArrayList<>();
        part.text().visit((Style style, String value) -> {
            for (int offset = 0; offset < value.length(); ) {
                int cp = value.codePointAt(offset);
                Component glyph = Component.literal(new String(Character.toChars(cp))).setStyle(style.withColor((TextColor) null));
                glyphs.add(new Glyph(glyph, font.width(glyph)));
                offset += Character.charCount(cp);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return new Run(part, glyphs, glyphs.stream().mapToInt(Glyph::width).sum());
    }

    private record Client(OasisoAnimatedTooltip row) implements ClientTooltipComponent {
        private int pad() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(Font font) {
            return row.parts().stream().mapToInt(p -> run(font, p).width()).sum() + pad() * 2;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            List<Run> runs = row.parts().stream().map(p -> run(font, p)).toList();
            double time = OasisoTextFx.time();
            int left = x + pad();
            int top = y + pad();
            int cursor = left;
            for (Run run : runs) {
                if (run.part().particles() != null && run.width() > 0)
                    particles(graphics, cursor, top - 1, run.width(), 9, time, run.part().particles(), false);
                cursor += run.width();
            }
            cursor = left;
            for (Run run : runs) {
                int index = 0;
                for (Glyph glyph : run.glyphs()) {
                    int rgb = OasisoTextFx.color(run.part().palette(), time, index++, run.part().shine());
                    if (run.part().glowing() && !glyph.component().getString().isBlank())
                        glow(graphics, font, glyph.component(), cursor, top, rgb);
                    graphics.drawString(font, glyph.component(), cursor, top, 0xFF000000 | rgb, false);
                    cursor += glyph.width();
                }
            }
            cursor = left;
            for (Run run : runs) {
                if (run.part().particles() != null && run.width() > 0)
                    particles(graphics, cursor, top - 1, run.width(), 9, time, run.part().particles(), true);
                cursor += run.width();
            }
        }
    }

    private static void glow(GuiGraphics g, Font font, Component glyph, int x, int y, int color) {
        int aura = 0x34000000 | OasisoTextFx.blend(0x000000, color, .72D);
        g.drawString(font, glyph, x - 1, y, aura, false);
        g.drawString(font, glyph, x + 1, y, aura, false);
        g.drawString(font, glyph, x, y + 1, aura, false);
    }

    private static void particles(GuiGraphics g, int x, int y, int width, int height, double time, Sparks palette, boolean cores) {
        for (int i = 0; i < 18; i++) {
            double age = OasisoTextFx.positiveMod(time * .036D + OasisoTextFx.hash01(i * 131L + 17L), 1.0D);
            double angle = OasisoTextFx.hash01(i * 197L + 53L) * Math.PI * 2.0D;
            double travel = 3.0D + OasisoTextFx.hash01(i * 83L + 7L) * 10.5D;
            double eased = OasisoTextFx.easeOutCubic(age);
            double originX = x + 1.0D + OasisoTextFx.hash01(i * 61L + 29L) * Math.max(1.0D, width - 2.0D);
            double originY = y + 1.0D + OasisoTextFx.hash01(i * 47L + 19L) * Math.max(1.0D, height - 2.0D);
            int px = (int) Math.round(originX + Math.cos(angle) * travel * eased);
            int py = (int) Math.round(originY + Math.sin(angle) * travel * eased);
            if (!cores) {
                double fade = Math.pow(1.0D - age, 2.0D);
                int alpha = Math.max(0, Math.min(92, (int) Math.round(92.0D * fade)));
                int color = OasisoTextFx.blend(palette.auraDark(), palette.auraBright(), Math.pow(1.0D - age, .55D));
                g.fill(px - 1, py - 1, px + 2, py + 2, alpha << 24 | color);
            } else {
                double fade = Math.pow(1.0D - age, 1.55D);
                int alpha = Math.max(0, Math.min(235, (int) Math.round(235.0D * fade)));
                int color = OasisoTextFx.blend(palette.coreDark(), palette.coreBright(), Math.pow(1.0D - age, .42D));
                if (age < .15D) color = OasisoTextFx.blend(color, palette.flash(), (.15D - age) / .15D * .35D);
                int size = age < .13D && OasisoTextFx.hash01(i * 23L + 3L) > .62D ? 2 : 1;
                g.fill(px, py, px + size, py + size, alpha << 24 | color);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        private Setup() {
        }

        @SubscribeEvent
        public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(OasisoAnimatedTooltip.class, Client::new);
        }
    }
}
