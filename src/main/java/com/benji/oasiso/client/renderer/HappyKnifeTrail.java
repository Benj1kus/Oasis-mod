package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.HappyKnifeEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class HappyKnifeTrail {
    private static final double LIFE = 7;
    private static final int MAX_ALPHA = 120;
    private static final int HEAD_COLOR = 0xFFD53D, TAIL_COLOR = 0x78341F;
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private record Point(Vec3 a, Vec3 b, double time) {
    }

    private static final Map<HappyKnifeEntity, ArrayDeque<Point>> TRAILS = new WeakHashMap<>();
    private static ClientLevel world;

    private static void world() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != world) {
            world = current;
            TRAILS.clear();
        }
    }

    public static void sample(HappyKnifeEntity e, Vec3 a, Vec3 b, float partial) {
        world();
        if (world == null) return;
        if (e.position().distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > 48 * 48)
            return;
        double now = world.getGameTime() + partial;
        ArrayDeque<Point> points = TRAILS.computeIfAbsent(e, key -> new ArrayDeque<>());
        Point last = points.peekLast();
        if (last != null && (now < last.time || a.distanceToSqr(last.a) > 16)) points.clear();
        else if (last != null && now - last.time < .20) return;
        points.addLast(new Point(a, b, now));
        while (points.size() > 48 || !points.isEmpty() && now - points.peekFirst().time > LIFE) points.removeFirst();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        world();
        if (world == null) return;
        double now = world.getGameTime() + event.getPartialTick();
        TRAILS.entrySet().removeIf(e -> e.getValue().isEmpty() || now - e.getValue().peekLast().time > LIFE);
        if (TRAILS.isEmpty()) return;
        Vec3 eye = event.getCamera().getPosition();
        var shader = RenderSystem.getShader();
        float[] color = RenderSystem.getShaderColor().clone();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int sr = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dr = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB), sa = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), da = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder out = Tesselator.getInstance().getBuilder();
            out.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = new Matrix4f();
            var entries = new ArrayList<>(TRAILS.entrySet());
            entries.sort(Comparator.comparingDouble((Map.Entry<HappyKnifeEntity, ArrayDeque<Point>> e) -> e.getKey().distanceToSqr(eye)).reversed());
            int drawn = 0;
            for (var entry : entries) {
                if (entry.getKey().distanceToSqr(eye) > 48 * 48 || drawn++ >= 48) continue;
                Point previous = null;
                int segment = 0;
                for (Point point : entry.getValue()) {
                    if (previous != null) {
                        for (int row = 0; row < 3; row++)
                            for (int col = 0; col < 8; col++) {
                                double t0 = row / 3.0, t1 = (row + 1) / 3.0, u0 = col / 8.0, u1 = (col + 1) / 8.0;
                                double age = now - (previous.time + (point.time - previous.time) * (t0 + t1) * .5);
                                double fade = Math.max(0, Math.min(1, 1 - age / LIFE));
                                if (fade <= (BAYER[(col & 3) + 4 * ((segment * 3 + row) & 3)] + .5) / 16) continue;
                                Vec3 a0 = previous.a.lerp(point.a, t0), b0 = previous.b.lerp(point.b, t0), a1 = previous.a.lerp(point.a, t1), b1 = previous.b.lerp(point.b, t1);
                                double f0 = Math.max(0, Math.min(1, 1 - (now - (previous.time + (point.time - previous.time) * t0)) / LIFE));
                                double f1 = Math.max(0, Math.min(1, 1 - (now - (previous.time + (point.time - previous.time) * t1)) / LIFE));
                                vertex(out, matrix, a0.lerp(b0, u0).subtract(eye), f0);
                                vertex(out, matrix, a1.lerp(b1, u0).subtract(eye), f1);
                                vertex(out, matrix, a1.lerp(b1, u1).subtract(eye), f1);
                                vertex(out, matrix, a0.lerp(b0, u1).subtract(eye), f0);
                            }
                    }
                    previous = point;
                    segment++;
                }
            }
            BufferUploader.drawWithShader(out.end());
        } finally {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.depthMask(write);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sr, dr, sa, da);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
        }
    }

    private static int channel(int shift, double tint) {
        return (int) (((TAIL_COLOR >> shift) & 255) * (1 - tint) + ((HEAD_COLOR >> shift) & 255) * tint);
    }

    private static void vertex(BufferBuilder out, Matrix4f matrix, Vec3 p, double fade) {
        double tint = Math.pow(fade, 1.5);
        int r = channel(16, tint), g = channel(8, tint), b = channel(0, tint);
        int alpha = (int) (MAX_ALPHA * fade);
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, alpha).endVertex();
    }
}
