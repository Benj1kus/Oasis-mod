package com.benji.oasiso.client.cutscene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

final class CutsceneVisuals {
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double hash(int a) {
        return ((a * 1103515245 + 12345) >>> 8 & 65535) / 65535.0;
    }

    private static int color(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0x994fff;
        }
    }

    private static int mix(int a, int b, double t) {
        int out = 0;
        for (int sh = 0; sh <= 16; sh += 8) out |= (int) (((a >> sh) & 255) * (1 - t) + ((b >> sh) & 255) * t) << sh;
        return out;
    }

    private static void quad(BufferBuilder b, Matrix4f m, float x, float y, float w, float h, int rgb, double a) {
        int alpha = (int) (255 * clamp(a));
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255;
        b.vertex(m, x, y, 0).color(r, g, bl, alpha).endVertex();
        b.vertex(m, x, y + h, 0).color(r, g, bl, alpha).endVertex();
        b.vertex(m, x + w, y + h, 0).color(r, g, bl, alpha).endVertex();
        b.vertex(m, x + w, y, 0).color(r, g, bl, alpha).endVertex();
    }

    private static BufferBuilder begin(GuiGraphics g) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        return b;
    }

    private static void end(BufferBuilder b) {
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    static void wipe(GuiGraphics g, int w, int h, double coverage) {
        if (coverage <= 0) return;
        if (coverage >= .999) {
            g.fill(0, 0, w, h, 0xff000000);
            return;
        }
        try (CutsceneRenderState saved = new CutsceneRenderState()) {
            BufferBuilder b = begin(g);
            Matrix4f m = g.pose().last().pose();
            RenderSystem.disableCull();
            CutsceneWeb.emit(w, h, coverage, (ax, ay, bx, by, cx, cy, dx, dy) -> {
                b.vertex(m, ax, ay, 0).color(0, 0, 0, 255).endVertex();
                b.vertex(m, bx, by, 0).color(0, 0, 0, 255).endVertex();
                b.vertex(m, cx, cy, 0).color(0, 0, 0, 255).endVertex();
                b.vertex(m, dx, dy, 0).color(0, 0, 0, 255).endVertex();
            });
            end(b);
        }
    }

    static void title(GuiGraphics g, CutsceneScreen s, int mx, int my, double time) {
        int w = s.width, h = s.height;
        backdrop(g, s, time);
        float tw = (float) Math.min(w * .44, h * .75), th = tw * 136 / 356;
        String align = s.string("title_position", "left");
        float cx = align.equals("right") ? w * .74F : align.equals("center") ? w * .5F : w * .26F, cy = h * .5F;
        float hover = Math.abs(mx - cx) < tw / 2 && Math.abs(my - cy) < th / 2 ? (mx - cx) / tw * 5 : 0;
        g.pose().pushPose();
        g.pose().translate(cx, cy + Math.sin(time * 1.8) * 3, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(hover));
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ResourceLocation.tryParse(s.string("title", "oasiso:textures/gui/apol_title.png")));
        BufferBuilder tex = Tesselator.getInstance().getBuilder();
        tex.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f mat = g.pose().last().pose();
        for (int yy = 0; yy < 136; yy += 4)
            for (int xx = 0; xx < 356; xx += 4) {
                float u = xx / 356F, v = yy / 136F, u2 = Math.min(356, xx + 4) / 356F, v2 = Math.min(136, yy + 4) / 136F;
                double radius = Math.sqrt(Math.pow((u - .5) * 2, 2) + Math.pow((v - .5) * 2, 2));
                float alpha = (float) clamp((time * 2.2 - radius - hash(xx + yy * 356) * .18) * 3);
                if (alpha <= 0) continue;
                float x = (u - .5F) * tw, y = (v - .5F) * th, x2 = (u2 - .5F) * tw, y2 = (v2 - .5F) * th;
                tex.vertex(mat, x, y, 0).uv(u, v).color(1F, 1F, 1F, alpha).endVertex();
                tex.vertex(mat, x, y2, 0).uv(u, v2).color(1F, 1F, 1F, alpha).endVertex();
                tex.vertex(mat, x2, y2, 0).uv(u2, v2).color(1F, 1F, 1F, alpha).endVertex();
                tex.vertex(mat, x2, y, 0).uv(u2, v).color(1F, 1F, 1F, alpha).endVertex();
            }
        BufferUploader.drawWithShader(tex.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        g.pose().popPose();
    }


    static void backdrop(GuiGraphics g, CutsceneScreen s, double time) {
        int w = s.width, h = s.height;
        if (w <= 0 || h <= 0) return;
        double reveal = clamp(time / .85);
        reveal = reveal * reveal * (3 - 2 * reveal);
        int ca = color(s.string("checker_a", "#7F38D9"));
        int cb = color(s.string("checker_b", "#43DFF5"));
        int light = color(s.string("flare_color", "#AC64FF"));
        double bandWidth = Math.max(.04, Math.min(.8, s.number("checker_width", .30)));
        double bandOpacity = clamp(s.number("checker_opacity", .96));
        double spiralScale = Math.max(.2, Math.min(2, s.number("spiral_scale", .75)));
        double spiralWidth = Math.max(.02, Math.min(.95, s.number("spiral_width", .23)));
        double spiralOpacity = clamp(s.number("spiral_opacity", .48));
        int spiralOutline = color(s.string("spiral_outline_color", "#AC64FF"));
        double outlineThickness = Math.max(0, Math.min(.5, s.number("spiral_outline_thickness", .12)));
        double outlineOpacity = clamp(s.number("spiral_outline_opacity", .85));
        double gradientProgress = clamp(time / .9);
        double gradientSlide = gradientProgress * gradientProgress * (3 - 2 * gradientProgress);
        double gradientHeight = h * Math.max(.1, Math.min(.9, s.number("bottom_gradient_height", .5)));
        double gradientTop = h - gradientHeight * gradientSlide;
        double gradientOpacity = clamp(s.number("bottom_gradient_opacity", .95));
        double speed = s.number("spiral_speed", .22);
        double jitter = s.number("spiral_jitter", .012);
        int step = (int) Math.floor(time * 12);
        double twitch = (hash(step + 523) - .5) * jitter;
        double rotation = time * s.number("flare_speed", .16);
        double cx = s.flareX + s.number("flare_offset_x", 0) * w;
        double cy = s.flareY + s.number("flare_offset_y", 0) * h;
        double radius = h * Math.max(.05, Math.min(2, s.number("flare_radius", .43)));
        double intensity = clamp(s.number("flare_opacity", 1));
        int cell = Math.max(2, (int) Math.ceil(h / 180.0));
        try (CutsceneRenderState saved = new CutsceneRenderState()) {
            BufferBuilder b = begin(g);
            RenderSystem.disableCull();
            Matrix4f m = g.pose().last().pose();
            for (int y = 0; y < h; y += cell) {
                double ny = (y + cell * .5) / h;
                for (int x = 0; x < w; x += cell) {
                    double px = x + cell * .5, py = y + cell * .5;
                    double dx = (px - w * .5) / h, dy = (py - h * .47) / h;
                    double angle = Math.atan2(dy, dx);
                    double sector = Math.PI * 2 / 10;
                    double polygonRadius = Math.hypot(dx, dy) * Math.cos((angle + Math.PI * 4 + sector * .5) % sector - sector * .5);
                    double phase = angle * 3 - polygonRadius * 14 / spiralScale - time * speed + twitch;
                    double stripe = phase / (Math.PI * 2);
                    stripe -= Math.floor(stripe);
                    float qw = Math.min(cell, w - x), qh = Math.min(cell, h - y);
                    double dither = (BAYER[((x / cell) & 3) + 4 * ((y / cell) & 3)] + .5) / 16;
                    double nx = px / w;
                    double along = (nx + 1 - ny) * .5;
                    double cross = nx + ny - 1;
                    double edgeFade = clamp((bandWidth - Math.abs(cross)) / .035);
                    if (Math.abs(cross) < bandWidth && edgeFade > dither && along < reveal * 1.3) {
                        int check = ((int) Math.floor(along * 10 - time * .55) + (int) Math.floor(cross * 12)) & 1;
                        int rgb = mix(ca, cb, (Math.sin(along * 5 - time * 1.5) + 1) * .5);
                        if (check == 0) rgb = mix(rgb, 0x120922, .7);
                        quad(b, m, x, y, qw, qh, rgb, reveal * bandOpacity);
                    }
                    if (Math.abs(cross) >= bandWidth && stripe < spiralWidth) {
                        double inset = Math.min(stripe, spiralWidth - stripe);
                        boolean outline = outlineThickness > 0 && inset < spiralWidth * outlineThickness;
                        quad(b, m, x, y, qw, qh, outline ? spiralOutline : 0x000000, reveal * (outline ? outlineOpacity : spiralOpacity));
                    }
                    if (py > gradientTop) {
                        double vertical = clamp((py - gradientTop) / gradientHeight);
                        double alpha = Math.pow(vertical, 1.25) * gradientOpacity;
                        double dithered = Math.floor(alpha * 4 + dither) / 4;
                        if (dithered > 0) quad(b, m, x, y, qw, qh, 0x000000, dithered);
                    }
                    if (s.flareValid) {
                        double fx = (px - cx) / radius, fy = (py - cy) / radius;
                        double r = Math.hypot(fx, fy);
                        double theta = Math.atan2(fy, fx) - rotation;
                        double tooth = Math.abs(2 * fract(theta * 7 / (Math.PI * 2)) - 1);
                        double boundary = .36 + .64 * Math.pow(tooth, 3);
                        double glow = clamp((boundary - r) / .24) * clamp(1.4 - r) * intensity;
                        double core = clamp(1 - r / .44) * intensity;
                        double opacity = Math.max(glow, core);
                        opacity = Math.floor(opacity * 4 + dither) / 4;
                        int flareTint = mix(light, 0xFFFFFF, clamp(1 - r / .42) * .65);
                        if (opacity > 0) quad(b, m, x, y, qw, qh, flareTint, reveal * clamp(opacity));
                    }

                }
            }
            end(b);
        }
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    static void bottomGradient(GuiGraphics g, CutsceneScreen s, double time) {
        double t = clamp(time / .9);
        if (t <= 0) return;
        double slide = t * t * (3 - 2 * t);
        double fraction = Math.max(.1, Math.min(.9, s.number("bottom_gradient_height", .50)));
        double opacity = clamp(s.number("bottom_gradient_opacity", .95));
        double gradientHeight = s.height * fraction, top = s.height - gradientHeight * slide;
        int cell = 3;
        try (CutsceneRenderState saved = new CutsceneRenderState()) {
            BufferBuilder b = begin(g);
            Matrix4f m = g.pose().last().pose();
            RenderSystem.disableCull();
            for (int y = Math.max(0, (int) Math.floor(top / cell) * cell); y < s.height; y += cell) {
                double vertical = clamp((y + cell * .5 - top) / gradientHeight);
                double alpha = Math.pow(vertical, 1.25) * opacity;
                for (int x = 0; x < s.width; x += cell) {
                    double threshold = (BAYER[((x / cell) & 3) + 4 * ((y / cell) & 3)] + .5) / 16.0;
                    double dithered = Math.floor(alpha * 4 + threshold) / 4;
                    if (dithered > 0)
                        quad(b, m, x, y, Math.min(cell, s.width - x), Math.min(cell, s.height - y), 0, dithered);
                }
            }
            end(b);
        }
    }

    static void edges(GuiGraphics g, CutsceneScreen s, double time) {
        int w = s.width, h = s.height, edge = color(s.string("edge_color", "#9957E5"));
        double reveal = clamp(time / .75);
        if (reveal <= 0) return;
        BufferBuilder b = begin(g);
        Matrix4f m = g.pose().last().pose();
        int cell = 3;
        for (int y = 0; y < h; y += cell)
            for (int x = 0; x < w; x += cell) {
                double nx = x / (double) w, ny = y / (double) h;
                double dith = (BAYER[((x / cell) & 3) + 4 * ((y / cell) & 3)] + .5) / 16.0;
                double dist = Math.min(Math.min(nx, 1 - nx), Math.min(ny, 1 - ny));
                double coord = ny < .1 || ny > .9 ? nx : ny;
                int spike = (int) (coord * 24);
                double within = coord * 24 - spike;
                double length = (.025 + hash(spike + 77) * .1) * (1 - Math.abs(within - .5) * 2);
                double opacity = clamp(1 - dist / Math.max(.001, length)) * reveal;
                if (dist < length && opacity > dith) quad(b, m, x, y, cell, cell, edge, opacity * .8);
            }
        end(b);
    }

}
