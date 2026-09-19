package com.benji.oasiso.client.configscroll;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

final class ConfigScrollDraw implements AutoCloseable {
    private final BufferBuilder buffer;
    private final Matrix4f matrix;
    ConfigScrollDraw(GuiGraphics graphics) {
        graphics.flush();
        matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    }
    private void vertex(double x, double y, int color) {
        buffer.vertex(matrix, (float) x, (float) y, 0).color((color >> 16) & 255, (color >> 8) & 255, color & 255, color >>> 24).endVertex();
    }
    void triangle(double ax, double ay, double bx, double by, double cx, double cy, int color) {
        vertex(ax, ay, color); vertex(bx, by, color); vertex(cx, cy, color);
    }
    void line(double ax, double ay, double bx, double by, double thickness, int color) {
        double length = Math.max(.001, Math.hypot(bx - ax, by - ay));
        double nx = -(by - ay) / length * thickness * .5, ny = (bx - ax) / length * thickness * .5;
        triangle(ax + nx, ay + ny, bx + nx, by + ny, bx - nx, by - ny, color);
        triangle(ax + nx, ay + ny, bx - nx, by - ny, ax - nx, ay - ny, color);
    }
    void ring(double cx, double cy, double radius, double thickness, double start, double end, int color) {
        if (end <= start) return;
        int steps = Math.max(8, (int) Math.ceil((end - start) * radius / 3));
        double inner = Math.max(0, radius - thickness * .5), outer = radius + thickness * .5;
        for (int i = 0; i < steps; i++) {
            double a = start + (end - start) * i / steps, b = start + (end - start) * (i + 1) / steps;
            double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
            triangle(cx + ca * inner, cy + sa * inner, cx + ca * outer, cy + sa * outer, cx + cb * outer, cy + sb * outer, color);
            triangle(cx + ca * inner, cy + sa * inner, cx + cb * outer, cy + sb * outer, cx + cb * inner, cy + sb * inner, color);
        }
    }
    void circle(double x, double y, double r, int color) { ring(x, y, r / 2, r, 0, Math.PI * 2, color); }
    void diamond(double x, double y, double r, int color) {
        triangle(x, y - r, x + r * .55, y, x, y + r, color);
        triangle(x, y - r, x, y + r, x - r * .55, y, color);
    }
    @Override public void close() {
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    static int alpha(int rgb, double opacity) {
        return ((int) Math.round(Math.max(0, Math.min(1, opacity)) * 255) << 24) | (rgb & 0xFFFFFF);
    }
    static int mix(int a, int b, double fraction) {
        double f = Math.max(0, Math.min(1, fraction));
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return r << 16 | g << 8 | bl;
    }
}
