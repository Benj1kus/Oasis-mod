package com.benji.oasiso.client.fluwirussort;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = "oasiso", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SortOutline {
    private static ShaderInstance shader;
    private static TextureTarget mask;
    private static boolean dirty = true;

    @SubscribeEvent
    public static void register(RegisterShadersEvent e) throws IOException {
        e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("oasiso", "sort_item_outline"), DefaultVertexFormat.POSITION_TEX), s -> {
            shader = s;
            invalidate();
        });
    }

    static void invalidate() {
        dirty = true;
    }

    static void release() {
        if (mask != null) mask.destroyBuffers();
        mask = null;
        dirty = true;
    }

    private static void capture(GuiGraphics g, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        g.flush();
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        g.pose().pushPose();
        try {
            if (mask == null) mask = new TextureTarget(96, 96, true, Minecraft.ON_OSX);
            mask.setClearColor(0, 0, 0, 0);
            mask.clear(Minecraft.ON_OSX);
            mask.bindWrite(true);
            mv.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 24, 24, 0, -4096, 4096), sorting);
            g.pose().setIdentity();
            g.renderItem(stack, 4, 4);
            g.flush();
            dirty = false;
        } finally {
            g.pose().popPose();
            mv.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    static void draw(GuiGraphics g, ItemStack stack, float x, float y, float scale) {
        if (shader == null || stack.isEmpty()) return;
        if (dirty) capture(g, stack);
        g.flush();
        float r = 12 * scale, cx = x + 8, cy = y + 8;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, mask.getColorTextureId());
        var matrix = g.pose().last().pose();
        var b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(matrix, cx - r, cy - r, 1).uv(0, 1).endVertex();
        b.vertex(matrix, cx - r, cy + r, 1).uv(0, 0).endVertex();
        b.vertex(matrix, cx + r, cy + r, 1).uv(1, 0).endVertex();
        b.vertex(matrix, cx + r, cy - r, 1).uv(1, 1).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
