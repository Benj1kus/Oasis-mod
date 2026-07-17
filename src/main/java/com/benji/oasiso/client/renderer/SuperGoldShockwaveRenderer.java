package com.benji.oasiso.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.benji.oasiso.Oasiso;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SuperGoldShockwaveRenderer {

    public static class SandShockwave {
        public final Vec3 center;
        public int delay;
        public int age = 0;
        public final int maxAge = 15; // Время жизни в тиках
        public final float targetRadius;

        public SandShockwave(Vec3 center, int delay, float targetRadius) {
            this.center = center;
            this.delay = delay;
            this.targetRadius = targetRadius;
        }
    }

    private static final List<SandShockwave> SHOCKWAVES = new ArrayList<>();

    // Вызываем этот метод для спавна 3-х кругов
    public static void spawnSandShockwave(Player player) {
        Vec3 center = player.position().add(0, 0.1D, 0); // Чуть выше земли

        // Спавним 3 круга: первый сразу, второй через 3 тика, третий через 6 тиков
        SHOCKWAVES.add(new SandShockwave(center, 0, 5.0F));
        SHOCKWAVES.add(new SandShockwave(center, 3, 4.0F));
        SHOCKWAVES.add(new SandShockwave(center, 6, 3.0F));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SHOCKWAVES.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Iterator<SandShockwave> iterator = SHOCKWAVES.iterator();
            while (iterator.hasNext()) {
                SandShockwave wave = iterator.next();
                if (wave.delay > 0) {
                    wave.delay--; // Ждем, пока пройдет задержка
                } else {
                    wave.age++;
                    if (wave.age >= wave.maxAge) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (SHOCKWAVES.isEmpty()) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        RenderSystem.disableCull();
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
        for (SandShockwave wave : SHOCKWAVES) {
            drawSandRing(buffer, matrix, wave, partialTick);
        }
        tesselator.end();

        poseStack.popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void drawSandRing(BufferBuilder buffer, Matrix4f matrix, SandShockwave wave, float partialTick) {
        if (wave.delay > 0) return; // Не рисуем, если кольцо еще "в задержке"

        float progress = (wave.age + partialTick) / (float) wave.maxAge;
        if (progress > 1.0F) return;

        float currentOuterRadius = wave.targetRadius * Math.min(1.0F, progress * 1.5F); // Быстрое расширение
        float thickness = 1.0F * (1.0F - progress); // Кольцо становится тоньше со временем
        float currentInnerRadius = Math.max(0.0F, currentOuterRadius - thickness);

        float heightOffset = progress * 1.5F; // Плавно поднимается на 1.5 блока вверх

        int alpha = (int) (200 * (1.0F - progress)); // Плавно исчезает

        // Песчаные цвета (Градиент)
        int innerR = 237, innerG = 216, innerB = 148;
        int outerR = 210, outerG = 190, outerB = 120;

        int segments = 32;
        Vec3 centerPos = wave.center.add(0, heightOffset, 0);

        Vec3 prevInner = null, prevOuter = null, firstInner = null, firstOuter = null;

        for (int i = 0; i <= segments; i++) {
            double angle = (i * 2.0 * Math.PI) / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // Кольцо лежит горизонтально в плоскости XZ
            Vec3 dirVec = new Vec3(cos, 0, sin);

            Vec3 innerPos = centerPos.add(dirVec.scale(currentInnerRadius));
            Vec3 outerPos = centerPos.add(dirVec.scale(currentOuterRadius));

            if (i == 0) {
                firstInner = innerPos;
                firstOuter = outerPos;
            } else {
                addVertex(buffer, matrix, prevInner, innerR, innerG, innerB, alpha);
                addVertex(buffer, matrix, prevOuter, outerR, outerG, outerB, alpha);
                addVertex(buffer, matrix, outerPos, outerR, outerG, outerB, alpha);
                addVertex(buffer, matrix, innerPos, innerR, innerG, innerB, alpha);
            }

            prevInner = innerPos;
            prevOuter = outerPos;
        }

        if (prevInner != null && firstInner != null) {
            addVertex(buffer, matrix, prevInner, innerR, innerG, innerB, alpha);
            addVertex(buffer, matrix, prevOuter, outerR, outerG, outerB, alpha);
            addVertex(buffer, matrix, firstOuter, outerR, outerG, outerB, alpha);
            addVertex(buffer, matrix, firstInner, innerR, innerG, innerB, alpha);
        }
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 pos, int r, int g, int b, int a) {
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(r, g, b, a).endVertex();
    }
}