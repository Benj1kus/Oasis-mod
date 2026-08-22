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
    private static final int SEGMENTS = 64;


    public PaladinShockwaveLayer(GeoRenderer<PaladinEntity> renderer) {
        super(renderer);
    }


    @Override
    public void render(PoseStack poseStack, PaladinEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        long start = entity.getClientShockwaveStartNanos();


        if (start <= 0L) {
            return;
        }


        long now = System.nanoTime();


        float progress = (now - start) / (float) DURATION_NS;


        if (progress < 0.0F || progress >= 1.0F) {
            return;
        }

        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);


        double radius = Mth.lerp(eased, 0.7D, MAX_RADIUS);
        double height = Mth.lerp(eased, 0.15D, 1.35D);
        float fade = 1.0F - progress;


        int mainAlpha = Mth.clamp(Math.round(fade * fade * 190.0F), 0, 255);
        int coreAlpha = Mth.clamp(Math.round(fade * 245.0F), 0, 255);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        Matrix4f matrix = poseStack.last().pose();

        drawRing(consumer, matrix, radius, 0.85D, height, 35, 255, 220, mainAlpha);

        drawRing(consumer, matrix, radius - 0.20D, 0.24D, height + 0.06D, 190, 255, 250, coreAlpha);

        if (radius > 2.0D) {

            drawRing(consumer, matrix, radius * 0.82D, 0.32D, height * 0.72D, 35, 220, 255, mainAlpha / 3);
        }
    }


    private void drawRing(VertexConsumer consumer, Matrix4f matrix, double radius, double width, double baseY, int red, int green, int blue, int alpha) {
        if (radius <= 0.0D || alpha <= 0) {
            return;
        }

        double innerRadius = Math.max(0.05D, radius - width * 0.5D);
        double outerRadius = radius + width * 0.5D;

        double innerY = baseY - 0.07D;
        double outerY = baseY + 0.22D;


        for (int i = 0; i < SEGMENTS; i++) {

            double angle1 = Math.PI * 2.0D * i / SEGMENTS;
            double angle2 = Math.PI * 2.0D * (i + 1) / SEGMENTS;


            Vec3 first = new Vec3(Math.cos(angle1) * innerRadius, innerY, Math.sin(angle1) * innerRadius);
            Vec3 second = new Vec3(Math.cos(angle1) * outerRadius, outerY, Math.sin(angle1) * outerRadius);
            Vec3 third = new Vec3(Math.cos(angle2) * outerRadius, outerY, Math.sin(angle2) * outerRadius);
            Vec3 fourth = new Vec3(Math.cos(angle2) * innerRadius, innerY, Math.sin(angle2) * innerRadius);

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }


    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, int red, int green, int blue, int alpha) {

        addVertex(consumer, matrix, first, red, green, blue, alpha);

        addVertex(consumer, matrix, second, red, green, blue, alpha);

        addVertex(consumer, matrix, third, red, green, blue, alpha);

        addVertex(consumer, matrix, fourth, red, green, blue, alpha);


        // back
        addVertex(consumer, matrix, fourth, red, green, blue, alpha);

        addVertex(consumer, matrix, third, red, green, blue, alpha);

        addVertex(consumer, matrix, second, red, green, blue, alpha);

        addVertex(consumer, matrix, first, red, green, blue, alpha);
    }


    private void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
}