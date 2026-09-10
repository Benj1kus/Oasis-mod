package com.benji.oasiso.client.renderutil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class EntropyRopeRenderUtil {

    private static final float HALO_WIDTH = 0.100F;
    private static final float CORE_WIDTH = 0.034F;

    private static final float HALO_R = 0.00F;
    private static final float HALO_G = 1.00F;
    private static final float HALO_B = 0.64F;
    private static final float HALO_A = 0.20F;

    private static final float CORE_R = 0.10F;
    private static final float CORE_G = 1.00F;
    private static final float CORE_B = 0.88F;
    private static final float CORE_A = 0.94F;

    private EntropyRopeRenderUtil() {
    }

    public static void renderCyanRope(PoseStack poseStack, List<Vec3> points, Vec3 camera, Vec3 renderOrigin) {
        if (points == null || points.size() < 2) {
            return;
        }

        renderCyanRope(poseStack, points.size(), points::get, camera, renderOrigin);
    }
    public static void renderCyanRope(PoseStack poseStack, int pointCount, PointProvider provider, Vec3 camera, Vec3 renderOrigin) {
        if (pointCount < 2 || provider == null) {
            return;
        }

        prepareRenderState();
        drawLayer(poseStack, pointCount, provider, camera, renderOrigin, HALO_WIDTH, HALO_R, HALO_G, HALO_B, HALO_A);
        drawLayer(poseStack, pointCount, provider, camera, renderOrigin, CORE_WIDTH, CORE_R, CORE_G, CORE_B, CORE_A);
        restoreRenderState();
    }

    public static void renderCyanHook(PoseStack poseStack, Vec3 worldPosition, Vec3 renderOrigin, float size) {
        Vec3 local = worldPosition.subtract(renderOrigin);

        prepareRenderState();
        poseStack.pushPose();
        poseStack.translate(local.x, local.y, local.z);
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float h = size * 0.5F;

//XY
        vertex(buffer, matrix, new Vec3(-h, -h, 0.0D), 0.15F, 1.00F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(h, -h, 0.0D), 0.15F, 1.00F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(h, h, 0.0D), 0.15F, 1.00F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(-h, h, 0.0D), 0.15F, 1.00F, 0.90F, 0.95F);
//YZ
        vertex(buffer, matrix, new Vec3(0.0D, -h, -h), 0.15F, 1.00F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(0.0D, -h, h), 0.15F, 1.00F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(0.0D, h, h), 0.15F, 1.00F, 0.90F, 0.95F);
        vertex(buffer, matrix, new Vec3(0.0D, h, -h), 0.15F, 1.00F, 0.90F, 0.95F);

        BufferUploader.drawWithShader(buffer.end());
        poseStack.popPose();
        restoreRenderState();
    }

    private static void drawLayer(PoseStack poseStack, int pointCount, PointProvider provider, Vec3 camera, Vec3 renderOrigin, float width, float red, float green, float blue, float alpha) {
        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < pointCount - 1; i++) {

            Vec3 first = provider.getPoint(i);
            Vec3 second = provider.getPoint(i + 1);

            if (first == null || second == null) {
                continue;
            }

            Vec3 segment = second.subtract(first);

            if (segment.lengthSqr() < 1.0E-8D) {
                continue;
            }

            Vec3 midpoint = first.add(second).scale(0.5D);
            Vec3 toCamera = camera.subtract(midpoint);
            Vec3 side = segment.cross(toCamera);

            if (side.lengthSqr() < 1.0E-8D) {
                side = segment.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-8D) {
                side = new Vec3(1.0D, 0.0D, 0.0D);

            } else {
                side = side.normalize();
            }
            double pulse = 0.88D + 0.12D * Math.sin(i * 0.83D);

            Vec3 half = side.scale(width * pulse);

            Vec3 a = first.add(half).subtract(renderOrigin);
            Vec3 b = first.subtract(half).subtract(renderOrigin);
            Vec3 c = second.subtract(half).subtract(renderOrigin);
            Vec3 d = second.add(half).subtract(renderOrigin);

            vertex(buffer, matrix, a, red, green, blue, alpha);
            vertex(buffer, matrix, b, red, green, blue, alpha);
            vertex(buffer, matrix, c, red, green, blue, alpha);
            vertex(buffer, matrix, d, red, green, blue, alpha);
        }

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void prepareRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void restoreRenderState() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }

    @FunctionalInterface
    public interface PointProvider {

        Vec3 getPoint(int index);
    }
}