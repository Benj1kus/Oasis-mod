package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.ApolWingsArmorItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApolWingsLightningTrail {
    public static final double LIFE = 18, SAMPLE_STEP = .5, RANGE = 64;
    public static final double WIDTH = .23; // Полуширина одного разряда у основания.
    private static final int MAX_PLAYERS = 12, MAX_POINTS = 40;
    private static final Map<Player, History[]> TRAILS = new WeakHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(65536));
    private static ClientLevel world;
    private static Matrix4f inverseView, view, projection;
    private static Vec3 camera;
    private static boolean sampling;

    private record Point(Vec3 pos, double time) {
    }

    private static final class History {
        final ArrayDeque<Point> points = new ArrayDeque<>();
        Vec3 last;
        double time, next;
    }

    private static void checkWorld() {
        var current = Minecraft.getInstance().level;
        if (current != world) {
            world = current;
            TRAILS.clear();
            inverseView = view = projection = null;
            sampling = false;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        if (world == null) return;
        double now = world.getGameTime();
        TRAILS.entrySet().removeIf(e -> e.getKey().isRemoved() || e.getKey().level() != world || Arrays.stream(e.getValue()).allMatch(h -> h.last == null || now - h.time > LIFE + 2));
    }

    public static void sample(Player player, int wing, Matrix4f bonePose, float partial) {
        if (!sampling || world == null || inverseView == null || player.level() != world || !player.isFallFlying() || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ApolWingsArmorItem))
            return;

        Vector4f point = bonePose.transform(new Vector4f(0, 0, 0, 1));
        inverseView.transform(point);
        Vec3 tip = new Vec3(point.x + camera.x, point.y + camera.y, point.z + camera.z);
        if (!Double.isFinite(tip.lengthSqr()) || tip.distanceToSqr(camera) > RANGE * RANGE) return;
        double now = world.getGameTime() + partial;
        History h = TRAILS.computeIfAbsent(player, p -> new History[]{new History(), new History()})[wing];
        if (h.last == null || now < h.time || now - h.time > 3 || tip.distanceToSqr(h.last) > 64) {
            h.points.clear();
            h.points.add(new Point(tip, now));
            h.next = now + SAMPLE_STEP;
        } else if (now > h.time) {
            for (; h.next <= now; h.next += SAMPLE_STEP) {
                double f = Mth.clamp((h.next - h.time) / (now - h.time), 0, 1);
                h.points.addLast(new Point(h.last.lerp(tip, f), h.next));
            }
        }
        h.last = tip;
        h.time = now;
        while (!h.points.isEmpty() && (now - h.points.peekFirst().time > LIFE || h.points.size() > MAX_POINTS))
            h.points.removeFirst();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            checkWorld();
            camera = event.getCamera().getPosition();
            inverseView = new Matrix4f(event.getPoseStack().last().pose()).invert();
            sampling = true;
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            sampling = false;
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            view = new Matrix4f(event.getPoseStack().last().pose());
            projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        sampling = false;
        Matrix4f matrix = view, proj = projection;
        view = projection = null;
        var shader = ApolWingsLightningShaders.shader;
        Minecraft mc = Minecraft.getInstance();
        if (shader == null || matrix == null || proj == null || world == null || world != mc.level) return;
        Vec3 eye = event.getCamera().getPosition();
        double now = world.getGameTime() + (mc.isPaused() ? mc.getFrameTime() : event.getPartialTick());
        var active = new ArrayList<>(TRAILS.entrySet());
        active.removeIf(e -> e.getKey().isRemoved() || !e.getKey().isAlive() || e.getKey().isInvisible() || e.getKey().position().distanceToSqr(eye) > RANGE * RANGE);
        active.sort(Comparator.comparingDouble(e -> e.getKey().position().distanceToSqr(eye)));
        var previous = RenderSystem.getShader();
        shader.safeGetUniform("TrailProjection").set(proj);
        try {
            VertexConsumer out = BUFFER.getBuffer(TrailType.TYPE);
            for (int n = 0; n < Math.min(MAX_PLAYERS, active.size()); n++) {
                var e = active.get(n);
                for (int wing = 0; wing < 2; wing++)
                    draw(out, matrix, eye, e.getValue()[wing], now, e.getKey().getId() * 31 + wing * 997);
            }
        } finally {
            BUFFER.endBatch(TrailType.TYPE);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void draw(VertexConsumer out, Matrix4f matrix, Vec3 eye, History h, double now, int seed) {
        if (h.last == null || now - h.time > LIFE) return;
        List<Point> points = new ArrayList<>();
        points.add(new Point(h.last, h.time));
        var iterator = h.points.descendingIterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            if (p.time < h.time - .01 && now - p.time <= LIFE) points.add(p);
        }
        if (points.size() < 2) return;
        double rangeFade = Mth.clamp((RANGE - h.last.distanceTo(eye)) / 8, 0, 1);
        for (int strand = 0; strand < 3; strand++) {
            Vec3 old = null;
            double oldAge = 0;
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                double age = Mth.clamp((now - p.time) / LIFE, 0, 1);
                Vec3 tangent = points.get(Math.min(i + 1, points.size() - 1)).pos.subtract(points.get(Math.max(0, i - 1)).pos).normalize();
                if (tangent.lengthSqr() < 1E-8) tangent = new Vec3(0, 0, 1);
                Vec3 side = tangent.cross(new Vec3(0, 1, 0)).normalize();
                if (side.lengthSqr() < 1E-8) side = new Vec3(1, 0, 0);
                Vec3 up = side.cross(tangent).normalize();
                double phase = (now - p.time) * .8 - now * .28 + strand * Math.PI * 2 / 3 + seed;
                double radius = .22 * Math.sin(Math.PI * age) * Math.min(1, age * 12);
                int frame = (int) Math.floor(now / 2.5);
                double jag = (hash(seed + i * 47 + frame * 337 + strand * 83) - .5) * .32 * Math.sin(Math.PI * age);
                Vec3 at = p.pos.add(side.scale(Math.cos(phase) * radius + jag)).add(up.scale(Math.sin(phase) * radius - jag * .5));
                if (old != null) ribbon(out, matrix, eye, old, at, oldAge, age, rangeFade, strand);
                old = at;
                oldAge = age;
            }
        }

        for (int i = 2; i < points.size(); i += 4) {
            Point p = points.get(i);
            double ticks = now - p.time, age = ticks / LIFE;
            if (age < .12 || age >= 1) continue;
            int key = seed + (int) Math.floor(p.time * 2) * 73;
            Vec3 drift = new Vec3(hash(key) - .5, hash(key + 5) * .7, hash(key + 11) - .5).scale(ticks * .045);
            Vec3 at = p.pos.add(drift);
            double size = .045 + .07 * hash(key + 19);
            Vec3 normal = eye.subtract(at).normalize();
            Vec3 side = normal.cross(new Vec3(0, 1, 0)).normalize();
            if (side.lengthSqr() < 1E-8) side = new Vec3(1, 0, 0);
            Vec3 up = side.cross(normal).normalize();
            double turn = ticks * .17 + key;
            Vec3 axis = side.scale(Math.cos(turn)).add(up.scale(Math.sin(turn)));
            ribbon(out, matrix, eye, at.subtract(axis.scale(size)), at.add(axis.scale(size)), age, age, rangeFade * .7, 2);
        }
    }

    private static void ribbon(VertexConsumer out, Matrix4f matrix, Vec3 eye, Vec3 a, Vec3 b, double ta, double tb, double fade, int strand) {
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1E-8) return;
        Vec3 side = direction.normalize().cross(eye.subtract(a.add(b).scale(.5)).normalize()).normalize();
        if (side.lengthSqr() < 1E-8) return;
        double wa = WIDTH * Math.pow(1 - ta, 1.2) * (strand == 0 ? 1 : .58), wb = WIDTH * Math.pow(1 - tb, 1.2) * (strand == 0 ? 1 : .58);
        vertex(out, matrix, eye, a.subtract(side.scale(wa)), ta, 0, fade);
        vertex(out, matrix, eye, b.subtract(side.scale(wb)), tb, 0, fade);
        vertex(out, matrix, eye, b.add(side.scale(wb)), tb, 1, fade);
        vertex(out, matrix, eye, a.add(side.scale(wa)), ta, 1, fade);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 eye, Vec3 p, double age, float v, double fade) {
        int color = age < .48 ? blend(0x62E3FF, 0xEF82D8, age / .48) : blend(0xEF82D8, 0x8952DF, (age - .48) / .52);
        int alpha = (int) (255 * Math.pow(1 - age, 1.35) * fade);
        out.vertex(matrix, (float) (p.x - eye.x), (float) (p.y - eye.y), (float) (p.z - eye.z)).color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).uv((float) age, v).endVertex();
    }

    private static int blend(int a, int b, double t) {
        int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255), g = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255), blue = (int) Mth.lerp(t, a & 255, b & 255);
        return r << 16 | g << 8 | blue;
    }

    private static double hash(int n) {
        n ^= n >>> 16;
        n *= 0x7feb352d;
        n ^= n >>> 15;
        n *= 0x846ca68b;
        n ^= n >>> 16;
        return (n & 0x7fffffff) / (double) Integer.MAX_VALUE;
    }

    private static final class TrailType extends RenderType {
        static final RenderType TYPE = create("oasiso_wings_lightning", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 65536, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> ApolWingsLightningShaders.shader)).setCullState(NO_CULL).setTransparencyState(NO_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private TrailType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }

    private ApolWingsLightningTrail() {
    }
}
