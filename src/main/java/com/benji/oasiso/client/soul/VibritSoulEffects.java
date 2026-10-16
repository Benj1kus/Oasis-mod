package com.benji.oasiso.client.soul;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.VibritBlock;
import com.benji.oasiso.common.soul.VibritSoulNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VibritSoulEffects {
    public static final ResourceLocation HEAD = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/particle/soul_head.png");
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(65536));
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private static ClientLevel world;
    private static final int SEGMENTS = 28;
    private static final double TRAIL_TICKS = 12;
    private static final double CHIP_LIFE = 13;
    private static final int[][] CUBE_FACES = {{0, 4, 6, 2}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}};
    private static final int[] CUBE_SHADE = {170, 210, 145, 255, 180, 225};
    private static final float HEAD_HALF_SIZE = .29F;
    private static final float TRAIL_HALF_WIDTH = .23F;
    private static Matrix4f soulView;
    private static Matrix4f soulProjection;

    private static final class Flight {
        final VibritSoulNetwork.Flight packet;
        final long start;
        double cancelledAt = -1;

        Flight(VibritSoulNetwork.Flight packet, long start) {
            this.packet = packet;
            this.start = start;
        }
    }

    private VibritSoulEffects() {
    }

    private static void world(ClientLevel level) {
        if (world != level) {
            FLIGHTS.clear();
            world = level;
        }
    }

    public static void receive(VibritSoulNetwork.Flight packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) return;
        world(level);
        long start = Math.min(packet.startTick(), level.getGameTime());
        if (level.getGameTime() - start >= VibritSoulPath.LIFE_TICKS) return;
        if (FLIGHTS.size() >= 128) FLIGHTS.remove(0);
        FLIGHTS.add(new Flight(packet, start));
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == world) {
            FLIGHTS.clear();
            world = null;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            soulView = new Matrix4f(event.getPoseStack().last().pose());
            soulProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Matrix4f view = soulView;
        Matrix4f projection = soulProjection;
        soulView = null;
        soulProjection = null;

        if (view == null || projection == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        world(mc.level);
        double now = mc.level.getGameTime() + event.getPartialTick();
        FLIGHTS.removeIf(f -> now - f.start >= VibritSoulPath.LIFE_TICKS || now < f.start || (f.cancelledAt >= 0 && now - f.cancelledAt >= 6));
        var shader = VibritSoulShaders.shader;
        if (shader == null || FLIGHTS.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        var previous = RenderSystem.getShader();
        shader.safeGetUniform("SoulProjection").set(projection);
        try {
            for (Flight flight : FLIGHTS) {
                var packet = flight.packet;
                if (flight.cancelledAt < 0 && (!mc.level.hasChunkAt(packet.target()) || !(mc.level.getBlockState(packet.target()).getBlock() instanceof VibritBlock)))
                    flight.cancelledAt = now;
                float age = (float) (now - flight.start), progress = age / VibritSoulPath.LIFE_TICKS;
                float alpha = Math.min(1, age / 4F);
                if (flight.cancelledAt >= 0) alpha *= Math.max(0, 1 - (float) (now - flight.cancelledAt) / 6F);
                if (alpha <= 0) continue;
                VertexConsumer out = BUFFER.getBuffer(SoulType.TYPE);
                renderChips(out, packet, age, progress, alpha, view, camera);
                Vector3f[] points = new Vector3f[SEGMENTS + 1];
                float first = (float) Math.max(0, (age - TRAIL_TICKS) / VibritSoulPath.LIFE_TICKS);
                for (int i = 0; i <= SEGMENTS; i++) {
                    double t = first + (progress - first) * i / SEGMENTS;
                    points[i] = toView(VibritSoulPath.sample(packet.from(), packet.to(), packet.seed(), t), view, camera);
                }
                float shrink = 1F - .75F * smooth((progress - .87F) / .13F);
                Vector3f[] sides = new Vector3f[SEGMENTS + 1];
                Vector3f previousSide = new Vector3f(1, 0, 0);
                for (int i = 0; i <= SEGMENTS; i++) {
                    Vector3f tangent = new Vector3f(points[Math.min(SEGMENTS, i + 1)]).sub(points[Math.max(0, i - 1)]);
                    Vector3f side = new Vector3f(-tangent.y, tangent.x, 0);
                    if (side.lengthSquared() < .000001F) side.set(previousSide);
                    else side.normalize();
                    if (side.dot(previousSide) < 0) side.negate();
                    previousSide.set(side);
                    float tailScale = .55F + .45F * (float) Math.floor(i / (float) SEGMENTS * 5F) / 5F;
                    sides[i] = side.mul(TRAIL_HALF_WIDTH * tailScale * shrink);
                }
                for (int i = 0; i < SEGMENTS; i++) {
                    float u = i / (float) SEGMENTS, next = (i + 1) / (float) SEGMENTS;
                    vertex(out, new Vector3f(points[i]).sub(sides[i]), 0, u, progress, 0, packet.seed(), alpha);
                    vertex(out, new Vector3f(points[i]).add(sides[i]), 1, u, progress, 0, packet.seed(), alpha);
                    vertex(out, new Vector3f(points[i + 1]).add(sides[i + 1]), 1, next, progress, 0, packet.seed(), alpha);
                    vertex(out, new Vector3f(points[i + 1]).sub(sides[i + 1]), 0, next, progress, 0, packet.seed(), alpha);
                }
                Vector3f p = points[SEGMENTS];
                Vector3f direction = new Vector3f(p).sub(points[SEGMENTS - 1]);
                direction.z = 0;
                if (direction.lengthSquared() < .000001F) direction.set(0, 1, 0);
                else direction.normalize();
                Vector3f side = new Vector3f(direction.y, -direction.x, 0).mul(HEAD_HALF_SIZE * shrink);
                direction.mul(HEAD_HALF_SIZE * shrink);
                vertex(out, new Vector3f(p).sub(side).add(direction), 0, 0, progress, 255, packet.seed(), alpha);
                vertex(out, new Vector3f(p).sub(side).sub(direction), 0, 1, progress, 255, packet.seed(), alpha);
                vertex(out, new Vector3f(p).add(side).sub(direction), 1, 1, progress, 255, packet.seed(), alpha);
                vertex(out, new Vector3f(p).add(side).add(direction), 1, 0, progress, 255, packet.seed(), alpha);
            }
            BUFFER.endBatch(SoulType.TYPE);
        } finally {
            BUFFER.endBatch(SoulType.TYPE);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void renderChips(VertexConsumer out, VibritSoulNetwork.Flight packet, float age, float progress, float alpha, Matrix4f view, Vec3 camera) {
        int last = (int) Math.floor(age / 2);
        int first = Math.max(3, (int) Math.ceil((age - CHIP_LIFE) / 2));
        for (int emission = first; emission <= last; emission++) {
            double birth = emission * 2.0, elapsed = age - birth;
            double pathTick = birth - 6;
            Vec3 origin = VibritSoulPath.sample(packet.from(), packet.to(), packet.seed(), pathTick / VibritSoulPath.LIFE_TICKS);
            Vec3 ahead = VibritSoulPath.sample(packet.from(), packet.to(), packet.seed(), (pathTick + .5) / VibritSoulPath.LIFE_TICKS);
            Vec3 behind = VibritSoulPath.sample(packet.from(), packet.to(), packet.seed(), (pathTick - .5) / VibritSoulPath.LIFE_TICKS);
            Vec3 drift = ahead.subtract(behind).scale(.22);
            for (int piece = 0; piece < 2; piece++) {
                int key = packet.seed() ^ emission * 73428767 ^ piece * 912931;
                double rx = random(key), ry = random(key + 1), rz = random(key + 2);
                double vx = (rx - .5) * .023, vy = .009 + ry * .014, vz = (rz - .5) * .023;
                Vec3 centre = origin.add((rx - .5) * .24, (ry - .5) * .18, (rz - .5) * .24).add(drift.scale(elapsed)).add(vx * elapsed, vy * elapsed, vz * elapsed);
                float fade = (1F - smooth((float) (elapsed / CHIP_LIFE))) * smooth((float) elapsed / 1.5F);
                float size = (.035F + .045F * random(key + 3)) * (1F - .45F * smooth((float) (elapsed / CHIP_LIFE)));
                float yaw = (float) (rx * Math.PI * 2 + elapsed * .035 * (rz > .5 ? 1 : -1));
                float pitch = (float) (ry * .8 + elapsed * .022);
                cube(out, centre, size, yaw, pitch, progress, alpha * fade, view, camera);
            }
        }
    }

    private static float random(int key) {
        key ^= key >>> 16;
        key *= 0x7feb352d;
        key ^= key >>> 15;
        key *= 0x846ca68b;
        key ^= key >>> 16;
        return (key & 0x00ffffff) / 16777216F;
    }

    private static void cube(VertexConsumer out, Vec3 centre, float size, float yaw, float pitch, float progress, float alpha, Matrix4f view, Vec3 camera) {
        if (alpha < .01F) return;
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector3f offset = new Vector3f((i & 1) == 0 ? -size : size, (i & 2) == 0 ? -size : size, (i & 4) == 0 ? -size : size).rotateX(pitch).rotateY(yaw);
            corners[i] = toView(centre.add(offset.x, offset.y, offset.z), view, camera);
        }
        for (int face = 0; face < CUBE_FACES.length; face++) {
            int[] f = CUBE_FACES[face];
            Vector3f normal = new Vector3f(corners[f[1]]).sub(corners[f[0]]).cross(new Vector3f(corners[f[2]]).sub(corners[f[0]]));
            if (normal.dot(corners[f[0]]) >= 0) continue;
            int shade = CUBE_SHADE[face];
            vertex(out, corners[f[0]], 0, 0, progress, 128, shade, alpha);
            vertex(out, corners[f[1]], 1, 0, progress, 128, shade, alpha);
            vertex(out, corners[f[2]], 1, 1, progress, 128, shade, alpha);
            vertex(out, corners[f[3]], 0, 1, progress, 128, shade, alpha);
        }
    }

    private static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static Vector3f toView(Vec3 p, Matrix4f view, Vec3 camera) {
        return view.transformPosition(new Vector3f((float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)));
    }

    private static void vertex(VertexConsumer out, Vector3f p, float u, float v, float age, int kind, int seed, float alpha) {
        out.vertex(p.x, p.y, p.z).color(Math.min(255, Math.max(0, (int) (age * 255))), kind, seed & 255, Math.min(255, Math.max(0, (int) (alpha * 255)))).uv(u, v).endVertex();
    }

    private static final class SoulType extends RenderType {
        static final RenderType TYPE = create("oasiso_vibrit_soul", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 65536, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> VibritSoulShaders.shader)).setTextureState(new TextureStateShard(HEAD, false, false)).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private SoulType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }
}
