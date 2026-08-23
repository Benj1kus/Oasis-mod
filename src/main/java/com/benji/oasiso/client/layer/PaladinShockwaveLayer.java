package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.PaladinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PaladinShockwaveLayer extends GeoRenderLayer<PaladinEntity> {

    private static final long DURATION_NS = 650_000_000L;
    private static final double MAX_RADIUS = 10.5D;
    private static final int SEGMENTS = 72;

    public PaladinShockwaveLayer(GeoRenderer<PaladinEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, PaladinEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        long start = entity.getClientShockwaveStartNanos();
        if (start <= 0L) return;

        long now = System.nanoTime();
        float progress = (now - start) / (float) DURATION_NS;
        if (progress < 0.0F || progress >= 1.0F) return;

        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
        float fade = 1.0F - progress;

        double radius = Mth.lerp(eased, 0.7D, MAX_RADIUS);
        double liftedHeight = Mth.lerp(eased, 0.12D, 1.30D);

        int outerAlpha = Mth.clamp(Math.round(fade * fade * 195.0F), 0, 255);
        int coreAlpha = Mth.clamp(Math.round(fade * 240.0F), 0, 255);
        int contactAlpha = Mth.clamp(Math.round(fade * fade * 165.0F), 0, 255);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        // high main wave
        drawLiftedRing(consumer, matrix, radius, 0.90D, liftedHeight, 35, 255, 220, outerAlpha);

        // cyan core
        drawLiftedRing(consumer, matrix, radius - 0.18D, 0.26D, liftedHeight + 0.06D, 190, 255, 250, coreAlpha);

        // ground contact band
        drawGroundRing(consumer, matrix, radius - 0.55D, radius + 0.70D, 0.02D, 0.16D, 255, 214, 90, contactAlpha);
        drawGroundRing(consumer, matrix, radius - 0.25D, radius + 0.22D, 0.01D, 0.08D, 120, 255, 245, Math.max(0, contactAlpha - 20));

        // contact tongues / ground licking arcs
        drawContactSpikes(consumer, matrix, radius, fade, progress);

        // inner follow ring for depth feeling
        if (radius > 2.0D) {
            drawLiftedRing(consumer, matrix, radius * 0.82D, 0.34D, liftedHeight * 0.74D, 35, 220, 255, outerAlpha / 3);
        }
    }

    private void drawLiftedRing(VertexConsumer consumer, Matrix4f matrix, double radius, double width, double baseY, int red, int green, int blue, int alpha) {
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

    private void drawGroundRing(VertexConsumer consumer, Matrix4f matrix, double innerRadius, double outerRadius, double y0, double y1, int red, int green, int blue, int alpha) {
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
    private void drawContactSpikes(VertexConsumer consumer, Matrix4f matrix, double radius, float fade, float progress) {
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

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, 255, 215, 100, spikeAlpha);
            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, 120, 255, 245, spikeAlpha / 2);
        }
    }

    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, int red, int green, int blue, int alpha) {
        addVertex(consumer, matrix, first, red, green, blue, alpha);
        addVertex(consumer, matrix, second, red, green, blue, alpha);
        addVertex(consumer, matrix, third, red, green, blue, alpha);
        addVertex(consumer, matrix, fourth, red, green, blue, alpha);

        addVertex(consumer, matrix, fourth, red, green, blue, alpha);
        addVertex(consumer, matrix, third, red, green, blue, alpha);
        addVertex(consumer, matrix, second, red, green, blue, alpha);
        addVertex(consumer, matrix, first, red, green, blue, alpha);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
}