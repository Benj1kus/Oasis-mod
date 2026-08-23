package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.BossPortalEntity;
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

import java.util.UUID;

public class BossPortalChaosLeakLayer extends GeoRenderLayer<BossPortalEntity> {

    private static final double PORTAL_Y = 0.14D;

    private static final int RING_SEGMENTS = 48;

    private static final int BEAM_COUNT = 7;

    private static final int BEAM_SEGMENTS = 10;

    private static final int BEAM_SIDES = 6;


    public BossPortalChaosLeakLayer(GeoRenderer<BossPortalEntity> renderer) {
        super(renderer);
    }


    @Override
    public void render(PoseStack poseStack, BossPortalEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!entity.isAlive() || !entity.isChaosPortal()) return;

        float time = entity.tickCount + partialTick;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        drawPortalRipples(consumer, matrix, time);
        drawLeakBeams(consumer, matrix, entity.getUUID(), time);
    }

    private void drawPortalRipples(VertexConsumer consumer, Matrix4f matrix, float time) {

        for (int i = 0; i < 3; i++) {

            float progress = fract(time * 0.022F + i / 3.0F);
            float fade = 1.0F - progress;

            double innerRadius = 0.48D + progress * 1.55D;
            double outerRadius = innerRadius + 0.11D + (1.0D - progress) * 0.14D;

            double y = PORTAL_Y + 0.025D + Math.sin(time * 0.08D + i * 2.1D) * 0.022D;

            int jadeAlpha = Mth.clamp(Math.round(fade * fade * 95.0F), 0, 255);
            int cyanAlpha = Mth.clamp(Math.round(fade * fade * 150.0F), 0, 255);

            drawFlatRing(consumer, matrix, innerRadius, outerRadius, y, 25, 205, 120, jadeAlpha);
            drawFlatRing(consumer, matrix, innerRadius + 0.045D, outerRadius - 0.025D, y + 0.018D, 105, 255, 225, cyanAlpha);
        }
    }

    private void drawLeakBeams(VertexConsumer consumer, Matrix4f matrix, UUID uuid, float time) {

        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();

        for (int i = 0; i < BEAM_COUNT; i++) {

            float speed = 0.095F + (float) hash01(seed, i, 1) * 0.040F;
            float phase = (float) (hash01(seed, i, 2) * Math.PI * 2.0D);
            float signal = 0.5F + 0.5F * Mth.sin(time * speed + phase);

            float energy = smoothstep(0.76F, 1.0F, signal);

            if (energy <= 0.001F) continue;


            double angle = hash01(seed, i, 3) * Math.PI * 2.0D;
            double baseRadius = 0.20D + hash01(seed, i, 4) * 1.15D;
            double height = 1.25D + hash01(seed, i, 5) * 2.45D;

            double outward = 0.30D + hash01(seed, i, 6) * 0.90D;
            double radius = 0.050D + hash01(seed, i, 7) * 0.055D;

            double baseX = Math.cos(angle) * baseRadius;
            double baseZ = Math.sin(angle) * baseRadius;


            for (int segment = 0; segment < BEAM_SEGMENTS; segment++) {

                double t0 = segment / (double) BEAM_SEGMENTS;
                double t1 = (segment + 1) / (double) BEAM_SEGMENTS;

                Vec3 p0 = beamPoint(baseX, baseZ, angle, outward, height, time, i, t0);
                Vec3 p1 = beamPoint(baseX, baseZ, angle, outward, height, time, i, t1);

                double r0 = radius * (1.0D - t0 * 0.78D);
                double r1 = radius * (1.0D - t1 * 0.78D);

                float segmentFade = (float) ((1.0D - t0) * energy);

                int outerAlpha = Mth.clamp(Math.round(segmentFade * 115.0F), 0, 255);
                int coreAlpha = Mth.clamp(Math.round(segmentFade * 205.0F), 0, 255);


                // Jade outer energy
                drawTubeSegment(consumer, matrix, p0, p1, r0, r1, 35, 205, 118, outerAlpha);

                // Bright cyan core
                drawTubeSegment(consumer, matrix, p0, p1, r0 * 0.38D, r1 * 0.38D, 125, 255, 235, coreAlpha);
            }
        }
    }


    private Vec3 beamPoint(double baseX, double baseZ, double angle, double outward, double height, float time, int index, double t) {

        double waveA = Math.sin(time * 0.12D + index * 1.73D + t * 7.0D) * 0.15D * (0.25D + t);
        double waveB = Math.sin(time * 0.075D + index * 2.17D + t * 11.0D) * 0.075D;


        double radialX = Math.cos(angle);
        double radialZ = Math.sin(angle);

        double sideX = -radialZ;
        double sideZ = radialX;


        return new Vec3(baseX + radialX * outward * t + sideX * waveA + radialX * waveB, PORTAL_Y + t * height + Math.sin(t * Math.PI) * 0.12D, baseZ + radialZ * outward * t + sideZ * waveA + radialZ * waveB);
    }

    private void drawTubeSegment(VertexConsumer consumer, Matrix4f matrix, Vec3 p0, Vec3 p1, double radius0, double radius1, int red, int green, int blue, int alpha) {

        if (alpha <= 0) return;

        Vec3 direction = p1.subtract(p0);

        if (direction.lengthSqr() < 0.000001D) return;

        direction = direction.normalize();

        Vec3 reference = Math.abs(direction.y) > 0.90D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 side = direction.cross(reference).normalize();
        Vec3 up = side.cross(direction).normalize();


        for (int i = 0; i < BEAM_SIDES; i++) {

            double a0 = Math.PI * 2.0D * i / BEAM_SIDES;
            double a1 = Math.PI * 2.0D * (i + 1) / BEAM_SIDES;

            Vec3 start0 = tubePoint(p0, side, up, a0, radius0);
            Vec3 start1 = tubePoint(p0, side, up, a1, radius0);

            Vec3 end0 = tubePoint(p1, side, up, a0, radius1);
            Vec3 end1 = tubePoint(p1, side, up, a1, radius1);

            addDoubleSidedQuad(consumer, matrix, start0, start1, end1, end0, red, green, blue, alpha);
        }
    }


    private Vec3 tubePoint(Vec3 center, Vec3 side, Vec3 up, double angle, double radius) {
        return center.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
    }


    private void drawFlatRing(VertexConsumer consumer, Matrix4f matrix, double innerRadius, double outerRadius, double y, int red, int green, int blue, int alpha) {

        if (alpha <= 0) return;

        for (int i = 0; i < RING_SEGMENTS; i++) {

            double a0 = Math.PI * 2.0D * i / RING_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / RING_SEGMENTS;

            Vec3 first = new Vec3(Math.cos(a0) * innerRadius, y, Math.sin(a0) * innerRadius);
            Vec3 second = new Vec3(Math.cos(a0) * outerRadius, y + 0.025D, Math.sin(a0) * outerRadius);
            Vec3 third = new Vec3(Math.cos(a1) * outerRadius, y + 0.025D, Math.sin(a1) * outerRadius);
            Vec3 fourth = new Vec3(Math.cos(a1) * innerRadius, y, Math.sin(a1) * innerRadius);

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
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


    private static float fract(float value) {
        return value - (float) Math.floor(value);
    }


    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }


    private static double hash01(long seed, int index, int salt) {
        long value = seed + index * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;

        return (value >>> 11) * 0x1.0p-53;
    }
}