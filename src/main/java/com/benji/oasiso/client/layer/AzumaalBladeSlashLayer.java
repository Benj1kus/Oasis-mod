package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

public class AzumaalBladeSlashLayer extends GeoRenderLayer<AzumaalEntity> {

    private static final long TRAIL_LIFETIME_NS = 300_000_000L;
    private static final long CAPTURE_INTERVAL_NS = 10_000_000L;
    private static final long PARTICLE_INTERVAL_NS = 40_000_000L;

    private static final int MAX_FRAMES = 32;
    private static final float MAX_ALPHA = 0.62F;

    private final Map<AzumaalEntity, TrailData> trails = new WeakHashMap<>();

    private final RandomSource random = RandomSource.create();

    public AzumaalBladeSlashLayer(GeoRenderer<AzumaalEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, AzumaalEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        TrailData trail = trails.computeIfAbsent(animatable, entity -> new TrailData());

        if (animatable.getAnimState() == AzumaalEntity.STATE_SPAWN) {
            trail.clear();
            return;
        }

        long now = System.nanoTime();

        boolean active = animatable.isBladeSplashActive();
        boolean particleAuraActive = animatable.hasBladeParticleAura();

        BladeFrame currentFrame = readBladeFrame(bakedModel, now);

        if (currentFrame == null) {
            return;
        }

        if (particleAuraActive && now - trail.lastParticleNanos >= PARTICLE_INTERVAL_NS) {
            spawnBladeParticles(animatable, currentFrame, trail.lastParticleFrame, AzumaalEntity.SPLASH_BOTH);
            trail.lastParticleFrame = currentFrame;
            trail.lastParticleNanos = now;
        }

        if (!particleAuraActive) {
            trail.lastParticleFrame = null;
        }

        if (active && !trail.wasActive) {
            trail.frames.clear();
            trail.lastCaptureNanos = 0L;
            trail.ribbonMode = animatable.getBladeSplashMode();
        }

        if (active) {

            if (now - trail.lastCaptureNanos >= CAPTURE_INTERVAL_NS) {

                BladeFrame previousFrame = trail.frames.peekLast();
                trail.frames.addLast(currentFrame);
                trail.lastCaptureNanos = now;
                while (trail.frames.size() > MAX_FRAMES) {
                    trail.frames.removeFirst();
                }

                if (now - trail.lastParticleNanos >= PARTICLE_INTERVAL_NS) {
                    spawnBladeParticles(animatable, currentFrame, previousFrame, trail.ribbonMode);
                    trail.lastParticleNanos = now;
                }
            }
        }
        trail.wasActive = active;
        removeExpiredFrames(trail, now);
        if (trail.frames.size() < 2) {
            return;
        }
        drawRibbonHistory(poseStack, animatable, bufferSource, trail, now, partialTick);
    }

    private BladeFrame readBladeFrame(BakedGeoModel model, long time) {
        Vec3 leftBottom = getBonePosition(model, "blade_left_bottom");
        Vec3 leftMiddle = getBonePosition(model, "blade_left_middle");
        Vec3 leftTop = getBonePosition(model, "blade_left_top");
        Vec3 rightBottom = getBonePosition(model, "blade_right_bottom");
        Vec3 rightMiddle = getBonePosition(model, "blade_right_middle");
        Vec3 rightTop = getBonePosition(model, "blade_right_top");

        if (leftBottom == null || leftMiddle == null || leftTop == null || rightBottom == null || rightMiddle == null || rightTop == null) {
            return null;
        }
        return new BladeFrame(time,
                new BladeStrip(leftBottom, leftMiddle, leftTop),
                new BladeStrip(rightBottom, rightMiddle, rightTop));
    }

    private Vec3 getBonePosition(BakedGeoModel model, String boneName) {
        GeoBone bone = model.getBone(boneName).orElse(null);

        if (bone == null) {
            return null;
        }
        Vector3d position = bone.getWorldPosition();
        return new Vec3(position.x, position.y, position.z);
    }

    private void drawRibbonHistory(PoseStack poseStack, AzumaalEntity entity, MultiBufferSource bufferSource, TrailData trail, long now, float partialTick) {
        List<BladeFrame> frames = new ArrayList<>(trail.frames);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        Matrix4f matrix = poseStack.last().pose();

        for (int i = 1; i < frames.size(); i++) {

            BladeFrame previous = frames.get(i - 1);
            BladeFrame current = frames.get(i);

            float previousAge = getAge(previous.time, now);
            float currentAge = getAge(current.time, now);

            Color previousColor = getTrailColor(previousAge);
            Color currentColor = getTrailColor(currentAge);

            // left
            if (trail.ribbonMode == AzumaalEntity.SPLASH_BOTH || trail.ribbonMode == AzumaalEntity.SPLASH_LEFT) {
                drawBladeStrip(matrix, consumer, entity, previous.left, current.left, previousColor, currentColor, partialTick);
            }
            // right
            if (trail.ribbonMode == AzumaalEntity.SPLASH_BOTH || trail.ribbonMode == AzumaalEntity.SPLASH_RIGHT) {
                drawBladeStrip(matrix, consumer, entity, previous.right, current.right, previousColor, currentColor, partialTick);
            }
        }
    }

    private void drawBladeStrip(Matrix4f matrix, VertexConsumer consumer, AzumaalEntity entity, BladeStrip previous, BladeStrip current, Color previousColor, Color currentColor, float partialTick) {
        Vec3 previousBottom = toLocal(previous.bottom, entity, partialTick);
        Vec3 previousMiddle = toLocal(previous.middle, entity, partialTick);
        Vec3 previousTop = toLocal(previous.top, entity, partialTick);
        Vec3 currentBottom = toLocal(current.bottom, entity, partialTick);
        Vec3 currentMiddle = toLocal(current.middle, entity, partialTick);
        Vec3 currentTop = toLocal(current.top, entity, partialTick);

        // blade bottom
        addDoubleSidedQuad(consumer, matrix, previousBottom, previousMiddle, currentMiddle, currentBottom, previousColor, currentColor);
        // blade top
        addDoubleSidedQuad(consumer, matrix, previousMiddle, previousTop, currentTop, currentMiddle, previousColor, currentColor);
    }

    private void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, Color oldColor, Color newColor) {
        // front part
        addVertex(consumer, matrix, first, oldColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, fourth, newColor);
        //back part
        addVertex(consumer, matrix, fourth, newColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, first, oldColor);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, Color color) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(toColorComponent(color.red), toColorComponent(color.green), toColorComponent(color.blue), toColorComponent(color.alpha)).endVertex();
    }

    private Vec3 toLocal(Vec3 worldPosition, AzumaalEntity entity, float partialTick) {
        double currentX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double currentY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double currentZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        float currentYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double deltaX = worldPosition.x - currentX;
        double deltaY = worldPosition.y - currentY;
        double deltaZ = worldPosition.z - currentZ;

        double yawRadians = Math.toRadians(currentYaw - 180.0F);
        double cos = Math.cos(-yawRadians);
        double sin = Math.sin(-yawRadians);

        return new Vec3(deltaX * cos - deltaZ * sin, deltaY, deltaX * sin + deltaZ * cos);
    }

    private void spawnBladeParticles(AzumaalEntity entity, BladeFrame current, BladeFrame previous, int bladeMode) {
        if (!(entity.level() instanceof ClientLevel level)) {
            return;
        }

        Vec3 leftVelocity = getBladeVelocity(previous == null ? null : previous.left, current.left);

        Vec3 rightVelocity = getBladeVelocity(previous == null ? null : previous.right, current.right);


        for (int i = 0; i < 2; i++) {

            if (bladeMode == AzumaalEntity.SPLASH_BOTH || bladeMode == AzumaalEntity.SPLASH_LEFT) {
                spawnParticleOnBlade(level, current.left, leftVelocity);
            }

            if (bladeMode == AzumaalEntity.SPLASH_BOTH || bladeMode == AzumaalEntity.SPLASH_RIGHT) {
                spawnParticleOnBlade(level, current.right, rightVelocity);
            }
        }
    }

    private Vec3 getBladeVelocity(BladeStrip previous, BladeStrip current) {
        if (previous == null) {
            return Vec3.ZERO;
        }
        return current.top.subtract(previous.top).scale(0.12D);
    }

    private void spawnParticleOnBlade(ClientLevel level, BladeStrip blade, Vec3 inheritedVelocity) {
        Vec3 position = randomPointOnBlade(blade);

        double randomX = (random.nextDouble() - 0.5D) * 0.035D;
        double randomY = (random.nextDouble() - 0.5D) * 0.025D;
        double randomZ = (random.nextDouble() - 0.5D) * 0.035D;

        level.addParticle(Oasiso.PURPLE_STARS.get(), position.x, position.y, position.z, inheritedVelocity.x + randomX, inheritedVelocity.y + randomY, inheritedVelocity.z + randomZ);
    }

    private Vec3 randomPointOnBlade(BladeStrip blade) {
        double progress = random.nextDouble() * 2.0D;

        if (progress < 1.0D) {
            return blade.bottom.lerp(blade.middle, progress);
        }

        return blade.middle.lerp(blade.top, progress - 1.0D);
    }

    private void removeExpiredFrames(TrailData trail, long now) {
        while (!trail.frames.isEmpty()) {
            BladeFrame first = trail.frames.peekFirst();
            if (now - first.time <= TRAIL_LIFETIME_NS) {
                break;
            }
            trail.frames.removeFirst();
        }
    }

    private float getAge(long frameTime, long now) {
        return Mth.clamp((now - frameTime) / (float) TRAIL_LIFETIME_NS, 0.0F, 1.0F);
    }

    private Color getTrailColor(float age) {
        float fade = 1.0F - age;

        float red = Mth.lerp(age, 0.82F, 0.25F);
        float green = Mth.lerp(age, 0.18F, 0.55F);
        float blue = 1.0F;
        float alpha = fade * MAX_ALPHA;

        return new Color(red, green, blue, alpha);
    }

    private int toColorComponent(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }

    private static final class TrailData {

        private final Deque<BladeFrame> frames = new ArrayDeque<>();

        private BladeFrame lastParticleFrame;
        private boolean wasActive;

        private int ribbonMode = AzumaalEntity.SPLASH_NONE;

        private long lastCaptureNanos;

        private long lastParticleNanos;

        private void clear() {
            this.frames.clear();
            this.ribbonMode = AzumaalEntity.SPLASH_NONE;
            this.wasActive = false;
            this.lastParticleFrame = null;
            this.lastCaptureNanos = 0L;
            this.lastParticleNanos = 0L;
        }
    }

    private record BladeFrame(long time, BladeStrip left, BladeStrip right) {
    }

    private record BladeStrip(Vec3 bottom, Vec3 middle, Vec3 top) {
    }

    private record Color(float red, float green, float blue, float alpha) {
    }
}