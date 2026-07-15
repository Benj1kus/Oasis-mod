package com.benji.oasiso.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.*;

@Mod.EventBusSubscriber(modid = com.benji.oasiso.Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DasherTrailRenderer {

    public static class TrailData {
        public LinkedList<Vec3> points = new LinkedList<>();
        public boolean isDashing = false;
    }

    private static final Map<UUID, TrailData> TRAILS = new HashMap<>();
    private static final int MAX_TRAIL_LENGTH = 15;

    public static void updateTrail(UUID uuid, Vec3 pos, boolean isDashing) {
        TrailData data = TRAILS.computeIfAbsent(uuid, k -> new TrailData());
        data.isDashing = isDashing;

        if (isDashing) {
            data.points.addFirst(pos);
            if (data.points.size() > MAX_TRAIL_LENGTH) {
                data.points.removeLast();
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TRAILS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Iterator<Map.Entry<UUID, TrailData>> iterator = TRAILS.entrySet().iterator();
            while (iterator.hasNext()) {
                TrailData data = iterator.next().getValue();
                if (!data.isDashing && !data.points.isEmpty()) {
                    data.points.removeLast();
                }
                if (!data.isDashing && data.points.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (TRAILS.isEmpty()) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.disableCull(); // Благодаря этому вертикальный след видно с обеих сторон
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (TrailData data : TRAILS.values()) {
            drawGradientRibbon(buffer, matrix, data.points);
        }

        tesselator.end();

        poseStack.popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void drawGradientRibbon(BufferBuilder buffer, Matrix4f matrix, LinkedList<Vec3> points) {
        if (points.size() < 2) return;

        // ИЗМЕНЕНИЕ 1: Сделали след в 3 раза шире. Можно настроить здесь (например, 2.5F или 3.5F)
        float startWidth = 3.0F;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            float progress1 = (float) i / points.size();
            float progress2 = (float) (i + 1) / points.size();

            float width1 = startWidth * (1.0F - progress1);
            float width2 = startWidth * (1.0F - progress2);

            int alpha1 = (int) (200 * (1.0F - progress1));
            int alpha2 = (int) (200 * (1.0F - progress2));

            int r1 = 255;
            int g1 = (int) lerp(215, 120, progress1);
            int b1 = 0;

            int r2 = 255;
            int g2 = (int) lerp(215, 120, progress2);
            int b2 = 0;

            // ИЗМЕНЕНИЕ 1: Теперь мы откладываем вершины только вверх и вниз по оси Y (0, height, 0)
            // Это делает след строго вертикальным, независимо от угла камеры
            Vec3 up1 = new Vec3(0, width1 / 2.0D, 0);
            Vec3 up2 = new Vec3(0, width2 / 2.0D, 0);

            addVertex(buffer, matrix, p1.add(up1), r1, g1, b1, alpha1);
            addVertex(buffer, matrix, p1.subtract(up1), r1, g1, b1, alpha1);
            addVertex(buffer, matrix, p2.subtract(up2), r2, g2, b2, alpha2);
            addVertex(buffer, matrix, p2.add(up2), r2, g2, b2, alpha2);
        }
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, int r, int g, int b, int a) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(r, g, b, a).endVertex();
    }

    private static float lerp(float start, float end, float delta) {
        return start + delta * (end - start);
    }
}