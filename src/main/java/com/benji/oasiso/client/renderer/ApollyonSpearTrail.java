package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class ApollyonSpearTrail {
    public static final double RANGE = 64;
    public static final float WIDTH = .28F;
    public static final double LIFE = 16;
    public static final double RISE = .125;
    private static final double STEP = .5;
    private static final int MAX_VISIBLE = 24;
    private static final Map<ApollyonEntity, History> TRAILS = new WeakHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(32768));
    private static ClientLevel world;
    private static Matrix4f worldView, worldProjection;
    private static ShaderInstance shader;

    private ApollyonSpearTrail() {
    }

    private static void checkWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (world != current) {
            world = current;
            TRAILS.clear();
            worldView = worldProjection = null;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        if (world == null) return;
        double now = world.getGameTime();
        TRAILS.entrySet().removeIf(e -> e.getKey().isRemoved() || e.getKey().level() != world || now - e.getValue().lastTime > LIFE + 2);
    }

    public static void clear(ApollyonEntity entity) {
        TRAILS.remove(entity);
    }

    public static void sample(ApollyonEntity entity, Vec3 tip, float partial) {
        checkWorld();
        if (entity.isTeleporting()) {
            clear(entity);
            return;
        }
        if (world == null || entity.level() != world || !Double.isFinite(tip.lengthSqr())) return;
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (tip.distanceToSqr(eye) > RANGE * RANGE) return;
        double now = world.getGameTime() + partial;
        History h = TRAILS.computeIfAbsent(entity, key -> new History());
        if (h.last == null || now < h.lastTime || now - h.lastTime > 5 || h.last.distanceToSqr(tip) > 64) {
            h.points.clear();
            h.points.add(new Point(tip, now));
            h.next = now + STEP;
        } else if (now > h.lastTime) {
            for (; h.next <= now; h.next += STEP) {
                double f = Mth.clamp((h.next - h.lastTime) / (now - h.lastTime), 0, 1);
                h.points.addLast(new Point(h.last.lerp(tip, f), h.next));
            }
        }
        h.last = tip;
        h.lastTime = now;
        while (!h.points.isEmpty() && now - h.points.peekFirst().time > LIFE) h.points.removeFirst();
        while (h.points.size() > 34) h.points.removeFirst();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            checkWorld();
            worldView = new Matrix4f(event.getPoseStack().last().pose());
            worldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = worldView, projection = worldProjection;
        worldView = worldProjection = null;
        Minecraft mc = Minecraft.getInstance();
        if (shader == null || world == null || world != mc.level || view == null || projection == null) return;
        Vec3 eye = event.getCamera().getPosition();
        float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
        double now = world.getGameTime() + partial;
        var active = new ArrayList<Map.Entry<ApollyonEntity, History>>();
        for (var entry : TRAILS.entrySet()) {
            var e = entry.getKey();
            var h = entry.getValue();
            if (!e.isRemoved() && e.isAlive() && !e.isInvisible() && !e.isTeleporting() && h.last != null && now - h.lastTime < 2 && h.last.distanceToSqr(eye) < RANGE * RANGE)
                active.add(entry);
        }
        if (active.isEmpty()) return;
        active.sort(Comparator.comparingDouble(e -> e.getValue().last.distanceToSqr(eye)));
        ShaderInstance previous = RenderSystem.getShader();
        shader.safeGetUniform("TrailProjection").set(projection);
        shader.safeGetUniform("Time").set((float) (now % 24000) / 20F);
        VertexConsumer out = BUFFER.getBuffer(TrailType.TRAIL);
        try {
            for (int i = 0; i < Math.min(MAX_VISIBLE, active.size()); i++) {
                var entry = active.get(i);
                draw(out, view, eye, entry.getValue(), now, entry.getKey().getId());
            }
        } finally {
            BUFFER.endBatch(TrailType.TRAIL);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void draw(VertexConsumer out, Matrix4f view, Vec3 eye, History h, double now, int id) {
        float fade = Mth.clamp((float) ((RANGE - h.last.distanceTo(eye)) / 8), 0, 1);
        int alpha = Math.round(255 * fade), seed = id & 255;
        List<Point> points = new ArrayList<>();
        points.add(new Point(h.last, now));
        var reverse = h.points.descendingIterator();
        while (reverse.hasNext()) {
            Point p = reverse.next();
            if (now - p.time > .015 && now - p.time <= LIFE) points.add(p);
        }
        for (int i = 1; i < points.size(); i++) {
            Point p = points.get(i - 1), q = points.get(i);
            double a = Mth.clamp(now - p.time, 0, LIFE), b = Mth.clamp(now - q.time, 0, LIFE);
            Vec3 from = flamePoint(p, a, id), to = flamePoint(q, b, id);
            ribbon(out, view, eye, from, to, WIDTH * (1 - (float) a / (float) LIFE * .80F), WIDTH * (1 - (float) b / (float) LIFE * .80F), (float) (a / LIFE), (float) (b / LIFE), 0, seed, alpha);
        }
        for (Point p : h.points) {
            long serial = (long) Math.floor(p.time / STEP);
            double age = now - p.time;
            if (Math.floorMod(serial, 4) != 0 || age < 5 || age > LIFE) continue;
            double t = (age - 5) / (LIFE - 5), phase = serial * 2.399963 + id;
            Vec3 center = p.pos.add(Math.cos(phase) * (.10 + t * .38), age * RISE + .09, Math.sin(phase) * (.10 + t * .38));
            double size = .10 * (1 - t * .65);
            ribbon(out, view, eye, center, center.add(Math.cos(phase) * size, size * 2, Math.sin(phase) * size), (float) size * 1.5F, (float) size * .7F, 0, 1, 255, seed, Math.round(alpha * (float) (1 - t)));
        }
    }

    private static Vec3 flamePoint(Point p, double age, int id) {
        double phase = p.time * .31 + id * .71, spread = age / LIFE;
        return p.pos.add(Math.sin(phase) * .13 * spread, age * RISE, Math.cos(phase * .83) * .13 * spread);
    }

    private static void ribbon(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 a, Vec3 b, float widthA, float widthB, float v0, float v1, int kind, int seed, int alpha) {
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1E-9) return;
        Vec3 side = direction.cross(a.lerp(b, .5).subtract(eye));
        if (side.lengthSqr() < 1E-9) side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1E-9) side = direction.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        Vec3 sa = side.scale(widthA * .5), sb = side.scale(widthB * .5);
        vertex(out, view, eye, a.subtract(sa), 0, v0, kind, seed, alpha);
        vertex(out, view, eye, a.add(sa), 1, v0, kind, seed, alpha);
        vertex(out, view, eye, b.add(sb), 1, v1, kind, seed, alpha);
        vertex(out, view, eye, b.subtract(sb), 0, v1, kind, seed, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 p, float u, float v, int kind, int seed, int alpha) {
        out.vertex(view, (float) (p.x - eye.x), (float) (p.y - eye.y), (float) (p.z - eye.z)).color(kind, seed, 255, alpha).uv(u, v).endVertex();
    }

    private record Point(Vec3 pos, double time) {
    }

    private static final class History {
        final ArrayDeque<Point> points = new ArrayDeque<>();
        Vec3 last;
        double lastTime, next;
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderRegistration {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            shader = null;
            TRAILS.clear();
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apollyon_spear_trail"), DefaultVertexFormat.POSITION_COLOR_TEX), value -> shader = value);
        }
    }

    private static final class TrailType extends RenderType {
        static final RenderType TRAIL = create("oasiso_apollyon_trail", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 32768, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private TrailType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }
}
