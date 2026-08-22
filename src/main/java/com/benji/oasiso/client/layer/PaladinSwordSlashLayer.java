package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.PaladinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class PaladinSwordSlashLayer extends GeoRenderLayer<PaladinEntity> {

    private static final long TRAIL_LIFETIME_NS = 380_000_000L;
    private static final long CAPTURE_INTERVAL_NS = 10_000_000L;
    private static final int MAX_FRAMES = 40;
    private static final float MAX_ALPHA = 0.78F;

    private static final double TOP_EXTENSION = 0.65D;
    private static final double BOTTOM_EXTENSION = 0.18D;

    private final Map<PaladinEntity, TrailData> trails = new WeakHashMap<>();

    public PaladinSwordSlashLayer(GeoRenderer<PaladinEntity> renderer) {
        super(renderer);
    }


    @Override
    public void render(PoseStack poseStack, PaladinEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        TrailData trail = this.trails.computeIfAbsent(entity, ignored -> new TrailData());

        if (!entity.isAlive()) {
            trail.clear();
            return;
        }

        long now = System.nanoTime();
        boolean active = entity.isSwordSplashActive();
        BladeFrame current = readBladeFrame(bakedModel, now);

        if (current == null) {
            return;
        }

        if (active && !trail.wasActive) {
            trail.frames.clear();
            trail.lastCaptureNanos = 0L;
        }

        if (active && now - trail.lastCaptureNanos >= CAPTURE_INTERVAL_NS) {
            trail.frames.addLast(current);
            trail.lastCaptureNanos = now;

            while (trail.frames.size() > MAX_FRAMES) {
                trail.frames.removeFirst();
            }
        }

        trail.wasActive = active;
        removeExpired(trail, now);

        if (trail.frames.size() < 2) {
            return;
        }

        drawHistory(poseStack, entity, bufferSource, trail, now, partialTick);
    }

    private BladeFrame readBladeFrame(BakedGeoModel model, long time) {
        Vec3 bottom = getBonePosition(model, "sword_bottom");
        Vec3 middle = getBonePosition(model, "sword_middle");
        Vec3 top = getBonePosition(model, "sword_top");

        if (bottom == null || middle == null || top == null) {
            return null;
        }

        Vec3 swordDirection = top.subtract(bottom);

        if (swordDirection.lengthSqr() > 0.00001D) {
            swordDirection = swordDirection.normalize();
            bottom = bottom.subtract(swordDirection.scale(BOTTOM_EXTENSION));
            top = top.add(swordDirection.scale(TOP_EXTENSION));
        }
        return new BladeFrame(time, bottom, middle, top);
    }

    private Vec3 getBonePosition(BakedGeoModel model, String name) {
        GeoBone bone = model.getBone(name).orElse(null);

        if (bone == null) {
            return null;
        }

        Vector3d position = bone.getWorldPosition();
        return new Vec3(position.x, position.y, position.z);
    }

    private void drawHistory(PoseStack poseStack, PaladinEntity entity, MultiBufferSource bufferSource, TrailData trail, long now, float partialTick) {
        List<BladeFrame> frames = new ArrayList<>(trail.frames);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 1; i < frames.size(); i++) {
            BladeFrame previous = frames.get(i - 1);
            BladeFrame current = frames.get(i);

            Color previousColor = getColor(getAge(previous.time, now));
            Color currentColor = getColor(getAge(current.time, now));

            Vec3 pBottom = toLocal(previous.bottom, entity, partialTick);
            Vec3 pMiddle = toLocal(previous.middle, entity, partialTick);
            Vec3 pTop = toLocal(previous.top, entity, partialTick);

            Vec3 cBottom = toLocal(current.bottom, entity, partialTick);
            Vec3 cMiddle = toLocal(current.middle, entity, partialTick);
            Vec3 cTop = toLocal(current.top, entity, partialTick);

            // bottom big
            addDoubleSidedQuad(consumer, matrix, pBottom, pMiddle, cMiddle, cBottom, previousColor, currentColor);
            // top big
            addDoubleSidedQuad(consumer, matrix, pMiddle, pTop, cTop, cMiddle, previousColor, currentColor);
        }
    }


    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, Color oldColor, Color newColor) {

        addVertex(consumer, matrix, first, oldColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, fourth, newColor);


        // back part
        addVertex(consumer, matrix, fourth, newColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, first, oldColor);
    }


    private void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, Color color) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(component(color.red), component(color.green), component(color.blue), component(color.alpha)).endVertex();
    }

    private Vec3 toLocal(Vec3 world, PaladinEntity entity, float partialTick) {
        double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

        double dx = world.x - entityX;
        double dy = world.y - entityY;
        double dz = world.z - entityZ;

        double radians = Math.toRadians(yaw - 180.0F);

        double cos = Math.cos(-radians);
        double sin = Math.sin(-radians);

        return new Vec3(dx * cos - dz * sin, dy, dx * sin + dz * cos);
    }

    private void removeExpired(TrailData trail, long now) {
        while (!trail.frames.isEmpty()) {
            BladeFrame first = trail.frames.peekFirst();

            if (now - first.time <= TRAIL_LIFETIME_NS) {
                break;
            }
            trail.frames.removeFirst();
        }
    }


    private float getAge(long time, long now) {
        return Mth.clamp((now - time) / (float) TRAIL_LIFETIME_NS, 0.0F, 1.0F);
    }

    private Color getColor(float age) {
        float fade = 1.0F - age;

        float red = Mth.lerp(age, 0.78F, 0.08F);
        float green = Mth.lerp(age, 1.0F, 0.78F);
        float blue = Mth.lerp(age, 1.0F, 0.88F);
        float alpha = fade * MAX_ALPHA;

        return new Color(red, green, blue, alpha);
    }

    private int component(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }

    private static final class TrailData {

        private final Deque<BladeFrame> frames = new ArrayDeque<>();

        private boolean wasActive;
        private long lastCaptureNanos;

        private void clear() {

            this.frames.clear();
            this.wasActive = false;
            this.lastCaptureNanos = 0L;
        }
    }

    private record BladeFrame(long time, Vec3 bottom, Vec3 middle, Vec3 top) {
    }

    private record Color(float red, float green, float blue, float alpha) {
    }
}