package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class EntropyGravityTrajectoryRenderer {

    private static final double SAMPLE_SPACING = 0.62D;
    private static final float CORE_SIZE = 0.105F;
    private static final float HALO_SIZE = 0.155F;

    private static final float CORE_R = 0.02F;
    private static final float CORE_G = 1.00F;
    private static final float CORE_B = 0.93F;

    private static final int BASE_LIFETIME_TICKS = 28;
    private static final int EXPIRE_STEP_TICKS = 2;

    private static final Map<UUID, TrailData> TRAILS = new HashMap<>();

    private EntropyGravityTrajectoryRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null) {
            TRAILS.clear();
            return;
        }

        long now = level.getGameTime();
        Set<UUID> seenThisTick = new HashSet<>();
        AABB search = minecraft.player.getBoundingBox().inflate(128.0D);

        for (EntropyPhysicsBlockEntity block : level.getEntitiesOfClass(EntropyPhysicsBlockEntity.class, search)) {
            seenThisTick.add(block.getUUID());
            TrailData trail = TRAILS.computeIfAbsent(block.getUUID(), ignored -> new TrailData());

            if (block.getMode() == EntropyPhysicsBlockEntity.MODE_THROWN) {
                addCurrentPosition(trail, block.position().add(0.0D, 0.50D, 0.0D), now);
            } else {
                trail.lastSample = null;
            }
        }

        Iterator<Map.Entry<UUID, TrailData>> iterator = TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrailData> entry = iterator.next();
            TrailData trail = entry.getValue();

            if (!seenThisTick.contains(entry.getKey())) {
                trail.lastSample = null;
            }

            while (!trail.points.isEmpty() && trail.points.peekFirst().expireTick <= now) {
                trail.points.removeFirst();
            }

            if (trail.points.isEmpty() && trail.lastSample == null) {
                iterator.remove();
            }
        }
    }

    private static void addCurrentPosition(TrailData trail, Vec3 current, long now) {
        if (trail.lastSample == null) {
            trail.lastSample = current;
            return;
        }

        Vec3 delta = current.subtract(trail.lastSample);
        double distance = delta.length();

        if (distance < SAMPLE_SPACING) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / distance);
        int safety = 0;

        while (distance >= SAMPLE_SPACING && safety++ < 8) {
            trail.lastSample = trail.lastSample.add(direction.scale(SAMPLE_SPACING));
            addPoint(trail, trail.lastSample, now);

            delta = current.subtract(trail.lastSample);
            distance = delta.length();
            if (distance > 1.0E-6D) {
                direction = delta.scale(1.0D / distance);
            }
        }
    }

    private static void addPoint(TrailData trail, Vec3 position, long now) {
        long earliestExpire = now + BASE_LIFETIME_TICKS;
        long expireTick = Math.max(earliestExpire, trail.nextExpireTick);
        trail.nextExpireTick = expireTick + EXPIRE_STEP_TICKS;
        trail.points.addLast(new TrailPoint(position, expireTick));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TRAILS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (TrailData trail : TRAILS.values()) {
            for (TrailPoint point : trail.points) {
                poseStack.pushPose();
                poseStack.translate(point.position.x - camera.x, point.position.y - camera.y, point.position.z - camera.z);

                RenderSystem.depthMask(false);
                drawCube(poseStack, HALO_SIZE, 0.00F, 1.00F, 0.96F, 0.20F);

                RenderSystem.depthMask(true);
                drawCube(poseStack, CORE_SIZE, CORE_R, CORE_G, CORE_B, 1.00F);

                poseStack.popPose();
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawCube(PoseStack poseStack, float size, float r, float g, float b, float a) {
        float h = size * 0.5F;
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        vertex(buffer, matrix, -h, -h, h, r, g, b, a);
        vertex(buffer, matrix, h, -h, h, r, g, b, a);
        vertex(buffer, matrix, h, h, h, r, g, b, a);
        vertex(buffer, matrix, -h, h, h, r, g, b, a);

        vertex(buffer, matrix, h, -h, -h, r, g, b, a);
        vertex(buffer, matrix, -h, -h, -h, r, g, b, a);
        vertex(buffer, matrix, -h, h, -h, r, g, b, a);
        vertex(buffer, matrix, h, h, -h, r, g, b, a);

        vertex(buffer, matrix, h, -h, h, r, g, b, a);
        vertex(buffer, matrix, h, -h, -h, r, g, b, a);
        vertex(buffer, matrix, h, h, -h, r, g, b, a);
        vertex(buffer, matrix, h, h, h, r, g, b, a);

        vertex(buffer, matrix, -h, -h, -h, r, g, b, a);
        vertex(buffer, matrix, -h, -h, h, r, g, b, a);
        vertex(buffer, matrix, -h, h, h, r, g, b, a);
        vertex(buffer, matrix, -h, h, -h, r, g, b, a);

        vertex(buffer, matrix, -h, h, h, r, g, b, a);
        vertex(buffer, matrix, h, h, h, r, g, b, a);
        vertex(buffer, matrix, h, h, -h, r, g, b, a);
        vertex(buffer, matrix, -h, h, -h, r, g, b, a);
        
        vertex(buffer, matrix, -h, -h, -h, r, g, b, a);
        vertex(buffer, matrix, h, -h, -h, r, g, b, a);
        vertex(buffer, matrix, h, -h, h, r, g, b, a);
        vertex(buffer, matrix, -h, -h, h, r, g, b, a);

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }

    private static final class TrailData {
        private final Deque<TrailPoint> points = new ArrayDeque<>();
        private Vec3 lastSample;
        private long nextExpireTick;
    }

    private record TrailPoint(Vec3 position, long expireTick) {
    }
}
