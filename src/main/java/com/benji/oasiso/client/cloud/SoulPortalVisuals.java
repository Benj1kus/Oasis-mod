package com.benji.oasiso.client.cloud;

import com.benji.oasiso.client.soul.VibritSoulEffects;
import com.benji.oasiso.client.soul.VibritSoulShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

@Mod.EventBusSubscriber(modid = "oasiso", value = Dist.CLIENT)
public final class SoulPortalVisuals {
    public static final int AMBIENT_RADIUS = 32, MAX_SOULS = 20, MAX_DUST = 128;
    private static final BufferBuilder BUFFER = new BufferBuilder(131072);
    private static final List<Flight> SOULS = new ArrayList<>();
    private static final List<Dust> DUST = new ArrayList<>();
    private static ClientLevel world;
    private static long clock;

    private record Flight(Vec3 origin, long birth, int life, int seed, Vec3 normal, float reach) {
    }

    private record Dust(Vec3 origin, Vec3 velocity, long birth, int life, float size, int shade) {
    }

    private static void clear(ClientLevel now) {
        world = now;
        clock = 0;
        SOULS.clear();
        DUST.clear();
    }

    public static void emit(Level level, BlockPos block, Direction.Axis axis, RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level || mc.player == null) return;
        if (world != mc.level) clear(mc.level);
        if (block.distToCenterSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ()) > AMBIENT_RADIUS * AMBIENT_RADIUS)
            return;
        int sign = random.nextBoolean() ? 1 : -1;
        Vec3 normal = axis == Direction.Axis.X ? new Vec3(0, 0, sign) : new Vec3(sign, 0, 0);
        Vec3 p = new Vec3(block.getX() + .5, block.getY() + random.nextDouble(), block.getZ() + .5).add(axis == Direction.Axis.X ? (random.nextDouble() - .5) * .9 : sign * .08, 0, axis == Direction.Axis.Z ? (random.nextDouble() - .5) * .9 : sign * .08);
        if (DUST.size() < MAX_DUST && random.nextFloat() < .55F)
            DUST.add(new Dust(p, normal.scale(.012 + random.nextDouble() * .022).add(0, .004 + random.nextDouble() * .01, 0), clock, 40 + random.nextInt(50), .015F + random.nextFloat() * .035F, 90 + random.nextInt(166)));
        if (SOULS.size() < MAX_SOULS && random.nextFloat() < .035F)
            SOULS.add(new Flight(p, clock, 75 + random.nextInt(45), random.nextInt(), normal, 2.5F + random.nextFloat() * 3));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null) {
            clear(mc.level);
            return;
        }
        if (mc.isPaused()) return;
        clock++;
        SOULS.removeIf(f -> clock - f.birth >= f.life);
        DUST.removeIf(f -> clock - f.birth >= f.life);
    }

    private static Vec3 point(Flight f, double age, boolean ignored) {
        double t = Math.max(0, Math.min(1, age / f.life));
        double phase = (f.seed & 65535) * .017;
        Vec3 side = new Vec3(-f.normal.z, 0, f.normal.x);
        return f.origin.add(f.normal.scale(t * f.reach)).add(side.scale((Math.sin(t * 9 + phase) - Math.sin(phase)) * .34 * Math.min(1, t * 4))).add(0, (Math.cos(t * 8 + phase) - Math.cos(phase)) * .22 * Math.min(1, t * 4) + t * .35, 0);
    }

    private static float distanceFade(Vec3 origin, Vec3 player, int radius) {
        return (float) Math.max(0, Math.min(1, (radius - origin.distanceTo(player)) / 4));
    }

    private static Vector3f toView(Vec3 p, Matrix4f matrix, Vec3 camera) {
        return new Vector3f((float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)).mulPosition(matrix);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        var shader = VibritSoulShaders.shader;
        if (world == null || world != mc.level || mc.player == null || shader == null || (SOULS.isEmpty() && DUST.isEmpty()))
            return;
        Matrix4f matrix = e.getPoseStack().last().pose();
        Vec3 camera = e.getCamera().getPosition(), player = mc.player.position();
        double now = clock + (mc.isPaused() ? 0 : e.getPartialTick());
        try (CloudRenderState ignored = new CloudRenderState()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.colorMask(true, true, true, false);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, VibritSoulEffects.HEAD);
            shader.safeGetUniform("SoulProjection").set(e.getProjectionMatrix());
            BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            for (Dust d : DUST) {
                float age = (float) (now - d.birth), alpha = CloudMotion.opacity(age, d.life) * distanceFade(d.origin, player, AMBIENT_RADIUS) * .75F;
                Vector3f p = toView(d.origin.add(d.velocity.scale(age)), matrix, camera);
                float s = d.size;
                soulVertex(new Vector3f(p).add(-s, -s, 0), 0, 0, 0, 128, d.shade, alpha);
                soulVertex(new Vector3f(p).add(s, -s, 0), 1, 0, 0, 128, d.shade, alpha);
                soulVertex(new Vector3f(p).add(s, s, 0), 1, 1, 0, 128, d.shade, alpha);
                soulVertex(new Vector3f(p).add(-s, s, 0), 0, 1, 0, 128, d.shade, alpha);
            }
            for (Flight f : SOULS) drawSoul(f, now, player, matrix, camera);
            BufferUploader.drawWithShader(BUFFER.end());
        }
    }

    private static void vertex(Vector3f p, float u, float v, int color, float alpha) {
        BUFFER.vertex(p.x, p.y, p.z).color((color >> 16) & 255, (color >> 8) & 255, color & 255, Math.max(0, Math.min(255, (int) (alpha * 255)))).uv(u, v).endVertex();
    }

    private static void soulVertex(Vector3f p, float u, float v, float progress, int kind, int seed, float alpha) {
        int encoded = ((Math.max(0, Math.min(255, (int) (progress * 255)))) << 16) | (kind << 8) | (seed & 255);
        vertex(p, u, v, encoded, alpha);
    }

    private static void drawSoul(Flight f, double now, Vec3 player, Matrix4f matrix, Vec3 camera) {
        float age = (float) (now - f.birth), progress = age / f.life;
        if (age < 0 || progress >= 1) return;
        float alpha = CloudMotion.opacity(age, f.life) * distanceFade(f.origin, player, AMBIENT_RADIUS);
        if (alpha <= .005F) return;
        drawChips(f, age, progress, alpha, matrix, camera);
        final int segments = 32;

        Vector3f[] points = new Vector3f[segments + 1], sides = new Vector3f[segments + 1];

        double first = Math.max(0, age - 36);
        for (int i = 0; i <= segments; i++)
            points[i] = toView(point(f, first + (age - first) * i / segments, true), matrix, camera);
        Vector3f previous = new Vector3f(1, 0, 0);
        for (int i = 0; i <= segments; i++) {
            Vector3f tangent = new Vector3f(points[Math.min(segments, i + 1)]).sub(points[Math.max(0, i - 1)]);
            Vector3f side = new Vector3f(-tangent.y, tangent.x, 0);
            if (side.lengthSquared() < .000001F) side.set(previous);
            else side.normalize();
            if (side.dot(previous) < 0) side.negate();
            previous.set(side);
            sides[i] = side.mul(.23F * (.55F + .45F * i / segments));
        }
        for (int i = 0; i < segments; i++) {
            float a = i / (float) segments, b = (i + 1F) / segments;
            soulVertex(new Vector3f(points[i]).sub(sides[i]), 0, a, progress, 0, f.seed, alpha);
            soulVertex(new Vector3f(points[i]).add(sides[i]), 1, a, progress, 0, f.seed, alpha);
            soulVertex(new Vector3f(points[i + 1]).add(sides[i + 1]), 1, b, progress, 0, f.seed, alpha);
            soulVertex(new Vector3f(points[i + 1]).sub(sides[i + 1]), 0, b, progress, 0, f.seed, alpha);
        }
        Vector3f head = points[segments];
        Vector3f up = new Vector3f(head).sub(points[segments - 1]);
        up.z = 0;
        if (up.lengthSquared() < .000001F) up.set(0, 1, 0);
        else up.normalize();
        Vector3f side = new Vector3f(up.y, -up.x, 0).mul(.29F);
        up.mul(.29F);
        soulVertex(new Vector3f(head).sub(side).add(up), 0, 0, progress, 255, f.seed, alpha);
        soulVertex(new Vector3f(head).sub(side).sub(up), 0, 1, progress, 255, f.seed, alpha);
        soulVertex(new Vector3f(head).add(side).sub(up), 1, 1, progress, 255, f.seed, alpha);
        soulVertex(new Vector3f(head).add(side).add(up), 1, 0, progress, 255, f.seed, alpha);
    }

    private static void drawChips(Flight f, float age, float progress, float alpha, Matrix4f matrix, Vec3 camera) {
        int last = (int) Math.floor(age / 3);
        int first = Math.max(4, (int) Math.ceil((age - 14) / 3));
        for (int emission = first; emission <= last; emission++) {
            float elapsed = age - emission * 3;
            int key = f.seed ^ emission * 73428767;
            float rx = noise(key), ry = noise(key + 1), rz = noise(key + 2);
            Vec3 origin = point(f, Math.max(0, emission * 3 - 12), true);
            Vec3 centre = origin.add((rx - .5) * (.20 + elapsed * .02), .04 + elapsed * (.01 + ry * .015), (rz - .5) * (.20 + elapsed * .02));
            float fade = CloudMotion.smooth(elapsed / 2) * (1 - CloudMotion.smooth(elapsed / 14));
            cube(centre, .025F + .035F * ry, rx * 6.28F + elapsed * .03F, rz + elapsed * .02F, progress, alpha * fade, matrix, camera);
        }
    }

    private static float noise(int key) {
        key ^= key >>> 16;
        key *= 0x7feb352d;
        key ^= key >>> 15;
        key *= 0x846ca68b;
        key ^= key >>> 16;
        return (key & 0xFFFFFF) / 16777216F;
    }

    private static final int[][] CUBE_FACES = {{0, 4, 6, 2}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 6, 7, 3}, {0, 2, 3, 1}, {4, 5, 7, 6}};
    private static final int[] CUBE_SHADE = {170, 210, 145, 255, 180, 225};

    private static void cube(Vec3 centre, float size, float yaw, float pitch, float progress, float alpha, Matrix4f matrix, Vec3 camera) {
        if (alpha < .01F) return;
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector3f offset = new Vector3f((i & 1) == 0 ? -size : size, (i & 2) == 0 ? -size : size, (i & 4) == 0 ? -size : size).rotateX(pitch).rotateY(yaw);
            corners[i] = toView(centre.add(offset.x, offset.y, offset.z), matrix, camera);
        }
        for (int face = 0; face < 6; face++) {
            int[] q = CUBE_FACES[face];
            Vector3f normal = new Vector3f(corners[q[1]]).sub(corners[q[0]]).cross(new Vector3f(corners[q[2]]).sub(corners[q[0]]));
            if (normal.dot(corners[q[0]]) >= 0) continue;
            soulVertex(corners[q[0]], 0, 0, progress, 128, CUBE_SHADE[face], alpha);
            soulVertex(corners[q[1]], 1, 0, progress, 128, CUBE_SHADE[face], alpha);
            soulVertex(corners[q[2]], 1, 1, progress, 128, CUBE_SHADE[face], alpha);
            soulVertex(corners[q[3]], 0, 1, progress, 128, CUBE_SHADE[face], alpha);
        }
    }

}
