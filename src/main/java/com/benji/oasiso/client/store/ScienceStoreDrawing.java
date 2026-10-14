package com.benji.oasiso.client.store;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

final class ScienceStoreDrawing {
    static void quad(GuiGraphics g, ShaderInstance shader, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float alpha) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(() -> shader);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(m, x, y + h, 0).uv(u0, v1).color(1F, 1F, 1F, alpha).endVertex();
        b.vertex(m, x + w, y + h, 0).uv(u1, v1).color(1F, 1F, 1F, alpha).endVertex();
        b.vertex(m, x + w, y, 0).uv(u1, v0).color(1F, 1F, 1F, alpha).endVertex();
        b.vertex(m, x, y, 0).uv(u0, v0).color(1F, 1F, 1F, alpha).endVertex();
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private ScienceStoreDrawing() {
    }
}
