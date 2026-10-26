package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AngerEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.*;

@Mod.EventBusSubscriber(modid=Oasiso.MODID, value=Dist.CLIENT)
public final class AngerShockwave {
    private static final double LIFE = 13; // 0.65 seconds, like Paladin
    private static final int SEGMENTS = 72;
    private static final int[] BAYER = {0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5};
    private record Wave(Vec3 origin, long start) {}
    private static final Map<Integer, Wave> WAVES = new HashMap<>();
    private static ClientLevel world;
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        ClientLevel current = Minecraft.getInstance().level;
        if (current != world) { world = current; WAVES.clear(); }
        if (world == null) return;
        double now = world.getGameTime() + event.getPartialTick();
        for (var entity : world.entitiesForRendering()) if (entity instanceof AngerEntity anger) {
            long start = anger.getClientShockTime();
            if (start != Long.MIN_VALUE && now >= start && now - start < LIFE)
                WAVES.put(entity.getId(), new Wave(anger.getClientShockOrigin(), start));
        }
        WAVES.values().removeIf(w -> now < w.start || now - w.start >= LIFE);
        if (WAVES.isEmpty()) return;
        Vec3 eye = event.getCamera().getPosition();
        var shader = RenderSystem.getShader(); float[] color = RenderSystem.getShaderColor().clone();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int sr=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dr=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                sa=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), da=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.enableDepthTest(); RenderSystem.depthMask(false); RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc(); RenderSystem.disableCull(); RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder out = Tesselator.getInstance().getBuilder();
            out.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            // The same AFTER_WEATHER / camera-relative convention as the working fountain renderer.
            Matrix4f matrix = new Matrix4f();
            var waves = new ArrayList<>(WAVES.values());
            waves.sort(Comparator.comparingDouble((Wave w) -> w.origin.distanceToSqr(eye)).reversed());
            int count = 0;
            for (Wave wave : waves) {
                if (wave.origin.distanceToSqr(eye) > 64*64 || count++ >= 32) continue;
                draw(out, new Matrix4f(matrix).translate((float)(wave.origin.x-eye.x),
                        (float)(wave.origin.y-eye.y), (float)(wave.origin.z-eye.z)), (float)((now-wave.start)/LIFE));
            }
            BufferUploader.drawWithShader(out.end());
        } finally {
            RenderSystem.setShader(() -> shader); RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.depthMask(write); if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(sr,dr,sa,da); if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
        }
    }
    private static void draw(VertexConsumer consumer, Matrix4f matrix, float progress) {
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        float fade = 1.0F - progress;

        double radius = Mth.lerp(eased, 0.7D, (AngerEntity.SHOCK_RADIUS + .5));
        double liftedHeight = Mth.lerp(eased, 0.12D, 1.30D);

        int outerAlpha = Mth.clamp(Math.round(fade * fade * 195.0F), 0, 255);
        int coreAlpha = Mth.clamp(Math.round(fade * 240.0F), 0, 255);
        int contactAlpha = Mth.clamp(Math.round(fade * fade * 165.0F), 0, 255);


        // high main wave
        drawLiftedRing(consumer, matrix, radius, 0.90D, liftedHeight, 220, 18, 34, outerAlpha);

        // bright warm-red core
        drawLiftedRing(consumer, matrix, radius - 0.18D, 0.26D, liftedHeight + 0.06D, 255, 142, 110, coreAlpha);

        // ground contact band
        drawGroundRing(consumer, matrix, radius - 0.55D, radius + 0.70D, 0.02D, 0.16D, 240, 42, 20, contactAlpha);
        drawGroundRing(consumer, matrix, radius - 0.25D, radius + 0.22D, 0.01D, 0.08D, 255, 68, 46, Math.max(0, contactAlpha - 20));

        // contact tongues / ground licking arcs
        drawContactSpikes(consumer, matrix, radius, fade, progress);

        // inner follow ring for depth feeling
        if (radius > 2.0D) {
            drawLiftedRing(consumer, matrix, radius * 0.82D, 0.34D, liftedHeight * 0.74D, 165, 10, 28, outerAlpha / 3);
        }
    }

    private static void drawLiftedRing(VertexConsumer consumer, Matrix4f matrix, double radius, double width, double baseY, int red, int green, int blue, int alpha) {
        if (radius <= 0.0D || alpha <= 0) return;

        double innerRadius = Math.max(0.05D, radius - width * 0.5D);
        double outerRadius = radius + width * 0.5D;

        double innerY = baseY - 0.08D;
        double outerY = baseY + 0.24D;

        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = Math.PI * 2.0D * i / SEGMENTS;
            double a2 = Math.PI * 2.0D * (i + 1) / SEGMENTS;

            Vec3 first = new Vec3(Math.cos(a1) * innerRadius, innerY, Math.sin(a1) * innerRadius);
            Vec3 second = new Vec3(Math.cos(a1) * outerRadius, outerY, Math.sin(a1) * outerRadius);
            Vec3 third = new Vec3(Math.cos(a2) * outerRadius, outerY, Math.sin(a2) * outerRadius);
            Vec3 fourth = new Vec3(Math.cos(a2) * innerRadius, innerY, Math.sin(a2) * innerRadius);

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }

    private static void drawGroundRing(VertexConsumer consumer, Matrix4f matrix, double innerRadius, double outerRadius, double y0, double y1, int red, int green, int blue, int alpha) {
        if (outerRadius <= 0.0D || alpha <= 0) return;

        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = Math.PI * 2.0D * i / SEGMENTS;
            double a2 = Math.PI * 2.0D * (i + 1) / SEGMENTS;

            Vec3 first = new Vec3(Math.cos(a1) * innerRadius, y0, Math.sin(a1) * innerRadius);
            Vec3 second = new Vec3(Math.cos(a1) * outerRadius, y1, Math.sin(a1) * outerRadius);
            Vec3 third = new Vec3(Math.cos(a2) * outerRadius, y1, Math.sin(a2) * outerRadius);
            Vec3 fourth = new Vec3(Math.cos(a2) * innerRadius, y0, Math.sin(a2) * innerRadius);

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }

    // small ground licks to imitate contact with terrain / structures
    private static void drawContactSpikes(VertexConsumer consumer, Matrix4f matrix, double radius, float fade, float progress) {
        int spikeAlpha = Mth.clamp(Math.round(fade * fade * 135.0F), 0, 255);
        if (spikeAlpha <= 0) return;

        int spikeCount = 24;

        for (int i = 0; i < spikeCount; i++) {
            double angle = Math.PI * 2.0D * i / spikeCount;
            double wobble = 0.85D + 0.15D * Math.sin(progress * 16.0D + i * 1.7D);

            double inner = radius - 0.10D;
            double outer = radius + 0.65D * wobble;
            double height = 0.12D + 0.22D * fade + 0.06D * Math.sin(progress * 18.0D + i);

            Vec3 first = new Vec3(Math.cos(angle) * inner, 0.02D, Math.sin(angle) * inner);
            Vec3 second = new Vec3(Math.cos(angle) * outer, height, Math.sin(angle) * outer);

            double sideAngle = angle + Math.PI / spikeCount;
            Vec3 third = new Vec3(Math.cos(sideAngle) * outer, height * 0.8D, Math.sin(sideAngle) * outer);
            Vec3 fourth = new Vec3(Math.cos(sideAngle) * inner, 0.02D, Math.sin(sideAngle) * inner);

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, 230, 28, 20, spikeAlpha);
            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, 255, 80, 46, spikeAlpha / 2);
        }
    }

    private static void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, int red, int green, int blue, int alpha) {
        // Subdivide each band into small cells; ordered coverage dissolves its edges.
        int seed = (int)Math.floor((Math.atan2(first.z, first.x) + Math.PI) * SEGMENTS / (2*Math.PI));
        for (int row=0; row<2; row++) for (int col=0; col<6; col++) {
            double u0=col/6.0, u1=(col+1)/6.0, v0=row/2.0, v1=(row+1)/2.0;
            double coverage = Math.min(1, alpha / 150.0) * (col == 0 || col == 5 ? .65 : 1);
            if (coverage <= (BAYER[(col&3)+4*((seed*2+row)&3)]+.5)/16.0) continue;
            Vec3 a = first.lerp(second,u0).lerp(fourth.lerp(third,u0),v0);
            Vec3 b = first.lerp(second,u1).lerp(fourth.lerp(third,u1),v0);
            Vec3 c = first.lerp(second,u1).lerp(fourth.lerp(third,u1),v1);
            Vec3 d = first.lerp(second,u0).lerp(fourth.lerp(third,u0),v1);
            addVertex(consumer,matrix,a,red,green,blue,alpha);
            addVertex(consumer,matrix,b,red,green,blue,alpha);
            addVertex(consumer,matrix,c,red,green,blue,alpha);
            addVertex(consumer,matrix,d,red,green,blue,alpha);
        }
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
}