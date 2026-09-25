package com.benji.oasiso.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;

final class ApollyonOutlinePass {
    private static int framebuffer, colorTexture, depthTexture, fieldFramebuffer, fieldTexture, width, height, fieldWidth, fieldHeight;
    private static int modelVao, modelVbo, screenVao, screenVbo;

    private ApollyonOutlinePass() {
    }

    static void draw(List<float[]> vertices, int texture, float minX, float minY, float maxX, float maxY, boolean crossesCamera, float time, float alpha) {
        State old = new State();
        try (MemoryStack memory = MemoryStack.stackPush()) {
            int w = old.viewport[2], h = old.viewport[3];
            if (w <= 0 || h <= 0) return;
            ensure(w, h);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL11.glViewport(0, 0, w, h);

            GL30.glClearBufferfv(GL11.GL_COLOR, 0, memory.floats(0, 0, 0, 0));
            GL30.glClearBufferfv(GL11.GL_DEPTH, 0, memory.floats(1));

            int mask = ApollyonShaders.mask.getId();
            GL20.glUseProgram(mask);
            matrix(mask, "ModelViewMat", RenderSystem.getModelViewMatrix(), memory);
            matrix(mask, "ProjMat", RenderSystem.getProjectionMatrix(), memory);
            GL20.glUniform1i(GL20.glGetUniformLocation(mask, "Sampler0"), 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            FloatBuffer data = MemoryUtil.memAllocFloat(vertices.size() * 5);
            try {
                for (float[] vertex : vertices) data.put(vertex);
                data.flip();
                bindMesh(modelVao, modelVbo, mask, data);
            } finally {
                MemoryUtil.memFree(data);
            }
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices.size());
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fieldFramebuffer);
            GL11.glViewport(0, 0, fieldWidth, fieldHeight);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, memory.floats(0, 0, 0, 0));

            if (!scissor(0, 0, fieldWidth, fieldHeight, null, minX, minY, maxX, maxY, crossesCamera, 22)) return;
            int horizontal = ApollyonShaders.distance.getId();
            GL20.glUseProgram(horizontal);
            GL20.glUniform1i(GL20.glGetUniformLocation(horizontal, "Sampler0"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(horizontal, "Sampler1"), 1);
            GL20.glUniform2f(GL20.glGetUniformLocation(horizontal, "FieldSize"), fieldWidth, fieldHeight);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
            screen(horizontal, memory);

            old.bindFramebuffer();
            toggle(GL30.GL_FRAMEBUFFER_SRGB, old.srgb);
            if (!scissor(old.viewport[0], old.viewport[1], w, h, old.scissor ? old.box : null, minX, minY, maxX, maxY, crossesCamera, 44))
                return;
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int outline = ApollyonShaders.outline.getId();
            GL20.glUseProgram(outline);
            GL20.glUniform1i(GL20.glGetUniformLocation(outline, "Sampler0"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(outline, "Sampler1"), 1);
            GL20.glUniform2f(GL20.glGetUniformLocation(outline, "FieldSize"), fieldWidth, fieldHeight);
            GL20.glUniform2f(GL20.glGetUniformLocation(outline, "Origin"), Float.isFinite(minX) ? (minX + maxX + 2F) * .25F * fieldWidth : fieldWidth * .5F, Float.isFinite(minY) ? (minY + maxY + 2F) * .25F * fieldHeight : fieldHeight * .5F);
            GL20.glUniform1f(GL20.glGetUniformLocation(outline, "WidthScale"), Math.max(.65F, Math.min(1.35F, h / 1080F)));
            GL20.glUniform1f(GL20.glGetUniformLocation(outline, "Time"), time);
            GL20.glUniform1f(GL20.glGetUniformLocation(outline, "Opacity"), alpha);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fieldTexture);
            screen(outline, memory);
        } finally {
            old.restore();
        }
    }

    private static void matrix(int program, String name, Matrix4f matrix, MemoryStack memory) {
        FloatBuffer value = memory.mallocFloat(16);
        matrix.get(value);
        GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, name), false, value);
    }

    private static void bindMesh(int vao, int vbo, int program, FloatBuffer vertices) {
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);
        int position = GL20.glGetAttribLocation(program, "Position");
        int uv = GL20.glGetAttribLocation(program, "UV0");
        GL20.glEnableVertexAttribArray(position);
        GL20.glVertexAttribPointer(position, 3, GL11.GL_FLOAT, false, 20, 0L);
        GL20.glEnableVertexAttribArray(uv);
        GL20.glVertexAttribPointer(uv, 2, GL11.GL_FLOAT, false, 20, 12L);
    }

    private static void screen(int program, MemoryStack memory) {
        bindMesh(screenVao, screenVbo, program, memory.floats(-1, -1, 0, 0, 0, 1, -1, 0, 1, 0, 1, 1, 0, 1, 1, -1, -1, 0, 0, 0, 1, 1, 0, 1, 1, -1, 1, 0, 0, 1));
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    private static boolean scissor(int vx, int vy, int w, int h, int[] box, float minX, float minY, float maxX, float maxY, boolean crossesCamera, float radius) {
        int x0 = vx, y0 = vy, x1 = vx + w, y1 = vy + h;
        if (!crossesCamera) {
            if (!Float.isFinite(minX)) return false;
            int pad = (int) Math.ceil(radius) + 2;
            x0 = Math.max(x0, vx + (int) Math.floor((minX * .5F + .5F) * w) - pad);
            x1 = Math.min(x1, vx + (int) Math.ceil((maxX * .5F + .5F) * w) + pad);
            y0 = Math.max(y0, vy + (int) Math.floor((minY * .5F + .5F) * h) - pad);
            y1 = Math.min(y1, vy + (int) Math.ceil((maxY * .5F + .5F) * h) + pad);
        }
        if (box != null) {
            x0 = Math.max(x0, box[0]);
            y0 = Math.max(y0, box[1]);
            x1 = Math.min(x1, box[0] + box[2]);
            y1 = Math.min(y1, box[1] + box[3]);
        }
        if (x1 <= x0 || y1 <= y0) return false;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x0, y0, x1 - x0, y1 - y0);
        return true;
    }

    private static void ensure(int w, int h) {
        if (modelVao == 0) {
            modelVao = GL30.glGenVertexArrays();
            modelVbo = GL15.glGenBuffers();
            screenVao = GL30.glGenVertexArrays();
            screenVbo = GL15.glGenBuffers();
        }
        if (framebuffer != 0 && width == w && height == h) return;
        deleteTarget();
        width = w;
        height = h;
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        colorTexture = texture(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, w, h);
        depthTexture = texture(GL14.GL_DEPTH_COMPONENT24, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, w, h);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Entropy Creature mask framebuffer is incomplete");
        fieldWidth = Math.max(1, (w + 1) / 2);
        fieldHeight = Math.max(1, (h + 1) / 2);
        fieldFramebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fieldFramebuffer);
        fieldTexture = texture(GL30.GL_RGBA32F, GL11.GL_RGBA, GL11.GL_FLOAT, fieldWidth, fieldHeight);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fieldTexture, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Entropy Creature distance framebuffer is incomplete");
    }

    private static int texture(int internal, int format, int type, int w, int h) {
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internal, w, h, 0, format, type, 0L);
        return id;
    }

    private static void deleteTarget() {
        if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
        if (colorTexture != 0) GL11.glDeleteTextures(colorTexture);
        if (depthTexture != 0) GL11.glDeleteTextures(depthTexture);
        if (fieldFramebuffer != 0) GL30.glDeleteFramebuffers(fieldFramebuffer);
        if (fieldTexture != 0) GL11.glDeleteTextures(fieldTexture);
        framebuffer = colorTexture = depthTexture = fieldFramebuffer = fieldTexture = 0;
    }

    static void release() {
        deleteTarget();
        if (modelVao != 0) GL30.glDeleteVertexArrays(modelVao);
        if (screenVao != 0) GL30.glDeleteVertexArrays(screenVao);
        if (modelVbo != 0) GL15.glDeleteBuffers(modelVbo);
        if (screenVbo != 0) GL15.glDeleteBuffers(screenVbo);
        modelVao = screenVao = modelVbo = screenVbo = 0;
    }

    private static void toggle(int cap, boolean enabled) {
        if (enabled) GL11.glEnable(cap);
        else GL11.glDisable(cap);
    }

    private static final class State {
        final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        final int vbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        final int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final int[] textures = new int[2], viewport = new int[4], box = new int[4], colorWrite = new int[4];
        final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        final boolean polygon = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL), logic = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
        final boolean srgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);
        final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        final int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), equationA = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);

        State() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorWrite);
            for (int i = 0; i < 2; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GL13.glActiveTexture(active);
        }

        void bindFramebuffer() {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }

        void restore() {
            bindFramebuffer();
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
            for (int i = 0; i < 2; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            }
            GL13.glActiveTexture(active);
            GL11.glDepthMask(depthWrite);
            GL11.glDepthFunc(depthFunc);
            GL20.glBlendEquationSeparate(equationRgb, equationA);
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcA, dstA);
            toggle(GL11.GL_DEPTH_TEST, depth);
            toggle(GL11.GL_BLEND, blend);
            toggle(GL11.GL_CULL_FACE, cull);
            toggle(GL11.GL_SCISSOR_TEST, scissor);
            toggle(GL11.GL_STENCIL_TEST, stencil);
            toggle(GL11.GL_POLYGON_OFFSET_FILL, polygon);
            toggle(GL11.GL_COLOR_LOGIC_OP, logic);
            toggle(GL30.GL_FRAMEBUFFER_SRGB, srgb);
            GL11.glScissor(box[0], box[1], box[2], box[3]);
            GL11.glColorMask(colorWrite[0] != 0, colorWrite[1] != 0, colorWrite[2] != 0, colorWrite[3] != 0);
        }
    }
}
