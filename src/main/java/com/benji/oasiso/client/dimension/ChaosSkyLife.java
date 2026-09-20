package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ChaosSkyLife {
    // celetsial
    private static final int STAR_COUNT = 760;
    private static final float STAR_BRIGHTNESS = 0.85F;
    private static final int MAX_COMETS = 2;
    private static final int COMET_DELAY_MIN = 100;
    private static final int COMET_DELAY_MAX = 180;
    private static final float DISTANCE = 90.0F;
    private static final double TAU = Math.PI * 2;

    private static final Random RANDOM = new Random();
    private static final List<Star> STARS = createStars();
    private static final List<Comet> COMETS = new ArrayList<>();
    private static ClientLevel currentLevel;
    private static long visualTicks;
    private static int cometDelay;

    private ChaosSkyLife() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(Oasiso.CHAOS_DIMENSION)) {
            currentLevel = null;
            visualTicks = 0;
            COMETS.clear();
            return;
        }
        if (currentLevel != mc.level) {
            currentLevel = mc.level;
            visualTicks = 0;
            COMETS.clear();
            cometDelay = 40 + RANDOM.nextInt(41);
        }
        if (mc.isPaused()) return;
        visualTicks++;
        double now = visualTicks / 20.0;
        COMETS.removeIf(c -> now - c.born > c.lifetime);
        if (--cometDelay <= 0) {
            if (COMETS.size() < MAX_COMETS) COMETS.add(newComet(now));
            cometDelay = between(COMET_DELAY_MIN, COMET_DELAY_MAX);
        }
    }

    public static float seconds(float partialTick) {
        return (float) ((visualTicks + Math.max(0, Math.min(1, partialTick))) / 20.0);
    }

    public static void render(Matrix4f matrix, float seconds) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferBuilder b = Tesselator.getInstance().getBuilder();

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Star s : STARS) drawStar(b, matrix, s, seconds);
        for (Comet c : COMETS) drawComet(b, matrix, c, seconds);
        BufferUploader.drawWithShader(b.end());

        RenderSystem.defaultBlendFunc();
    }

    private static List<Star> createStars() {
        Random r = new Random(0x0A5150C05L);
        List<Star> result = new ArrayList<>();
        Vec3[] clusters = new Vec3[7];
        for (int i = 0; i < clusters.length; i++) {
            clusters[i] = direction(i * TAU / clusters.length + 0.25, 0.22 + r.nextDouble() * 0.75);
        }
        int[] colors = {0x60C7D9, 0x51CDA5, 0x80FBE1, 0x74A5E0, 0xBDEFE8, 0xEAC77B};
        for (int i = 0; i < STAR_COUNT; i++) {
            Vec3 n;
            if (i % 4 != 0) {
                Vec3 center = clusters[i % clusters.length];
                Frame f = frame(center);
                n = center.add(f.u.scale(r.nextGaussian() * 0.23))
                        .add(f.v.scale(r.nextGaussian() * 0.075)).normalize();
            } else {
                n = direction(r.nextDouble() * TAU, -0.12 + r.nextDouble() * 1.1);
            }
            Frame f = frame(n);
            boolean bright = i % 29 == 0;
            int color = (i % 47 == 0) ? colors[5] : colors[r.nextInt(5)];
            double size = bright ? 0.14 + r.nextDouble() * 0.07 : 0.05 + r.nextDouble() * 0.09;
            result.add(new Star(n.scale(DISTANCE), f, size,
                    color, r.nextDouble() * TAU, 0.35 + r.nextDouble() * 0.8, bright, i % 5 == 0));
        }
        return List.copyOf(result);
    }

    private static void drawStar(BufferBuilder b, Matrix4f m, Star s, float t) {
        double wave = 0.5 + 0.5 * Math.sin(t * s.speed + s.phase);
        float alpha = STAR_BRIGHTNESS * (float) (s.twinkle ? 0.26 + 0.74 * Math.pow(wave, 5) : 0.60 + 0.22 * wave);
        double size = s.size;
        quad(b, m, s.center, s.frame, 0, 0, size, size, s.color, alpha);
        if (s.bright) {
            quad(b, m, s.center, s.frame, 0, 0, size * 3.7, size * 3.7, s.color, alpha * 0.045F);
            quad(b, m, s.center, s.frame, 0, 0, size * 0.45, size * 4.0, s.color, alpha * 0.55F);
            quad(b, m, s.center, s.frame, 0, 0, size * 3.0, size * 0.45, s.color, alpha * 0.55F);
            quad(b, m, s.center, s.frame, 0, 0, size * 0.55, size * 0.55, 0xD5FFF1, alpha);
        }
    }

    private static Comet newComet(double now) {
        Vec3 n = direction(RANDOM.nextDouble() * TAU, 0.35 + RANDOM.nextDouble() * 0.85);
        Frame f = frame(n);
        Vec3 down = new Vec3(0, -1, 0).subtract(n.scale(-n.y)).normalize();
        Vec3 travel = down.add(f.u.scale((RANDOM.nextDouble() - 0.5) * 1.7)).normalize();
        return new Comet(n, travel, now, 1.8 + RANDOM.nextDouble() * 1.6,
                0.15 + RANDOM.nextDouble() * 0.10, RANDOM.nextInt(7) == 0 ? 0xFFD584 : 0x79FFE3);
    }

    private static void drawComet(BufferBuilder b, Matrix4f m, Comet c, float now) {
        double age = now - c.born;
        if (age < 0 || age >= c.lifetime) return;
        float alpha = (float) (smooth(age / 0.25) * (1 - smooth((age / c.lifetime - 0.67) / 0.33)));
        Vec3 head = c.origin.add(c.travel.scale(age * c.speed)).normalize().scale(DISTANCE);
        Vec3 tail = c.origin.add(c.travel.scale(Math.max(0, age - 0.65) * c.speed)).normalize().scale(DISTANCE);
        Vec3 across = head.normalize().cross(c.travel).normalize();
        ribbon(b, m, head, tail, across, 0.27, c.color, alpha * 0.11F);
        ribbon(b, m, head, tail, across, 0.09, c.color, alpha * 0.72F);
        Frame f = frame(head.normalize());
        quad(b, m, head, f, 0, 0, 0.10, 0.10, 0xDAFFF3, alpha);
        quad(b, m, head, f, 0, 0, 0.035, 0.30, c.color, alpha * 0.7F);
    }

    private static void ribbon(BufferBuilder b, Matrix4f m, Vec3 head, Vec3 tail, Vec3 side,
                               double width, int color, float alpha) {
        point(b, m, tail.x, tail.y, tail.z, color, 0);
        point(b, m, head.x - side.x * width, head.y - side.y * width, head.z - side.z * width, color, alpha);
        point(b, m, head.x + side.x * width, head.y + side.y * width, head.z + side.z * width, color, alpha);
        point(b, m, tail.x, tail.y, tail.z, color, 0);
    }

    private static void quad(BufferBuilder b, Matrix4f m, Vec3 c, Frame f, double x, double y,
                             double rx, double ry, int color, float alpha) {
        local(b, m, c, f, x - rx, y - ry, color, alpha);
        local(b, m, c, f, x + rx, y - ry, color, alpha);
        local(b, m, c, f, x + rx, y + ry, color, alpha);
        local(b, m, c, f, x - rx, y + ry, color, alpha);
    }

    private static void local(BufferBuilder b, Matrix4f m, Vec3 c, Frame f, double x, double y,
                              int color, float alpha) {
        point(b, m, c.x + f.u.x * x + f.v.x * y, c.y + f.u.y * x + f.v.y * y, c.z + f.u.z * x + f.v.z * y, color, alpha);
    }

    private static void point(BufferBuilder b, Matrix4f m, double x, double y, double z, int color, float alpha) {
        b.vertex(m, (float) x, (float) y, (float) z)
                .color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, alpha).endVertex();
    }

    private static Frame frame(Vec3 normal) {
        Vec3 reference = Math.abs(normal.y) > 0.97 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        Vec3 u = reference.cross(normal).normalize();
        return new Frame(u, normal.cross(u).normalize());
    }

    private static Vec3 direction(double azimuth, double elevation) {
        return new Vec3(Math.cos(azimuth) * Math.cos(elevation), Math.sin(elevation), Math.sin(azimuth) * Math.cos(elevation));
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static int between(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }

    private record Frame(Vec3 u, Vec3 v) {
    }

    private record Star(Vec3 center, Frame frame, double size, int color, double phase, double speed,
                        boolean bright, boolean twinkle) {
    }

    private record Comet(Vec3 origin, Vec3 travel, double born, double lifetime, double speed, int color) {
    }
}
