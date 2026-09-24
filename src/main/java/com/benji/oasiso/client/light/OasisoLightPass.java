package com.benji.oasiso.client.light;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;

final class OasisoLightPass {
    private static int sceneTexture, depthTexture, shadowTexture, vao, vbo, width, height;

    private OasisoLightPass() {
    }

    static void draw(List<OasisoLocalLights.Source> sources, Vec3 camera, Matrix4f inverse, float time, long ticks) {
        State old = new State();
        try (MemoryStack memory = MemoryStack.stackPush()) {
            int w = old.viewport[2], h = old.viewport[3];
            if (w <= 0 || h <= 0 || sources.isEmpty()) return;

            if (GL11.glGetInteger(GL13.GL_SAMPLES) > 0) return;
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            ensure(w, h);

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, old.draw);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, old.viewport[0], old.viewport[1], w, h);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, old.viewport[0], old.viewport[1], w, h);
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowTexture);
            int size = OasisoLightStyles.SHADOW_SIZE, atlasWidth = size * 6, atlasHeight = size * 4;
            FloatBuffer atlas = memory.mallocFloat(atlasWidth * atlasHeight);
            for (int i = 0; i < atlasWidth * atlasHeight; i++) atlas.put(i, 0.0F);
            for (int light = 0; light < sources.size(); light++) {
                float[] data = sources.get(light).shadow;
                for (int face = 0; face < 6; face++)
                    for (int y = 0; y < size; y++)
                        for (int x = 0; x < size; x++)
                            atlas.put((light * size + y) * atlasWidth + face * size + x, data[face * size * size + y * size + x]);
            }
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, atlasWidth, atlasHeight, GL11.GL_RED, GL11.GL_FLOAT, atlas);

            int program = OasisoLightShaders.light.getId();
            GL20.glUseProgram(program);
            GL20.glUniform1i(location(program, "Scene"), 0);
            GL20.glUniform1i(location(program, "Depth"), 1);
            GL20.glUniform1i(location(program, "Shadows"), 2);
            GL20.glUniform1i(location(program, "Count"), sources.size());
            GL20.glUniform1f(location(program, "Time"), time);
            GL20.glUniform2f(location(program, "Resolution"), w, h);
            GL20.glUniform3f(location(program, "CameraCell"), cell(camera.x), cell(camera.y), cell(camera.z));
            FloatBuffer matrix = memory.mallocFloat(16);
            inverse.get(matrix);
            GL20.glUniformMatrix4fv(location(program, "InverseViewProjection"), false, matrix);
            FloatBuffer positions = memory.mallocFloat(16), colors = memory.mallocFloat(16), accents = memory.mallocFloat(16), settings = memory.mallocFloat(16);
            for (var source : sources) {
                var style = source.style;
                float distance = (float) source.center.distanceTo(camera);
                float fade = Math.max(0, Math.min(1, (24.0F - distance) / 4.0F));
                fade *= Math.max(0, Math.min(1, (ticks - source.firstReady) / 10.0F));
                positions.put((float) (source.center.x - camera.x)).put((float) (source.center.y - camera.y)).put((float) (source.center.z - camera.z)).put(style.radius());
                color(colors, style.color(), style.strength() * fade);
                color(accents, style.accent(), style.fogRadius());
                settings.put(style.fogDensity()).put(style.halo()).put(fade).put((source.pos.asLong() & 1023L) * 0.137F);
            }
            positions.flip();
            colors.flip();
            accents.flip();
            settings.flip();
            GL20.glUniform4fv(location(program, "LightPosition[0]"), positions);
            GL20.glUniform4fv(location(program, "LightColor[0]"), colors);
            GL20.glUniform4fv(location(program, "LightAccent[0]"), accents);
            GL20.glUniform4fv(location(program, "LightSettings[0]"), settings);

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            GL11.glColorMask(true, true, true, true);

            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            int position = GL20.glGetAttribLocation(program, "Position"), uv = GL20.glGetAttribLocation(program, "UV0");
            GL20.glEnableVertexAttribArray(position);
            GL20.glVertexAttribPointer(position, 3, GL11.GL_FLOAT, false, 20, 0L);
            GL20.glEnableVertexAttribArray(uv);
            GL20.glVertexAttribPointer(uv, 2, GL11.GL_FLOAT, false, 20, 12L);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        } finally {
            old.restore();
        }
    }

    private static int location(int program, String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    private static float cell(double coordinate) {
        return (float) (coordinate - Math.floor(coordinate / 256.0) * 256.0);
    }

    private static void color(FloatBuffer out, int rgb, float alpha) {
        out.put(((rgb >> 16) & 255) / 255.0F).put(((rgb >> 8) & 255) / 255.0F).put((rgb & 255) / 255.0F).put(alpha);
    }

    private static void ensure(int w, int h) {
        if (vao == 0) {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{-1, -1, 0, 0, 0, 1, -1, 0, 1, 0, 1, 1, 0, 1, 1, -1, -1, 0, 0, 0, 1, 1, 0, 1, 1, -1, 1, 0, 0, 1}, GL15.GL_STATIC_DRAW);
        }
        if (sceneTexture != 0 && width == w && height == h) return;
        deleteTextures();
        width = w;
        height = h;
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        sceneTexture = texture(GL11.GL_RGBA8, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
        depthTexture = texture(GL30.GL_DEPTH_COMPONENT32F, w, h, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT);
        shadowTexture = texture(GL30.GL_R32F, OasisoLightStyles.SHADOW_SIZE * 6, OasisoLightStyles.SHADOW_SIZE * 4, GL11.GL_RED, GL11.GL_FLOAT);
    }

    private static int texture(int internal, int w, int h, int format, int type) {
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

    private static void deleteTextures() {
        if (sceneTexture != 0) GL11.glDeleteTextures(sceneTexture);
        if (depthTexture != 0) GL11.glDeleteTextures(depthTexture);
        if (shadowTexture != 0) GL11.glDeleteTextures(shadowTexture);
        sceneTexture = depthTexture = shadowTexture = 0;
    }

    static void release() {
        deleteTextures();
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (vbo != 0) GL15.glDeleteBuffers(vbo);
        vao = vbo = 0;
    }

    private static void toggle(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }

    private static final class State {
        final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        final int vbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        final int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final int[] textures = new int[3], viewport = new int[4], box = new int[4], colorWrite = new int[4];
        final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        final boolean polygon = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL), logic = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
        final boolean srgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);
        final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        final int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), equationA = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        final int unpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        final int unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        final int unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        final int unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);

        State() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, colorWrite);
            for (int i = 0; i < 3; i++) {
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
            for (int i = 0; i < 3; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            }
            GL13.glActiveTexture(active);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
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
