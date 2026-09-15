package com.benji.oasiso.client.layer;

import com.benji.oasiso.client.shader.AzumaalStageTwoOutlinePass;
import com.benji.oasiso.common.entity.AzumaalEntity;
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
import com.mojang.math.Axis;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import com.benji.oasiso.client.event.AzumaalMegaBeamCameraShake;

import java.util.Map;
import java.util.WeakHashMap;

public class AzumaalStageTwoMegaBeamLayer extends GeoRenderLayer<AzumaalEntity> {

    private static final int RING_SEGMENTS = 48;

    private static final int BEAM_SEGMENTS = 48;
    private static final int BEAM_SIDES = 8;

    private static final int ENERGY_CHUNKS = 30;
    private static final int SOURCE_RAYS = 10;

    private static final double BEAM_LENGTH = 40.0D;

    private static final Vec3 BEAM_ANCHOR = new Vec3(0.75D / 16.0D, 43.75D / 16.0D, -8.75D / 16.0D);

    private final Map<AzumaalEntity, VisualClock> clocks = new WeakHashMap<>();

    public AzumaalStageTwoMegaBeamLayer(GeoRenderer<AzumaalEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, AzumaalEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!entity.isStageTwo()) {
            return;
        }

        if (AzumaalStageTwoOutlinePass.isRenderingMaskPass()) {

            return;
        }

        int state = entity.getAnimState();

        boolean charging = state == AzumaalEntity.STATE_STAGE_TWO_BEAM_CHARGE;
        boolean active = state == AzumaalEntity.STATE_STAGE_TWO_BEAM_ACTIVE;
        boolean closing = state == AzumaalEntity.STATE_STAGE_TWO_MOUTH_CLOSE;

        if (!charging && !active && !closing) {

            updateClock(entity, state);

            return;
        }

        float stateAge = getStateAge(entity, state, partialTick);

        float globalTime = entity.tickCount + partialTick;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        GeoBone beamBone = bakedModel.getBone("beam_anchor").orElse(null);

        if (beamBone == null) {
            return;
        }

        Vector3d worldAnchor = beamBone.getWorldPosition();

        double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        Vec3 anchorLocal = new Vec3(worldAnchor.x - entityX, worldAnchor.y - entityY, worldAnchor.z - entityZ);

        float bodyYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        poseStack.pushPose();
        poseStack.translate(anchorLocal.x, anchorLocal.y, anchorLocal.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.translate(-BEAM_ANCHOR.x, -BEAM_ANCHOR.y, -BEAM_ANCHOR.z);

        Matrix4f matrix = poseStack.last().pose();

        try {
            if (charging) {
                drawChargingRings(consumer, matrix, stateAge);

                return;
            }

            if (active) {
                AzumaalMegaBeamCameraShake.activate(entity);
                float build = Mth.clamp(stateAge / 24.0F, 0.0F, 1.0F);
                build = build * build * (3.0F - 2.0F * build);
                double length = BEAM_LENGTH * build;
                drawMegaBeam(consumer, matrix, entity, globalTime, length, 0.0F);
                drawSourceRays(consumer, matrix, entity, globalTime, build, 0.0F);
                return;
            }

            float breakup = Mth.clamp(stateAge / 15.0F, 0.0F, 1.0F);
            drawMegaBeam(consumer, matrix, entity, globalTime, BEAM_LENGTH, breakup);
            drawSourceRays(consumer, matrix, entity, globalTime, 1.0F - breakup, breakup);

        } finally {
            poseStack.popPose();
        }
    }

    private void drawChargingRings(VertexConsumer consumer, Matrix4f matrix, float chargeTime) {
        float chargeProgress = Mth.clamp(chargeTime / 100.0F, 0.0F, 1.0F);
        float basePhase = chargeTime * 0.018F + chargeTime * chargeTime * 0.00018F;

        for (int i = 0; i < 4; i++) {

            float phase = fract(basePhase + i * 0.25F);
            float eased = phase * phase * (3.0F - 2.0F * phase);

            double radius = Mth.lerp(eased, 3.50D, 0.18D);
            double forwardOffset = Mth.lerp(eased, 2.70D, 0.10D);
            double z = BEAM_ANCHOR.z - forwardOffset;

            float envelope = Mth.sin(phase * Mth.PI);
            float alpha = envelope * (0.38F + chargeProgress * 0.62F);

            if (alpha <= 0.01F) {
                continue;
            }

            drawRing(consumer, matrix, BEAM_ANCHOR.x, BEAM_ANCHOR.y, z, radius, 0.11D, 18, 122, 115, Mth.clamp(Math.round(alpha * 170.0F), 0, 255));

            drawRing(consumer, matrix, BEAM_ANCHOR.x, BEAM_ANCHOR.y, z - 0.015D, radius * 0.96D, 0.045D, 90, 245, 255, Mth.clamp(Math.round(alpha * 235.0F), 0, 255));
        }
    }

    private void drawMegaBeam(VertexConsumer consumer, Matrix4f matrix, AzumaalEntity entity, float time, double length, float breakup) {
        if (length <= 0.05D) {
            return;
        }

        long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();

        drawBlobbyTube(consumer, matrix, seed, time, length, 1.18D, 18, 145, 132, 16, 42, 145, 160, breakup);
        drawBlobbyTube(consumer, matrix, seed, time + 3.7F, length, 0.58D, 110, 252, 255, 30, 115, 255, 225, breakup);
        drawBlobbyTube(consumer, matrix, seed, time + 7.1F, length, 0.22D, 205, 255, 255, 85, 185, 255, 245, breakup);


        drawDetachedChunks(consumer, matrix, seed, time, length, breakup);
    }


    private void drawBlobbyTube(VertexConsumer consumer, Matrix4f matrix, long seed, float time, double length, double baseRadius, int startR, int startG, int startB, int endR, int endG, int endB, int baseAlpha, float breakup) {
        for (int segment = 0; segment < BEAM_SEGMENTS; segment++) {

            double t0 = segment / (double) BEAM_SEGMENTS;
            double t1 = (segment + 1) / (double) BEAM_SEGMENTS;

            double fragmentHash = hash01(seed, segment, 91);

            if (breakup > 0.0F && fragmentHash < breakup * 0.82D) {
                continue;
            }

            Vec3 center0 = beamCenter(seed, segment, t0, length, time, breakup);
            Vec3 center1 = beamCenter(seed, segment + 1, t1, length, time, breakup);

            double radius0 = beamRadius(baseRadius, t0, time, segment);
            double radius1 = beamRadius(baseRadius, t1, time, segment + 1);

            radius0 *= tipEnvelope(t0);
            radius1 *= tipEnvelope(t1);

            float colorWave = 0.5F + 0.5F * Mth.sin(time * 0.34F + segment * 0.47F);

            int red = lerpChannel(startR, endR, colorWave);
            int green = lerpChannel(startG, endG, colorWave);
            int blue = lerpChannel(startB, endB, colorWave);
            int alpha = Mth.clamp(Math.round(baseAlpha * (1.0F - breakup)), 0, 255);

            drawTubeSegment(consumer, matrix, center0, center1, radius0, radius1, red, green, blue, alpha);
        }
    }


    private Vec3 beamCenter(long seed, int segment, double t, double length, float time, float breakup) {
        double x = BEAM_ANCHOR.x;
        double y = BEAM_ANCHOR.y;
        double z = BEAM_ANCHOR.z - length * t;


        double wobbleEnvelope = Math.sin(t * Math.PI);

        x += Math.sin(time * 0.28D + t * 18.0D) * 0.09D * wobbleEnvelope;
        y += Math.cos(time * 0.35D + t * 15.0D) * 0.075D * wobbleEnvelope;


        if (breakup > 0.0F) {
            double angle = hash01(seed, segment, 141) * Math.PI * 2.0D;
            double drift = breakup * (0.25D + hash01(seed, segment, 142) * 1.75D);

            x += Math.cos(angle) * drift;
            y += Math.sin(angle) * drift;
            z -= breakup * hash01(seed, segment, 143) * 1.3D;
        }

        return new Vec3(x, y, z);
    }


    private double beamRadius(double baseRadius, double t, float time, int segment) {

        double waveA = 0.5D + 0.5D * Math.sin(time * 0.52D + t * 28.0D);
        double waveB = 0.5D + 0.5D * Math.sin(-time * 0.73D + t * 47.0D + segment * 0.17D);
        return baseRadius * (0.68D + waveA * 0.30D + waveB * 0.17D);
    }


    private double tipEnvelope(double t) {
        if (t < 0.84D) {

            return 1.0D;
        }

        double progress = (t - 0.84D) / 0.16D;
        return Math.max(0.05D, 1.0D - progress);
    }

    private void drawDetachedChunks(VertexConsumer consumer, Matrix4f matrix, long seed, float time, double length, float breakup) {
        for (int i = 0; i < ENERGY_CHUNKS; i++) {

            double speed = 0.013D + hash01(seed, i, 201) * 0.025D;
            double progress = fract((float) (i / (double) ENERGY_CHUNKS + time * speed));
            double angle = hash01(seed, i, 202) * Math.PI * 2.0D + time * (0.08D + hash01(seed, i, 203) * 0.15D);
            double radial = 1.15D + hash01(seed, i, 204) * 1.15D;
            radial *= 1.0D + breakup * 1.8D;

            Vec3 center = new Vec3(BEAM_ANCHOR.x + Math.cos(angle) * radial, BEAM_ANCHOR.y + Math.sin(angle) * radial, BEAM_ANCHOR.z - length * progress);

            double size = 0.08D + hash01(seed, i, 205) * 0.22D;
            size *= 1.0D + breakup * 0.8D;
            float colorWave = 0.5F + 0.5F * Mth.sin(time * 0.8F + i * 1.7F);

            int red = lerpChannel(55, 20, colorWave);
            int green = lerpChannel(235, 90, colorWave);
            int blue = lerpChannel(245, 255, colorWave);
            int alpha = Mth.clamp(Math.round(205.0F * (1.0F - breakup)), 0, 255);

            drawEnergyChunk(consumer, matrix, center, size, red, green, blue, alpha);
        }
    }

    private void drawSourceRays(VertexConsumer consumer, Matrix4f matrix, AzumaalEntity entity, float time, float strength, float breakup) {
        if (strength <= 0.01F) {
            return;
        }

        long seed = entity.getUUID().getLeastSignificantBits();

        for (int i = 0; i < SOURCE_RAYS; i++) {

            float signal = 0.5F + 0.5F * Mth.sin(time * 0.82F + i * 2.17F);
            float visibility = smoothstep(0.48F, 0.92F, signal) * strength;
            if (visibility <= 0.01F) {
                continue;
            }

            double length = 2.6D + hash01(seed, i, 301) * 5.4D;
            double angle = hash01(seed, i, 302) * Math.PI * 2.0D;
            double spread = 0.20D + hash01(seed, i, 303) * 0.85D;

            Vec3 start = BEAM_ANCHOR;
            Vec3 end = new Vec3(BEAM_ANCHOR.x + Math.cos(angle) * spread, BEAM_ANCHOR.y + Math.sin(angle) * spread, BEAM_ANCHOR.z - length);

            int outerAlpha = Mth.clamp(Math.round(visibility * 145.0F * (1.0F - breakup)), 0, 255);
            int coreAlpha = Mth.clamp(Math.round(visibility * 240.0F * (1.0F - breakup)), 0, 255);

            drawTubeSegment(consumer, matrix, start, end, 0.075D, 0.005D, 20, 115, 155, outerAlpha);
            drawTubeSegment(consumer, matrix, start, end, 0.027D, 0.002D, 115, 250, 255, coreAlpha);
        }
    }

    private void drawRing(VertexConsumer consumer, Matrix4f matrix, double centerX, double centerY, double centerZ, double radius, double thickness, int red, int green, int blue, int alpha) {
        double innerRadius = Math.max(0.0D, radius - thickness);
        double outerRadius = radius + thickness;

        for (int i = 0; i < RING_SEGMENTS; i++) {

            double a0 = Math.PI * 2.0D * i / RING_SEGMENTS;
            double a1 = Math.PI * 2.0D * (i + 1) / RING_SEGMENTS;

            Vec3 first = new Vec3(centerX + Math.cos(a0) * innerRadius, centerY + Math.sin(a0) * innerRadius, centerZ);
            Vec3 second = new Vec3(centerX + Math.cos(a0) * outerRadius, centerY + Math.sin(a0) * outerRadius, centerZ);
            Vec3 third = new Vec3(centerX + Math.cos(a1) * outerRadius, centerY + Math.sin(a1) * outerRadius, centerZ);
            Vec3 fourth = new Vec3(centerX + Math.cos(a1) * innerRadius, centerY + Math.sin(a1) * innerRadius, centerZ);

            addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }


    private void drawTubeSegment(VertexConsumer consumer, Matrix4f matrix, Vec3 p0, Vec3 p1, double radius0, double radius1, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {

            return;
        }

        Vec3 direction = p1.subtract(p0);

        if (direction.lengthSqr() < 0.000001D) {

            return;
        }

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


    private void drawEnergyChunk(VertexConsumer consumer, Matrix4f matrix, Vec3 center, double size, int red, int green, int blue, int alpha) {

        drawPlaneChunk(consumer, matrix, center, new Vec3(size, 0.0D, 0.0D), new Vec3(0.0D, size, 0.0D), red, green, blue, alpha);
        drawPlaneChunk(consumer, matrix, center, new Vec3(size, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, size), red, green, blue, alpha);
        drawPlaneChunk(consumer, matrix, center, new Vec3(0.0D, size, 0.0D), new Vec3(0.0D, 0.0D, size), red, green, blue, alpha);
    }


    private void drawPlaneChunk(VertexConsumer consumer, Matrix4f matrix, Vec3 center, Vec3 axisA, Vec3 axisB, int red, int green, int blue, int alpha) {
        Vec3 first = center.add(axisA).add(axisB);
        Vec3 second = center.subtract(axisA).add(axisB);
        Vec3 third = center.subtract(axisA).subtract(axisB);
        Vec3 fourth = center.add(axisA).subtract(axisB);

        addDoubleSidedQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
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

    private float getStateAge(AzumaalEntity entity, int state, float partialTick) {
        VisualClock clock = clocks.computeIfAbsent(entity, ignored -> new VisualClock());

        if (clock.state != state) {
            clock.state = state;
            clock.startTick = entity.tickCount;
        }

        return entity.tickCount - clock.startTick + partialTick;
    }


    private void updateClock(AzumaalEntity entity, int state) {
        VisualClock clock = clocks.computeIfAbsent(entity, ignored -> new VisualClock());

        if (clock.state != state) {
            clock.state = state;
            clock.startTick = entity.tickCount;
        }
    }


    private static float fract(float value) {
        return value - (float) Math.floor(value);
    }


    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }


    private static int lerpChannel(int a, int b, float t) {
        return Mth.clamp(Math.round(Mth.lerp(t, a, b)), 0, 255);
    }


    private static double hash01(long seed, int index, int salt) {
        long value = seed + index * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;

        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;

        return (value >>> 11) * 0x1.0p-53;
    }


    private static final class VisualClock {
        private int state = Integer.MIN_VALUE;
        private int startTick;
    }
}