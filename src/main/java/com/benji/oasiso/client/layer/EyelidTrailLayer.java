package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.EyelidEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class EyelidTrailLayer {

    private static final long TRAIL_LIFETIME_NS = 320_000_000L;
    private static final long CAPTURE_INTERVAL_NS = 10_000_000L;
    private static final int MAX_FRAMES = 40;
    private static final double TRAIL_BONE_OFFSET = 4.75D / 16.0D;
    private static final double TRAIL_BONE_Y_OFFSET = -4.5D / 16.0D;
    private static final double MAX_HALF_WIDTH = 0.24D;
    private static final float MAX_ALPHA = 0.64F;

    private final Map<EyelidEntity, TrailData> trails = new WeakHashMap<>();

    public void render(EyelidEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        TrailData trail = this.trails.computeIfAbsent(entity, ignored -> new TrailData());

        int mode = entity.getFlightMode();

        if (trail.lastMode != mode) {
            trail.frames.clear();
            trail.lastCaptureNanos = 0L;
            trail.lastMode = mode;
        }

        long now = System.nanoTime();
        captureFrame(entity, trail, now, partialTick);
        removeExpiredFrames(trail, now);

        if (trail.frames.size() < 2) {
            return;
        }

        drawTrail(poseStack, entity, bufferSource, trail, now, partialTick);
    }

    private void captureFrame(EyelidEntity entity, TrailData trail, long now, float partialTick) {
        if (now - trail.lastCaptureNanos < CAPTURE_INTERVAL_NS) {
            return;
        }
        trail.lastCaptureNanos = now;
        trail.frames.addLast(new TrailFrame(now, getTrailBoneWorldPosition(entity, partialTick)));

        while (trail.frames.size() > MAX_FRAMES) {
            trail.frames.removeFirst();
        }
    }

    private Vec3 getTrailBoneWorldPosition(EyelidEntity entity, float partialTick) {

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) + entity.getBbHeight() * 0.5D;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        Vec3 center = new Vec3(x, y, z);

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);

        Vec3 forward = new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);

        if (forward.lengthSqr() < 0.000001D) {
            return center;
        }


        forward = forward.normalize();
        Vec3 backPosition = center.subtract(forward.scale(TRAIL_BONE_OFFSET));

        return backPosition.add(0.0D, TRAIL_BONE_Y_OFFSET, 0.0D);
    }

    private void drawTrail(PoseStack poseStack, EyelidEntity entity, MultiBufferSource bufferSource, TrailData trail, long now, float partialTick) {
        List<TrailFrame> frames = new ArrayList<>(trail.frames);

        Vec3 liveHead = getTrailBoneWorldPosition(entity, partialTick);

        if (!frames.isEmpty()) {
            frames.add(new TrailFrame(now, liveHead));
        }

        double renderX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double renderY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double renderZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        Matrix4f matrix = poseStack.last().pose();

        for (int i = 1; i < frames.size(); i++) {

            TrailFrame previous = frames.get(i - 1);
            TrailFrame current = frames.get(i);

            Vec3 movement = current.position.subtract(previous.position);

            if (movement.lengthSqr() < 0.000001D) {
                continue;
            }

            float previousAge = getAge(previous.time, now);
            float currentAge = getAge(current.time, now);

            double previousWidth = MAX_HALF_WIDTH * (1.0D - previousAge);
            double currentWidth = MAX_HALF_WIDTH * (1.0D - currentAge);

            Color previousColor = getTrailColor(previousAge);
            Color currentColor = getTrailColor(currentAge);

            Vec3 previousVertical = new Vec3(0.0D, previousWidth, 0.0D);
            Vec3 currentVertical = new Vec3(0.0D, currentWidth, 0.0D);


            addWorldQuad(consumer, matrix, previous.position.subtract(previousVertical), previous.position.add(previousVertical), current.position.add(currentVertical), current.position.subtract(currentVertical), renderX, renderY, renderZ, previousColor, currentColor);

            Vec3 side = new Vec3(-movement.z, 0.0D, movement.x);

            if (side.lengthSqr() < 0.0001D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);

            } else {
                side = side.normalize();
            }

            Vec3 previousSide = side.scale(previousWidth);
            Vec3 currentSide = side.scale(currentWidth);

            addWorldQuad(consumer, matrix, previous.position.subtract(previousSide), previous.position.add(previousSide), current.position.add(currentSide), current.position.subtract(currentSide), renderX, renderY, renderZ, previousColor, currentColor);
        }
    }


    private void addWorldQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, double renderX, double renderY, double renderZ, Color oldColor, Color newColor) {

        Vec3 localFirst = toEntityLocal(first, renderX, renderY, renderZ);
        Vec3 localSecond = toEntityLocal(second, renderX, renderY, renderZ);
        Vec3 localThird = toEntityLocal(third, renderX, renderY, renderZ);
        Vec3 localFourth = toEntityLocal(fourth, renderX, renderY, renderZ);

        addDoubleSidedQuad(consumer, matrix, localFirst, localSecond, localThird, localFourth, oldColor, newColor);
    }


    private Vec3 toEntityLocal(Vec3 worldPosition, double renderX, double renderY, double renderZ) {
        return new Vec3(worldPosition.x - renderX, worldPosition.y - renderY, worldPosition.z - renderZ);
    }

    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, Color oldColor, Color newColor) {

        addVertex(consumer, matrix, first, oldColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, fourth, newColor);
        //back face
        addVertex(consumer, matrix, fourth, newColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, first, oldColor);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, Color color) {
        consumer.vertex(matrix,
                (float) position.x, (float) position.y, (float) position.z).color(toColorComponent(color.red),
                toColorComponent(color.green),
                toColorComponent(color.blue),
                toColorComponent(color.alpha)).endVertex();
    }

    private void removeExpiredFrames(TrailData trail, long now) {
        while (!trail.frames.isEmpty()) {
            TrailFrame first = trail.frames.peekFirst();
            if (now - first.time <= TRAIL_LIFETIME_NS) {
                break;
            }
            trail.frames.removeFirst();
        }
    }

    private float getAge(long time, long now) {
        return Mth.clamp((now - time) / (float) TRAIL_LIFETIME_NS, 0.0F, 1.0F);
    }

    private Color getTrailColor(float age) {
        float fade = 1.0F - age;

        float red = Mth.lerp(age, 0.18F, 0.04F);
        float green = Mth.lerp(age, 1.0F, 0.48F);
        float blue = Mth.lerp(age, 0.68F, 0.38F);
        float alpha = fade * MAX_ALPHA;

        return new Color(red, green, blue, alpha);
    }

    private int toColorComponent(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }

    private static final class TrailData {
        private final Deque<TrailFrame> frames = new ArrayDeque<>();
        private long lastCaptureNanos;
        private int lastMode = -1;
    }

    private record TrailFrame(long time, Vec3 position) {
    }

    private record Color(float red, float green, float blue, float alpha) {
    }
}