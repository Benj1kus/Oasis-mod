package com.benji.oasiso.client.layer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.item.AzumalitArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AzumalitBladeSlashLayer {

    private static final long TRAIL_LIFETIME_NS = 300_000_000L;
    private static final long CAPTURE_INTERVAL_NS = 8_000_000L;
    private static final long PARTICLE_INTERVAL_NS = 40_000_000L;
    private static final long STALE_TRAIL_NS = 2_000_000_000L;

    private static final int MAX_FRAMES = 40;
    private static final float MAX_ALPHA = 0.72F;

    private static final Map<LivingEntity, TrailData> TRAILS = new WeakHashMap<>();

    private static final RandomSource RANDOM = RandomSource.create();

    private AzumalitBladeSlashLayer() {
    }
    public static void captureBone(LivingEntity wearer, String boneName, GeoBone bone, float partialTick) {
        if (wearer == null || bone == null || !wearer.isAlive() || wearer.isInvisible() || !(wearer.level() instanceof ClientLevel)) {
            return;
        }

        if (!isBladeBone(boneName)) {
            return;
        }
        Matrix4f localMatrix = bone.getLocalSpaceMatrix();

        if (localMatrix == null) {
            return;
        }

        Vector3f markerPivot = new Vector3f(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
        localMatrix.transformPosition(markerPivot);

        if (!Float.isFinite(markerPivot.x) || !Float.isFinite(markerPivot.y) || !Float.isFinite(markerPivot.z)) {
            return;
        }

        Vector3d local = new Vector3d(markerPivot.x, markerPivot.y, markerPivot.z);
        Vec3 worldPosition = ownerLocalToWorld(wearer, local, partialTick);

        TrailData trail = TRAILS.computeIfAbsent(wearer, ignored -> new TrailData());

        long renderKey = (((long) wearer.tickCount) << 32) ^ (Float.floatToRawIntBits(partialTick) & 0xffffffffL);

        if (trail.renderKey != renderKey) {
            trail.renderKey = renderKey;
            trail.current.clear();
            trail.frameCapturedForRender = false;
        }

        switch (boneName) {
            case "blade_left_bottom" -> trail.current.leftBottom = worldPosition;
            case "blade_left_middle" -> trail.current.leftMiddle = worldPosition;
            case "blade_left_top" -> trail.current.leftTop = worldPosition;
            case "blade_right_bottom" -> trail.current.rightBottom = worldPosition;
            case "blade_right_middle" -> trail.current.rightMiddle = worldPosition;
            case "blade_right_top" -> trail.current.rightTop = worldPosition;
        }

        trail.lastSeenNanos = System.nanoTime();
        if (!trail.current.isComplete() || trail.frameCapturedForRender) {
            return;
        }

        captureCompletedFrame(wearer, trail);
    }

    private static void captureCompletedFrame(LivingEntity wearer, TrailData trail) {
        int attackMode = AzumalitArmorItem.getAttackMode(wearer);

        long attackStart = AzumalitArmorItem.getAttackAnimationStartTick(wearer);

        boolean active = attackMode == AzumalitArmorItem.ATTACK_MODE_BOTH && AzumalitArmorItem.isAttackTrailActive(wearer);

        if (!active) {
            trail.wasActive = false;
            return;
        }

        if (!trail.wasActive || trail.attackStartTick != attackStart || trail.ribbonMode != attackMode) {

            trail.frames.clear();
            trail.attackStartTick = attackStart;
            trail.ribbonMode = attackMode;
            trail.lastCaptureNanos = 0L;
            trail.lastParticleNanos = 0L;
        }

        long now = System.nanoTime();

        if (now - trail.lastCaptureNanos < CAPTURE_INTERVAL_NS) {
            trail.wasActive = true;
            return;
        }

        BladeFrame frame = trail.current.toFrame(now);

        BladeFrame previous = trail.frames.peekLast();

        trail.frames.addLast(frame);
        trail.lastCaptureNanos = now;
        trail.frameCapturedForRender = true;
        trail.wasActive = true;

        while (trail.frames.size() > MAX_FRAMES) {
            trail.frames.removeFirst();
        }

        if (now - trail.lastParticleNanos >= PARTICLE_INTERVAL_NS) {

            spawnBladeParticles(wearer, frame, previous, attackMode);

            trail.lastParticleNanos = now;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        ClientLevel level = minecraft.level;

        if (level == null || TRAILS.isEmpty()) {
            return;
        }

        long now = System.nanoTime();

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        PoseStack poseStack = event.getPoseStack();

        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f matrix = poseStack.last().pose();

        Iterator<Map.Entry<LivingEntity, TrailData>> iterator = TRAILS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, TrailData> entry = iterator.next();

            LivingEntity wearer = entry.getKey();

            TrailData trail = entry.getValue();

            if (wearer == null || wearer.level() != level || !wearer.isAlive() || now - trail.lastSeenNanos > STALE_TRAIL_NS) {

                iterator.remove();
                continue;
            }
            removeExpiredFrames(trail, now);
            if (trail.frames.size() < 2) {
                continue;
            }

            drawRibbonHistory(matrix, consumer, trail, now);
        }
        poseStack.popPose();
        bufferSource.endBatch(RenderType.lightning());
    }

    private static void drawRibbonHistory(Matrix4f matrix, VertexConsumer consumer, TrailData trail, long now) {
        List<BladeFrame> frames = new ArrayList<>(trail.frames);

        for (int i = 1; i < frames.size(); i++) {
            BladeFrame previous = frames.get(i - 1);
            BladeFrame current = frames.get(i);

            Color previousColor = getTrailColor(getAge(previous.time, now));
            Color currentColor = getTrailColor(getAge(current.time, now));

            if (trail.ribbonMode == AzumalitArmorItem.ATTACK_MODE_BOTH || trail.ribbonMode == AzumalitArmorItem.ATTACK_MODE_LEFT) {

                drawBladeStrip(matrix, consumer, previous.right, current.right, previousColor, currentColor);
            }

            if (trail.ribbonMode == AzumalitArmorItem.ATTACK_MODE_BOTH || trail.ribbonMode == AzumalitArmorItem.ATTACK_MODE_RIGHT) {

                drawBladeStrip(matrix, consumer, previous.left, current.left, previousColor, currentColor);
            }
        }
    }

    private static void drawBladeStrip(Matrix4f matrix, VertexConsumer consumer, BladeStrip previous, BladeStrip current, Color previousColor, Color currentColor) {
        addDoubleSidedQuad(consumer, matrix, previous.bottom, previous.middle, current.middle, current.bottom, previousColor, currentColor);

        addDoubleSidedQuad(consumer, matrix, previous.middle, previous.top, current.top, current.middle, previousColor, currentColor);
    }

    private static Vec3 ownerLocalToWorld(LivingEntity wearer, Vector3d local, float partialTick) {
        double entityX = Mth.lerp(partialTick, wearer.xOld, wearer.getX());
        double entityY = Mth.lerp(partialTick, wearer.yOld, wearer.getY());
        double entityZ = Mth.lerp(partialTick, wearer.zOld, wearer.getZ());

        float bodyYaw = Mth.rotLerp(partialTick, wearer.yBodyRotO, wearer.yBodyRot);

        double yaw = Math.toRadians(bodyYaw - 180.0F);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double worldX = entityX + local.x * cos - local.z * sin;
        double worldZ = entityZ + local.x * sin + local.z * cos;
        double worldY = entityY + 1.5D - local.y;

        return new Vec3(worldX, worldY, worldZ);
    }

    private static boolean isBladeBone(String boneName) {
        return "blade_left_bottom".equals(boneName) || "blade_left_middle".equals(boneName) || "blade_left_top".equals(boneName) || "blade_right_bottom".equals(boneName) || "blade_right_middle".equals(boneName) || "blade_right_top".equals(boneName);
    }

    private static void spawnBladeParticles(LivingEntity wearer, BladeFrame current, BladeFrame previous, int bladeMode) {
        if (!(wearer.level() instanceof ClientLevel level)) {
            return;
        }

        Vec3 leftVelocity = getBladeVelocity(previous == null ? null : previous.left, current.left);

        Vec3 rightVelocity = getBladeVelocity(previous == null ? null : previous.right, current.right);

        for (int i = 0; i < 2; i++) {
            if (bladeMode == AzumalitArmorItem.ATTACK_MODE_BOTH || bladeMode == AzumalitArmorItem.ATTACK_MODE_LEFT) {

                spawnParticleOnBlade(level, current.right, rightVelocity);
            }

            if (bladeMode == AzumalitArmorItem.ATTACK_MODE_BOTH || bladeMode == AzumalitArmorItem.ATTACK_MODE_RIGHT) {

                spawnParticleOnBlade(level, current.left, leftVelocity);
            }
        }
    }

    private static Vec3 getBladeVelocity(BladeStrip previous, BladeStrip current) {
        if (previous == null) {
            return Vec3.ZERO;
        }

        return current.top.subtract(previous.top).scale(0.12D);
    }

    private static void spawnParticleOnBlade(ClientLevel level, BladeStrip blade, Vec3 inheritedVelocity) {
        Vec3 position = randomPointOnBlade(blade);

        double randomX = (RANDOM.nextDouble() - 0.5D) * 0.035D;
        double randomY = (RANDOM.nextDouble() - 0.5D) * 0.025D;
        double randomZ = (RANDOM.nextDouble() - 0.5D) * 0.035D;

        level.addParticle(Oasiso.PURPLE_STARS.get(), position.x, position.y, position.z, inheritedVelocity.x + randomX, inheritedVelocity.y + randomY, inheritedVelocity.z + randomZ);
    }

    private static Vec3 randomPointOnBlade(BladeStrip blade) {
        double progress = RANDOM.nextDouble() * 2.0D;

        if (progress < 1.0D) {
            return blade.bottom.lerp(blade.middle, progress);
        }

        return blade.middle.lerp(blade.top, progress - 1.0D);
    }

    private static void removeExpiredFrames(TrailData trail, long now) {
        while (!trail.frames.isEmpty()) {
            BladeFrame first = trail.frames.peekFirst();

            if (now - first.time <= TRAIL_LIFETIME_NS) {
                break;
            }

            trail.frames.removeFirst();
        }
    }

    private static float getAge(long frameTime, long now) {
        return Mth.clamp((now - frameTime) / (float) TRAIL_LIFETIME_NS, 0.0F, 1.0F);
    }

    private static Color getTrailColor(float age) {
        float fade = 1.0F - age;
        float red = Mth.lerp(age, 0.82F, 0.25F);
        float green = Mth.lerp(age, 0.18F, 0.55F);
        float blue = 1.0F;
        float alpha = fade * MAX_ALPHA;

        return new Color(red, green, blue, alpha);
    }

    private static void addDoubleSidedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, Color oldColor, Color newColor) {
        addVertex(consumer, matrix, first, oldColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, fourth, newColor);
        addVertex(consumer, matrix, fourth, newColor);
        addVertex(consumer, matrix, third, newColor);
        addVertex(consumer, matrix, second, oldColor);
        addVertex(consumer, matrix, first, oldColor);
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, Color color) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(toColorComponent(color.red), toColorComponent(color.green), toColorComponent(color.blue), toColorComponent(color.alpha)).endVertex();
    }

    private static int toColorComponent(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }

    private static final class TrailData {
        private final Deque<BladeFrame> frames = new ArrayDeque<>();

        private final CurrentBladePose current = new CurrentBladePose();

        private boolean wasActive;
        private boolean frameCapturedForRender;

        private int ribbonMode = AzumalitArmorItem.ATTACK_MODE_NONE;

        private long attackStartTick = Long.MIN_VALUE;

        private long renderKey = Long.MIN_VALUE;

        private long lastCaptureNanos;
        private long lastParticleNanos;
        private long lastSeenNanos;
    }

    private static final class CurrentBladePose {
        private Vec3 leftBottom;
        private Vec3 leftMiddle;
        private Vec3 leftTop;
        private Vec3 rightBottom;
        private Vec3 rightMiddle;
        private Vec3 rightTop;

        private boolean isComplete() {
            return this.leftBottom != null && this.leftMiddle != null && this.leftTop != null && this.rightBottom != null && this.rightMiddle != null && this.rightTop != null;
        }

        private BladeFrame toFrame(long time) {
            return new BladeFrame(time, new BladeStrip(this.leftBottom, this.leftMiddle, this.leftTop), new BladeStrip(this.rightBottom, this.rightMiddle, this.rightTop));
        }

        private void clear() {
            this.leftBottom = null;
            this.leftMiddle = null;
            this.leftTop = null;
            this.rightBottom = null;
            this.rightMiddle = null;
            this.rightTop = null;
        }
    }

    private record BladeFrame(long time, BladeStrip left, BladeStrip right) {
    }

    private record BladeStrip(Vec3 bottom, Vec3 middle, Vec3 top) {
    }

    private record Color(float red, float green, float blue, float alpha) {
    }
}
