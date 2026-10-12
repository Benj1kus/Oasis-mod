package com.benji.oasiso.client.factory;

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

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class FactoryCoreStreams {
    public static final int MAX_FLIGHTS = 192;
    private static ShaderInstance shader;
    private static final BufferBuilder BUILDER = new BufferBuilder(262144);
    private static final Map<RouteKey, Route> ROUTES = new HashMap<>();
    private static final List<MovingFlame> FLAMES = new ArrayList<>();
    private static ClientLevel seenLevel, frameLevel;
    private static Matrix4f frameView, frameProjection;
    private static Vec3 frameCamera;
    private static double previousTime = Double.NaN, clockOrigin = Double.NaN;

    private record RouteKey(int seed, Vec3 destination) {
    }

    private static final class Route {
        double next;
        long serial;

        Route(double now, long seed) {
            next = now + .08 + FactoryCoreStreamPattern.hash(seed, 10) * .65;
        }
    }

    private record MovingFlame(RouteKey route, FactoryCoreStreamPattern.Flight flight) {
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
            seenLevel = mc.level;
            ROUTES.clear();
            FLAMES.clear();
            previousTime = Double.NaN;
            clockOrigin = Double.NaN;
        }
        if (shader == null || level == null || level != mc.level || view == null || projection == null || camera == null)
            return;
        float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
        double rawTime = (level.getGameTime() + (double) partial) / 20.0;
        if (Double.isNaN(clockOrigin) || rawTime < clockOrigin) clockOrigin = rawTime;
        double clock = rawTime - clockOrigin;
        if (!Double.isNaN(previousTime) && clock < previousTime) {
            ROUTES.clear();
            FLAMES.clear();
        }
        previousTime = clock;
        var links = FactoryCoreVisuals.links(camera);
        Set<RouteKey> live = new HashSet<>();
        for (var link : links) live.add(new RouteKey(link.seed(), link.end()));
        ROUTES.keySet().retainAll(live);
        FLAMES.removeIf(f -> clock - f.flight.birth >= f.flight.duration);
        if (!mc.isPaused()) for (var link : links) {
            var key = new RouteKey(link.seed(), link.end());
            Route route = ROUTES.computeIfAbsent(key, k -> new Route(clock, k.seed()));
            if (clock >= route.next) {
                long seed = ((long) key.seed() << 32) ^ (++route.serial * 0x9E3779B97F4A7C15L);
                route.next = clock + FactoryCoreStreamPattern.interval(seed);
                if (FLAMES.size() < MAX_FLIGHTS) {
                    var flight = new FactoryCoreStreamPattern.Flight(point(link.start()), point(link.end()), clock, seed);
                    FLAMES.add(new MovingFlame(key, flight));
                }
            }
        }
        List<FactoryCoreStreamPattern.Flight> visible = new ArrayList<>();
        for (var flame : FLAMES) {
            var f = flame.flight;
            if (!event.getFrustum().isVisible(new AABB(vector(f.start), vector(f.end)).inflate(3.2))) continue;
            visible.add(f);
        }
        if (visible.isEmpty()) return;
        visible.sort(Comparator.comparingDouble((FactoryCoreStreamPattern.Flight f) -> vector(f.at(f.progress(clock))).distanceToSqr(camera)).reversed());
        shader.safeGetUniform("FountainViewProjection").set(new Matrix4f(projection).mul(view));
        shader.safeGetUniform("Time").set((float) clock);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC), program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int eqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), eqAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        ShaderInstance previous = RenderSystem.getShader();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            org.joml.Vector3f cameraRight = new org.joml.Vector3f(1, 0, 0);
            new Matrix4f(view).invert().transformDirection(cameraRight);
            Vec3 fallback = new Vec3(cameraRight.x, cameraRight.y, cameraRight.z).normalize();
            BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            for (var f : visible) ribbon(f, clock, camera, fallback);
            BufferUploader.drawWithShader(BUILDER.end());
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

    private static FactoryCoreStreamPattern.Point point(Vec3 v) {
        return new FactoryCoreStreamPattern.Point(v.x, v.y, v.z);
    }

    private static Vec3 vector(FactoryCoreStreamPattern.Point p) {
        return new Vec3(p.x(), p.y(), p.z());
    }

    private static void ribbon(FactoryCoreStreamPattern.Flight f, double now, Vec3 camera, Vec3 fallback) {
        int count = FactoryCoreStreamPattern.TRAIL_SEGMENTS;
        double age = now - f.birth;
        double tail = Math.min(age, FactoryCoreStreamPattern.TRAIL_SECONDS);
        float opacity = (float) f.opacity(now);
        if (opacity < .002F || tail < .001) return;
        Vec3 oldLeft = null, oldRight = null, oldSide = null;
        for (int i = 0; i <= count; i++) {
            float v = i / (float) count;
            double progress = Math.max(0, Math.min(1, (age - tail * (1 - v)) / f.duration));
            Vec3 center = vector(f.at(progress));
            Vec3 tangent = vector(f.at(Math.min(1, progress + .002))).subtract(vector(f.at(Math.max(0, progress - .002)))).normalize();
            Vec3 side = tangent.cross(center.subtract(camera));
            if (side.lengthSqr() < 1e-8) {
                side = fallback.subtract(tangent.scale(fallback.dot(tangent)));
                if (side.lengthSqr() < 1e-8)
                    side = tangent.cross(Math.abs(tangent.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
            }
            side = side.normalize();
            if (oldSide != null && side.dot(oldSide) < 0) side = side.scale(-1);
            oldSide = side;
            Vec3 left = center.subtract(side.scale(f.halfWidth)).subtract(camera);
            Vec3 right = center.add(side.scale(f.halfWidth)).subtract(camera);
            if (i > 0) {
                float previousV = (i - 1) / (float) count;
                vertex(oldLeft, 0, previousV, f, age, opacity);
                vertex(oldRight, 1, previousV, f, age, opacity);
                vertex(right, 1, v, f, age, opacity);
                vertex(left, 0, v, f, age, opacity);
            }
            oldLeft = left;
            oldRight = right;
        }
    }

    private static void vertex(Vec3 p, float u, float v, FactoryCoreStreamPattern.Flight f, double age, float opacity) {
        BUILDER.vertex(p.x, p.y, p.z).color((float) (f.phase / (Math.PI * 2)), 1F, 1F, opacity).uv(u, v).endVertex();
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            shader = null;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "factory_core_stream"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> {
                shader = loaded;

            });
        }
    }

    private FactoryCoreStreams() {
    }
}
