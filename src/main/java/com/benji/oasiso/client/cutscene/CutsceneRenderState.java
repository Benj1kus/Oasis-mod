package com.benji.oasiso.client.cutscene;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.*;

final class CutsceneRenderState implements AutoCloseable {
    private final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int[] viewport = new int[4], box = new int[4];
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int eq = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), eqA = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    private final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int[] textures = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)};
    private final float[] color = RenderSystem.getShaderColor().clone();
    private final ShaderInstance shader = RenderSystem.getShader();

    CutsceneRenderState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
    }

    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        RenderSystem.depthMask(write);
        RenderSystem.depthFunc(func);
        if (depth) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        if (blend) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
        if (cull) RenderSystem.enableCull();
        else RenderSystem.disableCull();
        if (scissor) RenderSystem.enableScissor(box[0], box[1], box[2], box[3]);
        else RenderSystem.disableScissor();
        GL20.glBlendEquationSeparate(eq, eqA);
        RenderSystem.blendFuncSeparate(src, dst, srcA, dstA);
        for (int i = 0; i < 3; i++) RenderSystem.setShaderTexture(i, textures[i]);
        RenderSystem.activeTexture(active);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        if (shader != null) RenderSystem.setShader(() -> shader);
    }
}
