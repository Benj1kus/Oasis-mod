package com.benji.oasiso.client.weather;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.block.entity.StormTotemBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StormTotemSandstormRenderer {

    private static final int STREAM_COUNT = 44;
    private static final int CURTAIN_COUNT = 22;
    private static final int STREAM_SEGMENTS = 24;
    private static final int CURTAIN_SEGMENTS = 16;
    private static final double RENDER_DISTANCE = 42.0D;

    private StormTotemSandstormRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        List<StormTotemBlockEntity.ClientStormSource> sources = StormTotemBlockEntity.getClientSources(minecraft.level.dimension());

        if (sources.isEmpty()) {
            return;
        }


        Vec3 cameraPosition = event.getCamera().getPosition();

        List<StormTotemBlockEntity.ClientStormSource> visibleSources = new ArrayList<>();

        double renderDistanceSqr = RENDER_DISTANCE * RENDER_DISTANCE;

        for (StormTotemBlockEntity.ClientStormSource source : sources) {

            BlockPos pos = source.pos();

            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.8D;
            double centerZ = pos.getZ() + 0.5D;

            if (cameraPosition.distanceToSqr(centerX, centerY, centerZ) > renderDistanceSqr) {
                continue;
            }

            if (source.intensity() <= 0.002F) {
                continue;
            }

            visibleSources.add(source);
        }

        if (visibleSources.isEmpty()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = new Matrix4f().identity();

        double time = (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0D;

        for (StormTotemBlockEntity.ClientStormSource source : visibleSources) {

            renderStorm(buffer, matrix, source, cameraPosition, time);
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }


    private static void renderStorm(BufferBuilder buffer, Matrix4f matrix, StormTotemBlockEntity.ClientStormSource source, Vec3 cameraPosition, double time) {
        BlockPos pos = source.pos();

        double centerX = pos.getX() + 0.5D - cameraPosition.x;
        double centerY = pos.getY() + 0.75D - cameraPosition.y;
        double centerZ = pos.getZ() + 0.5D - cameraPosition.z;

        float intensity = smoothstep(source.intensity());
        long seed = pos.asLong();

        for (int i = 0; i < CURTAIN_COUNT; i++) {
            drawDustCurtain(buffer, matrix, centerX, centerY, centerZ, time, intensity, seed, i);
        }

        for (int i = 0; i < STREAM_COUNT; i++) {
            drawOrbitalStream(buffer, matrix, centerX, centerY, centerZ, time, intensity, seed, i);
        }
    }

    private static void drawOrbitalStream(BufferBuilder buffer, Matrix4f matrix, double centerX, double centerY, double centerZ, double time, float intensity, long sourceSeed, int index) {

        double radius = 3.0D + hash01(sourceSeed, index, 1) * 11.3D;


        double width = 0.65D + hash01(sourceSeed, index, 2) * 1.35D;


        double baseY = 0.15D + hash01(sourceSeed, index, 3) * 4.8D;
        double arcLength = Math.toRadians(100.0D + hash01(sourceSeed, index, 4) * 190.0D);
        double phase = hash01(sourceSeed, index, 5) * Math.PI * 2.0D;
        double speed = 3.5D + hash01(sourceSeed, index, 6) * 1.45D;

        int direction = (index % 3 == 0) ? -1 : 1;

        double tilt = 0.15D + hash01(sourceSeed, index, 7) * 0.85D;
        double turbulence = 0.12D + hash01(sourceSeed, index, 8) * 0.45D;

        int red = 190 + (int) (hash01(sourceSeed, index, 9) * 44.0D);
        int green = 132 + (int) (hash01(sourceSeed, index, 10) * 42.0D);
        int blue = 65 + (int) (hash01(sourceSeed, index, 11) * 38.0D);

        double startAngle = phase + time * speed * direction;


        for (int segment = 0; segment < STREAM_SEGMENTS; segment++) {

            double progress0 = segment / (double) STREAM_SEGMENTS;
            double progress1 = (segment + 1) / (double) STREAM_SEGMENTS;

            double angle0 = startAngle + arcLength * progress0 * direction;
            double angle1 = startAngle + arcLength * progress1 * direction;

            double radius0 = radius + Math.sin(angle0 * 3.0D + phase - time * 2.5D) * turbulence;
            double radius1 = radius + Math.sin(angle1 * 3.0D + phase - time * 2.5D) * turbulence;

            double y0 = centerY + baseY + Math.sin(angle0 + phase) * tilt + Math.sin(angle0 * 4.0D - time * 4.2D + phase) * turbulence;
            double y1 = centerY + baseY + Math.sin(angle1 + phase) * tilt + Math.sin(angle1 * 4.0D - time * 4.2D + phase) * turbulence;

            float alpha0 = getArcAlpha(progress0, intensity, 0.13F + (float) hash01(sourceSeed, index, 12) * 0.12F);
            float alpha1 = getArcAlpha(progress1, intensity, 0.13F + (float) hash01(sourceSeed, index, 12) * 0.12F);


            addRibbonSegment(buffer, matrix, centerX, centerZ, angle0, angle1, radius0, radius1, width, y0, y1, red, green, blue, alpha0, alpha1);
        }
    }

    private static void drawDustCurtain(BufferBuilder buffer, Matrix4f matrix, double centerX, double centerY, double centerZ, double time, float intensity, long sourceSeed, int index) {
        double radius = 12.7D + hash01(sourceSeed, index, 40) * 2.0D;
        double lowerY = -0.4D + hash01(sourceSeed, index, 41) * 1.4D;
        double height = 3.2D + hash01(sourceSeed, index, 42) * 3.8D;
        double arcLength = Math.toRadians(55.0D + hash01(sourceSeed, index, 43) * 105.0D);
        double phase = hash01(sourceSeed, index, 44) * Math.PI * 2.0D;
        double speed = 4.5D + hash01(sourceSeed, index, 45) * 1.3D;

        int direction = index % 2 == 0 ? 1 : -1;

        int red = 180 + (int) (hash01(sourceSeed, index, 46) * 34.0D);
        int green = 120 + (int) (hash01(sourceSeed, index, 47) * 38.0D);
        int blue = 55 + (int) (hash01(sourceSeed, index, 48) * 26.0D);


        double startAngle = phase + time * speed * direction;

        for (int segment = 0; segment < CURTAIN_SEGMENTS; segment++) {

            double progress0 = segment / (double) CURTAIN_SEGMENTS;
            double progress1 = (segment + 1) / (double) CURTAIN_SEGMENTS;

            double angle0 = startAngle + progress0 * arcLength * direction;
            double angle1 = startAngle + progress1 * arcLength * direction;

            double wave0 = Math.sin(angle0 * 3.0D - time * 5.0D + phase) * 0.32D;
            double wave1 = Math.sin(angle1 * 3.0D - time * 5.0D + phase) * 0.32D;

            double radius0 = radius + wave0;
            double radius1 = radius + wave1;

            double x0 = centerX + Math.cos(angle0) * radius0;
            double z0 = centerZ + Math.sin(angle0) * radius0;
            double x1 = centerX + Math.cos(angle1) * radius1;
            double z1 = centerZ + Math.sin(angle1) * radius1;

            double bottom0 = centerY + lowerY + Math.sin(angle0 * 2.0D + phase) * 0.25D;
            double bottom1 = centerY + lowerY + Math.sin(angle1 * 2.0D + phase) * 0.25D;

            double top0 = bottom0 + height;
            double top1 = bottom1 + height;

            float edgeAlpha0 = getArcAlpha(progress0, intensity, 0.105F);
            float edgeAlpha1 = getArcAlpha(progress1, intensity, 0.105F);

            addVertex(buffer, matrix, x0, bottom0, z0, red, green, blue, edgeAlpha0);

            addVertex(buffer, matrix, x0, top0, z0, red, green, blue, edgeAlpha0 * 0.48F);

            addVertex(buffer, matrix, x1, top1, z1, red, green, blue, edgeAlpha1 * 0.48F);

            addVertex(buffer, matrix, x1, bottom1, z1, red, green, blue, edgeAlpha1);
        }
    }

    private static void addRibbonSegment(BufferBuilder buffer, Matrix4f matrix, double centerX, double centerZ, double angle0, double angle1, double radius0, double radius1, double width, double y0, double y1, int red, int green, int blue, float alpha0, float alpha1) {

        double inner0 = radius0 - width * 0.5D;
        double outer0 = radius0 + width * 0.5D;

        double inner1 = radius1 - width * 0.5D;
        double outer1 = radius1 + width * 0.5D;

        double innerX0 = centerX + Math.cos(angle0) * inner0;
        double innerZ0 = centerZ + Math.sin(angle0) * inner0;

        double outerX0 = centerX + Math.cos(angle0) * outer0;
        double outerZ0 = centerZ + Math.sin(angle0) * outer0;

        double innerX1 = centerX + Math.cos(angle1) * inner1;
        double innerZ1 = centerZ + Math.sin(angle1) * inner1;

        double outerX1 = centerX + Math.cos(angle1) * outer1;
        double outerZ1 = centerZ + Math.sin(angle1) * outer1;

        addVertex(buffer, matrix, innerX0, y0 - 0.08D, innerZ0, red, green, blue, alpha0);

        addVertex(buffer, matrix, outerX0, y0 + 0.08D, outerZ0, red, green, blue, alpha0);

        addVertex(buffer, matrix, outerX1, y1 + 0.08D, outerZ1, red, green, blue, alpha1);

        addVertex(buffer, matrix, innerX1, y1 - 0.08D, innerZ1, red, green, blue, alpha1);
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, int red, int green, int blue, float alpha) {
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        buffer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alphaByte).endVertex();
    }

    private static float getArcAlpha(double progress, float intensity, float maximum) {

        double envelope = Math.sin(progress * Math.PI);

        envelope = Math.pow(Math.max(0.0D, envelope), 0.55D);

        return (float) (maximum * intensity * envelope);
    }


    private static double hash01(long sourceSeed, int index, int salt) {
        long value = sourceSeed + index * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;

        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value = value ^ (value >>> 31);

        return (value >>> 11) * 0x1.0p-53;
    }


    private static float smoothstep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }
}