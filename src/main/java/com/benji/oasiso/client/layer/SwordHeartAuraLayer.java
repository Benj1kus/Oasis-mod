package com.benji.oasiso.client.layer;

import com.benji.oasiso.common.entity.SwordHeartEntity;
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

public class SwordHeartAuraLayer extends GeoRenderLayer<SwordHeartEntity> {

    public SwordHeartAuraLayer(GeoRenderer<SwordHeartEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, SwordHeartEntity entity, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!entity.isAlive()) return;

        float time = entity.tickCount + partialTick;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        double halfWidth = Math.max(0.18D, entity.getBbWidth() * 0.5D);
        double height = Math.max(0.35D, entity.getBbHeight());

        float cyanPulseA = 0.5F + 0.5F * Mth.sin(time * 0.22F);
        float cyanPulseB = 0.5F + 0.5F * Mth.sin(time * 0.18F + 2.1F);

        double innerExpand = 0.10D + cyanPulseA * 0.03D;
        double outerExpand = 0.19D + cyanPulseB * 0.045D;

        // inner cyan shell
        drawEnergyBox(consumer, matrix, -halfWidth - innerExpand, -0.03D, -halfWidth - innerExpand, halfWidth + innerExpand, height + 0.03D, halfWidth + innerExpand, 40, 255, 215, 42 + (int) (cyanPulseA * 36.0F));

        // outer cyan shell
        drawEnergyBox(consumer, matrix, -halfWidth - outerExpand, -0.06D, -halfWidth - outerExpand, halfWidth + outerExpand, height + 0.06D, halfWidth + outerExpand, 70, 225, 255, 20 + (int) (cyanPulseB * 24.0F));

        drawAuraSquares(consumer, matrix, halfWidth, height, time);
        drawGoldSquares(consumer, matrix, halfWidth, height, time);
    }

    // cyan digital squares
    private void drawAuraSquares(VertexConsumer consumer, Matrix4f matrix, double halfWidth, double height, float time) {
        int squareCount = 10;
        double radius = halfWidth + 0.22D;

        for (int i = 0; i < squareCount; i++) {
            double direction = i % 2 == 0 ? 1.0D : -1.0D;
            double angle = i * 2.399963D + time * 0.045D * direction;
            double localRadius = radius + Math.sin(time * 0.08D + i * 1.7D) * 0.05D;

            double x = Math.cos(angle) * localRadius;
            double z = Math.sin(angle) * localRadius;
            double normalizedHeight = (i + 0.5D) / squareCount;
            double y = normalizedHeight * height + Math.sin(time * 0.11D + i * 1.31D) * 0.09D;
            double size = 0.035D + (i % 3) * 0.010D;

            Vec3 horizontal = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle)).scale(size);
            Vec3 vertical = new Vec3(0.0D, size, 0.0D);
            Vec3 center = new Vec3(x, y, z);

            Vec3 first = center.subtract(horizontal).subtract(vertical);
            Vec3 second = center.add(horizontal).subtract(vertical);
            Vec3 third = center.add(horizontal).add(vertical);
            Vec3 fourth = center.subtract(horizontal).add(vertical);

            float wave = 0.5F + 0.5F * Mth.sin(time * 0.16F + i * 0.9F);

            int red = (int) Mth.lerp(wave, 20.0F, 75.0F);
            int green = (int) Mth.lerp(wave, 235.0F, 255.0F);
            int blue = (int) Mth.lerp(wave, 185.0F, 255.0F);
            int alpha = (int) Mth.lerp(wave, 90.0F, 210.0F);

            addDoubleSidedColorQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }

    // gold outline squares
    private void drawGoldSquares(VertexConsumer consumer, Matrix4f matrix, double halfWidth, double height, float time) {
        int squareCount = 6;
        double radius = halfWidth + 0.34D;

        for (int i = 0; i < squareCount; i++) {
            double angle = i * (Math.PI * 2.0D / squareCount) - time * 0.030D;
            double localRadius = radius + Math.sin(time * 0.06D + i * 1.9D) * 0.045D;

            double x = Math.cos(angle) * localRadius;
            double z = Math.sin(angle) * localRadius;
            double y = height * (0.18D + (i / (double) squareCount) * 0.70D) + Math.sin(time * 0.09D + i) * 0.08D;

            double size = 0.030D + (i % 2) * 0.010D;

            Vec3 horizontal = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle)).scale(size);
            Vec3 vertical = new Vec3(0.0D, size, 0.0D);
            Vec3 center = new Vec3(x, y, z);

            Vec3 first = center.subtract(horizontal).subtract(vertical);
            Vec3 second = center.add(horizontal).subtract(vertical);
            Vec3 third = center.add(horizontal).add(vertical);
            Vec3 fourth = center.subtract(horizontal).add(vertical);

            float wave = 0.5F + 0.5F * Mth.sin(time * 0.12F + i * 1.2F);

            int red = (int) Mth.lerp(wave, 255.0F, 255.0F);
            int green = (int) Mth.lerp(wave, 190.0F, 242.0F);
            int blue = (int) Mth.lerp(wave, 80.0F, 180.0F);
            int alpha = (int) Mth.lerp(wave, 55.0F, 150.0F);

            addDoubleSidedColorQuad(consumer, matrix, first, second, third, fourth, red, green, blue, alpha);
        }
    }

    private void drawEnergyBox(VertexConsumer consumer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int red, int green, int blue, int alpha) {
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, minY, maxZ), new Vec3(maxX, minY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ), red, green, blue, alpha);
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(maxX, minY, minZ), new Vec3(minX, minY, minZ), new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ), red, green, blue, alpha);
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ), new Vec3(minX, maxY, maxZ), new Vec3(minX, maxY, minZ), red, green, blue, alpha);
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(maxX, minY, maxZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ), red, green, blue, alpha);
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, maxY, maxZ), new Vec3(maxX, maxY, maxZ), new Vec3(maxX, maxY, minZ), new Vec3(minX, maxY, minZ), red, green, blue, alpha);
        addDoubleSidedColorQuad(consumer, matrix, new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ), red, green, blue, alpha);
    }

    private void addDoubleSidedColorQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth, int red, int green, int blue, int alpha) {
        addColorVertex(consumer, matrix, first, red, green, blue, alpha);
        addColorVertex(consumer, matrix, second, red, green, blue, alpha);
        addColorVertex(consumer, matrix, third, red, green, blue, alpha);
        addColorVertex(consumer, matrix, fourth, red, green, blue, alpha);

        addColorVertex(consumer, matrix, fourth, red, green, blue, alpha);
        addColorVertex(consumer, matrix, third, red, green, blue, alpha);
        addColorVertex(consumer, matrix, second, red, green, blue, alpha);
        addColorVertex(consumer, matrix, first, red, green, blue, alpha);
    }

    private void addColorVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z).color(red, green, blue, alpha).endVertex();
    }
}