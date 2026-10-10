package com.benji.oasiso.client.fountain;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EntropyFountainRenderer {
    private static ShaderInstance shader;
    private static int blobLocation;
    private static final BufferBuilder BUILDER = new BufferBuilder(4096);
    private static final Map<EntropyFountainLayout.Fountain, Long> FIRST_SEEN = new HashMap<>();
    private static ClientLevel seenLevel, frameLevel;
    private static Matrix4f frameView, frameProjection;
    private static Vec3 frameCamera;

    private static Vec3 center(EntropyFountainLayout.Fountain f) {
        return vector(f.center());
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            frameView = new Matrix4f(event.getPoseStack().last().pose());
            frameProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            frameCamera = event.getCamera().getPosition();
            frameLevel = mc.level;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = frameView, projection = frameProjection;
        Vec3 camera = frameCamera;
        ClientLevel level = frameLevel;
        frameView = frameProjection = null;
        frameCamera = null;
        frameLevel = null;
        if (seenLevel != mc.level) {
            FIRST_SEEN.clear();
            seenLevel = mc.level;
        }
        if (shader == null || level == null || level != mc.level || view == null || projection == null || camera == null)
            return;
        var all = EntropyFountainClient.fountains();
        FIRST_SEEN.keySet().retainAll(all);
        List<EntropyFountainLayout.Fountain> visible = new ArrayList<>();
        for (var f : all) {
            Vec3 c = center(f);
            if (c.distanceToSqr(camera) > EntropyFountainClient.VIEW_DISTANCE * EntropyFountainClient.VIEW_DISTANCE)
                continue;
            if (!event.getFrustum().isVisible(new AABB(vector(f.min()), vector(f.max())))) continue;
            visible.add(f);
        }
        visible.sort(Comparator.comparingDouble(f -> center(f).distanceToSqr(camera)));
        if (visible.size() > EntropyFountainClient.MAX_VISIBLE)
            visible = new ArrayList<>(visible.subList(0, EntropyFountainClient.MAX_VISIBLE));
        if (visible.isEmpty()) return;
        Collections.reverse(visible);
        shader.safeGetUniform("FountainViewProjection").set(new Matrix4f(projection).mul(view));
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC), program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int eqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), eqAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        ShaderInstance previous = RenderSystem.getShader();
        try (MemoryStack memory = MemoryStack.stackPush()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
            double clock = (level.getGameTime() % 1_000_000L + partial) / 20.0;
            var buffer = memory.mallocFloat(EntropyFountainPattern.MAX_BLOBS * 4);
            for (var f : visible) {
                long first = FIRST_SEEN.computeIfAbsent(f, k -> level.getGameTime());
                float fade = (float) Math.max(0, Math.min(1, (level.getGameTime() - first + partial) / 12.0));
                fade *= (float) Math.max(0, Math.min(1, (32 - center(f).distanceTo(camera)) / 4));
                float width = f.size() * .94F;
                float[] blobs = EntropyFountainPattern.build(clock, f.height(), width, f.seed());
                buffer.clear();
                buffer.put(blobs);
                buffer.flip();
                GL20.glUseProgram(shader.getId());
                GL20.glUniform4fv(blobLocation, buffer);
                shader.safeGetUniform("BlobCount").set(blobs.length / 4);
                shader.safeGetUniform("Time").set((float) clock);
                shader.safeGetUniform("Seed").set((float) f.seed());
                shader.safeGetUniform("Reveal").set(fade);
                shader.safeGetUniform("Dimensions").set(width, (float) f.height());
                Vec3 base = vector(f.start()).subtract(camera);
                Vec3 along = vector(f.direction());
                Vec3 right = along.cross(base);
                if (right.lengthSqr() < 1.0e-8) {
                    right = f.axis() == EntropyFountainLayout.Axis.X ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                } else right = right.normalize();
                double half = width * .5, h = f.height() - .006;
                BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                vertex(base, right, along, -half, 0, 0, 0);
                vertex(base, right, along, half, 0, 1, 0);
                vertex(base, right, along, half, h, 1, 1);
                vertex(base, right, along, -half, h, 0, 1);
                BufferUploader.drawWithShader(BUILDER.end());
            }
        } finally {
            RenderSystem.depthMask(mask);
            RenderSystem.depthFunc(func);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL20.glBlendEquationSeparate(eqRgb, eqAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
            GL20.glUseProgram(program);
        }
    }

    private static Vec3 vector(EntropyFountainLayout.Point p) {
        return new Vec3(p.x(), p.y(), p.z());
    }

    private static void vertex(Vec3 base, Vec3 right, Vec3 along, double x, double y, float u, float v) {
        BUILDER.vertex(base.x + right.x * x + along.x * y, base.y + right.y * x + along.y * y, base.z + right.z * x + along.z * y).color(1F, 1F, 1F, 1F).uv(u, v).endVertex();
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            shader = null;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "entropy_dark_fountain"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> {
                shader = loaded;
                blobLocation = GL20.glGetUniformLocation(loaded.getId(), "Blobs[0]");
            });
        }
    }

    private EntropyFountainRenderer() {
    }
}
