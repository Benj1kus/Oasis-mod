package com.benji.oasiso.client.store;

import com.benji.oasiso.common.store.ScienceStoreOffers;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

final class ScienceStoreIcons implements AutoCloseable {
    private TextureTarget target;
    private double lastRender = -100;
    private final ItemStack[] items = {ScienceStoreOffers.stack(0), ScienceStoreOffers.stack(1), ScienceStoreOffers.stack(2), new ItemStack(ScienceStoreOffers.currency())};

    void update(GuiGraphics main, double time) {
        if (time - lastRender < .10) return;
        Minecraft mc = Minecraft.getInstance();
        main.flush();
        if (target == null) {
            target = new TextureTarget(128, 128, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            target.setFilterMode(9728); // GL_NEAREST: preserve item pixels.
        }
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        ShaderInstance previous = RenderSystem.getShader();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 128, 128, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            modelView.setIdentity();
            modelView.translate(0, 0, -11000);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            for (int i = 0; i < items.length; i++) {
                g.pose().pushPose();
                g.pose().translate((i % 2) * 64 + 8, (i / 2) * 64 + 8, 0);
                g.pose().scale(3, 3, 1);
                g.renderItem(items[i], 0, 0);
                g.flush();
                g.pose().popPose();
            }
            lastRender = time;
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
            mc.getMainRenderTarget().bindWrite(true);
            if (previous != null) RenderSystem.setShader(() -> previous);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    void draw(GuiGraphics g, int item, float x, float y, float size, double time, float flash, float opacity) {
        ShaderInstance shader = ScienceStoreShaders.icon;
        if (target == null || shader == null) {
            g.pose().pushPose();
            g.pose().translate(x + size * .125F, y + size * .125F, 0);
            g.pose().scale(size * .75F / 16, size * .75F / 16, 1);
            g.renderItem(items[item], 0, 0);
            g.flush();
            g.pose().popPose();
            return;
        }
        g.flush();
        shader.safeGetUniform("TexelSize").set(1F / 128, 1F / 128);
        shader.safeGetUniform("OutlineWidth").set(item == 3 ? 1F : 3.3F + .65F * (float) Math.sin(time * 3.4));
        shader.safeGetUniform("Flash").set(flash);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        float u = (item % 2) * .5F, v = 1 - (item / 2) * .5F;
        ScienceStoreDrawing.quad(g, shader, x, y, size, size, u, v, u + .5F, v - .5F, opacity);
    }

    @Override
    public void close() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
