package com.benji.oasiso.client.configscroll;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigScrollEffects implements AutoCloseable {
    private static ShaderInstance backdrop;
    private PostChain blur;
    private int targetWidth, targetHeight;
    private boolean failed;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("oasiso", "config_scroll_backdrop"), DefaultVertexFormat.POSITION_TEX), shader -> backdrop = shader);
        } catch (Exception ex) { LogUtils.getLogger().warn("Config Scroll backdrop unavailable; using plain background", ex); }
    }

    void render(GuiGraphics g, int width, int height, float time, float entrance, int accent, float partialTick) {
        g.flush();
        Minecraft mc = Minecraft.getInstance();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        try {
            if (!failed) {
                if (blur == null) {
                    blur = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), ResourceLocation.fromNamespaceAndPath("oasiso", "shaders/post/config_scroll_blur.json"));
                    targetWidth = targetHeight = 0;
                }
                int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
                if (w != targetWidth || h != targetHeight) { blur.resize(w, h); targetWidth = w; targetHeight = h; }
                blur.process(partialTick);
            }
        } catch (Exception ex) {
            failed = true;
            close();
            LogUtils.getLogger().warn("Config Scroll blur unavailable; the editor is still usable", ex);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.depthMask(true);
            RenderSystem.clearDepth(1.0D);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);

            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        if (backdrop == null) {
            g.fill(0, 0, width, height, 0x6603070C);
            g.fillGradient((int) (width * .36), 0, width, height, 0xE00B222B, 0xEB03080E);
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> backdrop);
        backdrop.safeGetUniform("Time").set(time);
        backdrop.safeGetUniform("Entrance").set(entrance);
        backdrop.safeGetUniform("Size").set((float) width, (float) height);
        backdrop.safeGetUniform("Accent").set(((accent >> 16) & 255) / 255F, ((accent >> 8) & 255) / 255F, (accent & 255) / 255F);
        Matrix4f matrix = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(matrix, 0, height, 0).uv(0, 1).endVertex();
        b.vertex(matrix, width, height, 0).uv(1, 1).endVertex();
        b.vertex(matrix, width, 0, 0).uv(1, 0).endVertex();
        b.vertex(matrix, 0, 0, 0).uv(0, 0).endVertex();
        BufferUploader.drawWithShader(b.end());
        RenderSystem.disableBlend();
    }

    @Override public void close() {
        if (blur != null) { blur.close(); blur = null; }
    }
}
