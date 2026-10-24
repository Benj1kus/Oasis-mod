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
        int w = s.width, h = s.height, a = color(s.string("checker_a", "#7F38D9")), c = color(s.string("checker_b", "#43DFF5")), edge = color(s.string("edge_color", "#9957E5"));
        double reveal = clamp(time / .75);
        BufferBuilder b = begin(g);
        Matrix4f m = g.pose().last().pose();
        int cell = 3;
        for (int y = 0; y < h; y += cell)
            for (int x = 0; x < w; x += cell) {
                double nx = x / (double) w, ny = y / (double) h, along = (nx + 1 - ny) * .5, cross = nx + ny - 1;
                double band = clamp(s.number("checker_width", .30)), soft = clamp((band - Math.abs(cross)) / .035);
                double dith = (BAYER[((x / cell) & 3) + 4 * ((y / cell) & 3)] + .5) / 16.0;
                if (along < reveal * 1.3 && Math.abs(cross) < band && soft > dith) {
                    int check = ((int) Math.floor((along * 10 - time * .55)) + (int) Math.floor(cross * 12)) & 1;
                    double tint = (Math.sin(along * 5 - time * 1.5) + 1) * .5;
                    int rgb = mix(a, c, tint);
                    if (check == 0) rgb = mix(rgb, 0x120922, .7);
                    quad(b, m, x, y, cell, cell, rgb, reveal * (.78 + .2 * soft));
                }

            }
        end(b);
        bottomGradient(g, s, time);
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
