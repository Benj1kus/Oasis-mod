package com.benji.oasiso.client.dimension;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ChaosSkyRenderer {

    private static final float MOON_AZIMUTH = -35.0F;


    private static final float MOON_TILT_FROM_ZENITH = 20.0F;

    private static final float MOON_SIZE = 18.0F;
    private static final float MOON_DISTANCE = 90.0F;

    private static final ResourceLocation CHAOS_FLASH = ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/environment/celestial/chaos_flash.png");

    private static final float FLASH_DISTANCE = 88.0F;

    private static final ResourceLocation SKY_BACK = texture("thread_sky_back");
    private static final ResourceLocation SKY_LAYER_2 = texture("thread_sky_layer2");
    private static final ResourceLocation SKY_FRONT = texture("thread_sky_front");

    private static final ResourceLocation MOON_1 = texture("chaos_moon");
    private static final ResourceLocation MOON_2 = texture("chaos_moon2");
    private static final ResourceLocation MOON_3 = texture("chaos_moon3");
    private static final ResourceLocation MOON_4 = texture("chaos_moon4");
    private static final ResourceLocation MOON_5 = texture("chaos_moon5");
    private static final ResourceLocation MOON_6 = texture("chaos_moon6");

    private static final ResourceLocation MOON_SAD = texture("chaos_moon_sad");

    private static final float SKY_SIZE = 100.0F;

    private ChaosSkyRenderer() {
    }

    public static void render(PoseStack skyPoseStack, int ticks, float partialTick) {
        float time = ticks + partialTick;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);


        drawSkyLayer(skyPoseStack, SKY_BACK, 0.0F, 1.0F);
        drawSkyLayer(skyPoseStack, SKY_LAYER_2, -time * 0.006F, 0.9F);
        drawSkyLayer(skyPoseStack, SKY_FRONT, time * 0.01F, 0.95F);

        drawChaosFlash(skyPoseStack, partialTick);

        drawVoidBottom(skyPoseStack, SKY_SIZE);

        drawMoon(skyPoseStack);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawSkyLayer(PoseStack skyPoseStack, ResourceLocation texture, float rotation, float alpha) {
        skyPoseStack.pushPose();

        skyPoseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        Matrix4f skyMatrix = skyPoseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        drawCube(skyMatrix, SKY_SIZE);

        skyPoseStack.popPose();
    }

    private static void drawChaosFlash(PoseStack skyPoseStack, float partialTick) {
        float intensity = ChaosFlashClientHandler.getFlashIntensity(partialTick);

        if (intensity <= 0.001F) {
            return;
        }
        skyPoseStack.pushPose();

        skyPoseStack.mulPose(Axis.YP.rotationDegrees(ChaosFlashClientHandler.getFlashYaw()));
        skyPoseStack.mulPose(Axis.XP.rotationDegrees(ChaosFlashClientHandler.getFlashTilt()));
        skyPoseStack.mulPose(Axis.ZP.rotationDegrees(ChaosFlashClientHandler.getFlashRoll()));

        Matrix4f flashMatrix = skyPoseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, CHAOS_FLASH);

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, intensity * 0.95F);
        drawCelestialQuad(flashMatrix, ChaosFlashClientHandler.getFlashSize(), FLASH_DISTANCE);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        skyPoseStack.popPose();
    }

    private static void drawMoon(PoseStack skyPoseStack) {
        skyPoseStack.pushPose();


        skyPoseStack.mulPose(Axis.YP.rotationDegrees(MOON_AZIMUTH));
        skyPoseStack.mulPose(Axis.XP.rotationDegrees(MOON_TILT_FROM_ZENITH));

        Matrix4f moonMatrix = skyPoseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, getCurrentMoonTexture());

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        drawCelestialQuad(moonMatrix, MOON_SIZE, MOON_DISTANCE);
        RenderSystem.defaultBlendFunc();
        skyPoseStack.popPose();
    }

    private static ResourceLocation getCurrentMoonTexture() {
        // sad :(
        if (ChaosDimensionClientEvents.isBossArenaDefeated()) {

            return MOON_SAD;
        }
        return switch (ChaosDimensionClientEvents.getMoonStage()) {

            case 1 -> MOON_1;
            case 2 -> MOON_2;
            case 3 -> MOON_3;
            case 4 -> MOON_4;
            case 5 -> MOON_5;
            default -> MOON_6;
        };
    }

    private static void drawCelestialQuad(Matrix4f matrix, float size, float distance) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);


        builder.vertex(matrix, -size, distance, -size).uv(0.0F, 0.0F).endVertex();
        builder.vertex(matrix, size, distance, -size).uv(1.0F, 0.0F).endVertex();
        builder.vertex(matrix, size, distance, size).uv(1.0F, 1.0F).endVertex();
        builder.vertex(matrix, -size, distance, size).uv(0.0F, 1.0F).endVertex();

        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawVoidBottom(PoseStack skyPoseStack, float size) {
        Matrix4f voidMatrix = skyPoseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addColorQuad(builder, voidMatrix, -size, -size, size, size, -size, size, size, -size, -size, -size, -size, -size, 0.0F, 0.0F, 0.0F, 0.82F);
        addGradientSide(builder, voidMatrix, -size, 0.0F, -size, size, 0.0F, -size, size, -size, -size, -size, -size, -size);
        addGradientSide(builder, voidMatrix, size, 0.0F, size, -size, 0.0F, size, -size, -size, size, size, -size, size);
        addGradientSide(builder, voidMatrix, -size, 0.0F, size, -size, 0.0F, -size, -size, -size, -size, -size, -size, size);
        addGradientSide(builder, voidMatrix, size, 0.0F, -size, size, 0.0F, size, size, -size, size, size, -size, -size);

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void addGradientSide(BufferBuilder builder, Matrix4f matrix, float topX1, float topY1, float topZ1, float topX2, float topY2, float topZ2, float bottomX2, float bottomY2, float bottomZ2, float bottomX1, float bottomY1, float bottomZ1) {

        builder.vertex(matrix, topX1, topY1, topZ1).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
        builder.vertex(matrix, topX2, topY2, topZ2).color(0.0F, 0.0F, 0.0F, 0.0F).endVertex();
        builder.vertex(matrix, bottomX2, bottomY2, bottomZ2).color(0.0F, 0.0F, 0.0F, 0.82F).endVertex();
        builder.vertex(matrix, bottomX1, bottomY1, bottomZ1).color(0.0F, 0.0F, 0.0F, 0.82F).endVertex();
    }

    private static void addColorQuad(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float red, float green, float blue, float alpha) {

        builder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha).endVertex();
        builder.vertex(matrix, x4, y4, z4).color(red, green, blue, alpha).endVertex();
    }


    private static void drawCube(Matrix4f matrix, float size) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // north
        addQuad(builder, matrix, -size, -size, -size, size, -size, -size, size, size, -size, -size, size, -size);
        // south
        addQuad(builder, matrix, size, -size, size, -size, -size, size, -size, size, size, size, size, size);
        // west
        addQuad(builder, matrix, -size, -size, size, -size, -size, -size, -size, size, -size, -size, size, size);
        // east
        addQuad(builder, matrix, size, -size, -size, size, -size, size, size, size, size, size, size, -size);
        // up
        addQuad(builder, matrix, -size, size, -size, size, size, -size, size, size, size, -size, size, size);
        // down
        addQuad(builder, matrix, -size, -size, size, size, -size, size, size, -size, -size, -size, -size, -size);

        BufferUploader.drawWithShader(builder.end());
    }

    private static void addQuad(BufferBuilder builder, Matrix4f matrix,

                                float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        builder.vertex(matrix, x1, y1, z1).uv(0.0F, 0.0F).endVertex();

        builder.vertex(matrix, x2, y2, z2).uv(1.0F, 0.0F).endVertex();

        builder.vertex(matrix, x3, y3, z3).uv(1.0F, 1.0F).endVertex();

        builder.vertex(matrix, x4, y4, z4).uv(0.0F, 1.0F).endVertex();
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/environment/" + name + ".png");
    }
}