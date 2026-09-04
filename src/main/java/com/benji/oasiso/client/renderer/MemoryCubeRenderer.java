package com.benji.oasiso.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

final class MemoryCubeRenderer {

    private MemoryCubeRenderer() {
    }

    static void renderCube(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture, Level level, BlockPos pos, int packedLight, int packedOverlay, float cyanFlash) {
        cyanFlash = Mth.clamp(cyanFlash, 0.0F, 1.0F);

        float red = Mth.lerp(cyanFlash, 1.0F, 0.16F);
        float green = Mth.lerp(cyanFlash, 1.0F, 1.0F);
        float blue = Mth.lerp(cyanFlash, 1.0F, 0.92F);

        int northLight = faceLight(level, pos, Direction.NORTH, cyanFlash, packedLight);
        int southLight = faceLight(level, pos, Direction.SOUTH, cyanFlash, packedLight);
        int westLight = faceLight(level, pos, Direction.WEST, cyanFlash, packedLight);
        int eastLight = faceLight(level, pos, Direction.EAST, cyanFlash, packedLight);
        int downLight = faceLight(level, pos, Direction.DOWN, cyanFlash, packedLight);
        int upLight = faceLight(level, pos, Direction.UP, cyanFlash, packedLight);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        PoseStack.Pose pose = poseStack.last();

        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        quad(consumer, matrix, normal, northLight, packedOverlay, red, green, blue, 1.0F, 0.0F, 0.0F, -1.0F, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0);
        quad(consumer, matrix, normal, southLight, packedOverlay, red, green, blue, 1.0F, 0.0F, 0.0F, 1.0F, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1);
        quad(consumer, matrix, normal, westLight, packedOverlay, red, green, blue, 1.0F, -1.0F, 0.0F, 0.0F, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0);
        quad(consumer, matrix, normal, eastLight, packedOverlay, red, green, blue, 1.0F, 1.0F, 0.0F, 0.0F, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1);
        quad(consumer, matrix, normal, downLight, packedOverlay, red, green, blue, 1.0F, 0.0F, -1.0F, 0.0F, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1);
        quad(consumer, matrix, normal, upLight, packedOverlay, red, green, blue, 1.0F, 0.0F, 1.0F, 0.0F, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0);
    }

    static float bounceScale(float age) {
        if (age < 0.0F || age >= 10.0F) {
            return 1.0F;
        }
        if (age < 3.0F) {
            return Mth.lerp(smooth(age / 3.0F), 1.0F, 1.16F);
        }
        if (age < 6.0F) {
            return Mth.lerp(smooth((age - 3.0F) / 3.0F), 1.16F, 0.95F);
        }
        return Mth.lerp(smooth((age - 6.0F) / 4.0F), 0.95F, 1.0F);
    }

    static float cyanFlash(float age) {
        if (age < 0.0F || age > 9.0F) {
            return 0.0F;
        }

        float x = age / 9.0F;
        return Mth.sin(x * Mth.PI) * 0.92F;
    }

    static float solvedPush(float age) {
        if (age < 0.0F) {
            return 0.0F;
        }
        if (age < 4.0F) {
            return Mth.lerp(smooth(age / 4.0F), 0.0F, 0.34F);
        }
        if (age < 7.0F) {
            return Mth.lerp(smooth((age - 4.0F) / 3.0F), 0.34F, 0.28F);
        }
        return 0.28F;
    }

    static float solvedJump(float age) {
        if (age < 0.0F || age > 24.0F) {
            return 0.0F;
        }
        float seconds = age / 20.0F;
        return Math.max(0.0F, 4.2F * seconds - 3.5F * seconds * seconds);
    }

    static float solvedRotation(BlockPos pos, float age, int salt) {
        if (age < 0.0F || age > 24.0F) {
            return 0.0F;
        }

        float progress = Mth.clamp(age / 24.0F, 0.0F, 1.0F);
        float arc = Mth.sin(progress * Mth.PI);
        long hash = pos.asLong() ^ (0x9E3779B97F4A7C15L * (salt + 1L));

        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;

        float random = ((hash >>> 40) & 0xFFFFFFL) / 8388607.5F - 1.0F;
        return random * 18.0F * arc;
    }

    static float mismatchShake(float age) {
        if (age < 0.0F || age > 11.0F) {
            return 0.0F;
        }
        float decay = 1.0F - age / 11.0F;
        return Mth.sin(age * 4.2F) * 0.065F * decay;
    }

    private static float smooth(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float normalX, float normalY, float normalZ, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(consumer, matrix, normalMatrix, packedLight, packedOverlay, red, green, blue, alpha, normalX, normalY, normalZ, x0, y0, z0, 0.0F, 1.0F);
        vertex(consumer, matrix, normalMatrix, packedLight, packedOverlay, red, green, blue, alpha, normalX, normalY, normalZ, x1, y1, z1, 1.0F, 1.0F);
        vertex(consumer, matrix, normalMatrix, packedLight, packedOverlay, red, green, blue, alpha, normalX, normalY, normalZ, x2, y2, z2, 1.0F, 0.0F);
        vertex(consumer, matrix, normalMatrix, packedLight, packedOverlay, red, green, blue, alpha, normalX, normalY, normalZ, x3, y3, z3, 0.0F, 0.0F);
    }

    private static int faceLight(Level level, BlockPos pos, Direction direction, float cyanFlash, int fallback) {
        if (cyanFlash > 0.08F) {
            return LightTexture.FULL_BRIGHT;
        }

        if (level == null || pos == null) {
            return fallback;
        }

        return LevelRenderer.getLightColor(level, pos.relative(direction));
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float normalX, float normalY, float normalZ, float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z).color(red, green, blue, alpha).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(normalMatrix, normalX, normalY, normalZ).endVertex();
    }
}
