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

    private static final ResourceLocation SKY_BACK =
            texture("thread_sky_back");

    private static final ResourceLocation SKY_LAYER_2 =
            texture("thread_sky_layer2");

    private static final ResourceLocation SKY_FRONT =
            texture("thread_sky_front");

    private static final ResourceLocation MOON_1 =
            texture("chaos_moon");

    private static final ResourceLocation MOON_2 =
            texture("chaos_moon2");

    private static final ResourceLocation MOON_3 =
            texture("chaos_moon3");

    private static final ResourceLocation MOON_4 =
            texture("chaos_moon4");

    private static final ResourceLocation MOON_5 =
            texture("chaos_moon5");

    private static final ResourceLocation MOON_6 =
            texture("chaos_moon6");

    private static final float SKY_SIZE = 100.0F;

    private ChaosSkyRenderer() {
    }

    public static void render(
            PoseStack skyPoseStack,
            int ticks,
            float partialTick
    ) {
        float time =
                ticks + partialTick;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(
                GameRenderer::getPositionTexShader
        );


        drawSkyLayer(
                SKY_BACK,
                0.0F,
                1.0F
        );


        drawSkyLayer(
                SKY_LAYER_2,
                -time * 0.006F,
                0.9F
        );


        drawSkyLayer(
                SKY_FRONT,
                time * 0.01F,
                0.95F
        );

        drawVoidBottom(SKY_SIZE);

        drawMoon(skyPoseStack);

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawSkyLayer(
            ResourceLocation texture,
            float rotation,
            float alpha
    ) {
        PoseStack modelView =
                RenderSystem.getModelViewStack();

        modelView.pushPose();

        modelView.mulPose(
                Axis.YP.rotationDegrees(rotation)
        );

        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShaderTexture(
                0,
                texture
        );

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                alpha
        );

        drawCube(SKY_SIZE);

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void drawMoon(
            PoseStack skyPoseStack
    ) {
        skyPoseStack.pushPose();


        skyPoseStack.mulPose(
                Axis.YP.rotationDegrees(
                        MOON_AZIMUTH
                )
        );

        skyPoseStack.mulPose(
                Axis.XP.rotationDegrees(
                        MOON_TILT_FROM_ZENITH
                )
        );

        Matrix4f moonMatrix =
                skyPoseStack.last().pose();

        RenderSystem.setShader(
                GameRenderer::getPositionTexShader
        );

        RenderSystem.setShaderTexture(
                0,
                getCurrentMoonTexture()
        );


        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE
        );

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        drawMoonQuad(
                moonMatrix,
                MOON_SIZE,
                MOON_DISTANCE
        );

        RenderSystem.defaultBlendFunc();

        skyPoseStack.popPose();
    }

    private static ResourceLocation getCurrentMoonTexture() {
        return switch (
                ChaosDimensionClientEvents.getMoonStage()
                ) {
            case 1 -> MOON_1;
            case 2 -> MOON_2;
            case 3 -> MOON_3;
            case 4 -> MOON_4;
            case 5 -> MOON_5;
            default -> MOON_6;
        };
    }

    private static void drawMoonQuad(
            Matrix4f matrix,
            float size,
            float distance
    ) {
        BufferBuilder builder =
                Tesselator.getInstance()
                        .getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX
        );


        builder.vertex(
                matrix,
                -size,
                distance,
                -size
        ).uv(
                0.0F,
                0.0F
        ).endVertex();

        builder.vertex(
                matrix,
                size,
                distance,
                -size
        ).uv(
                1.0F,
                0.0F
        ).endVertex();

        builder.vertex(
                matrix,
                size,
                distance,
                size
        ).uv(
                1.0F,
                1.0F
        ).endVertex();

        builder.vertex(
                matrix,
                -size,
                distance,
                size
        ).uv(
                0.0F,
                1.0F
        ).endVertex();

        BufferUploader.drawWithShader(
                builder.end()
        );
    }

    private static void drawVoidBottom(
            float size
    ) {
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        RenderSystem.defaultBlendFunc();

        BufferBuilder builder =
                Tesselator.getInstance()
                        .getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );


        addColorQuad(
                builder,

                -size, -size, size,
                size, -size, size,
                size, -size, -size,
                -size, -size, -size,

                0.0F,
                0.0F,
                0.0F,
                0.82F
        );


        addGradientSide(
                builder,

                -size, 0.0F, -size,
                size, 0.0F, -size,

                size, -size, -size,
                -size, -size, -size
        );


        addGradientSide(
                builder,

                size, 0.0F, size,
                -size, 0.0F, size,

                -size, -size, size,
                size, -size, size
        );


        addGradientSide(
                builder,

                -size, 0.0F, size,
                -size, 0.0F, -size,

                -size, -size, -size,
                -size, -size, size
        );


        addGradientSide(
                builder,

                size, 0.0F, -size,
                size, 0.0F, size,

                size, -size, size,
                size, -size, -size
        );

        BufferUploader.drawWithShader(
                builder.end()
        );

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }

    private static void addGradientSide(
            BufferBuilder builder,

            float topX1,
            float topY1,
            float topZ1,

            float topX2,
            float topY2,
            float topZ2,

            float bottomX2,
            float bottomY2,
            float bottomZ2,

            float bottomX1,
            float bottomY1,
            float bottomZ1
    ) {

        builder.vertex(
                topX1,
                topY1,
                topZ1
        ).color(
                0.0F,
                0.0F,
                0.0F,
                0.0F
        ).endVertex();

        builder.vertex(
                topX2,
                topY2,
                topZ2
        ).color(
                0.0F,
                0.0F,
                0.0F,
                0.0F
        ).endVertex();


        builder.vertex(
                bottomX2,
                bottomY2,
                bottomZ2
        ).color(
                0.0F,
                0.0F,
                0.0F,
                0.82F
        ).endVertex();

        builder.vertex(
                bottomX1,
                bottomY1,
                bottomZ1
        ).color(
                0.0F,
                0.0F,
                0.0F,
                0.82F
        ).endVertex();
    }

    private static void addColorQuad(
            BufferBuilder builder,

            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3,

            float x4,
            float y4,
            float z4,

            float red,
            float green,
            float blue,
            float alpha
    ) {
        builder.vertex(x1, y1, z1)
                .color(red, green, blue, alpha)
                .endVertex();

        builder.vertex(x2, y2, z2)
                .color(red, green, blue, alpha)
                .endVertex();

        builder.vertex(x3, y3, z3)
                .color(red, green, blue, alpha)
                .endVertex();

        builder.vertex(x4, y4, z4)
                .color(red, green, blue, alpha)
                .endVertex();
    }


    private static void drawCube(
            float size
    ) {
        BufferBuilder builder =
                Tesselator.getInstance()
                        .getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX
        );

        addQuad(
                builder,
                -size, -size, -size,
                size, -size, -size,
                size, size, -size,
                -size, size, -size
        );

        addQuad(
                builder,
                size, -size, size,
                -size, -size, size,
                -size, size, size,
                size, size, size
        );

        addQuad(
                builder,
                -size, -size, size,
                -size, -size, -size,
                -size, size, -size,
                -size, size, size
        );

        addQuad(
                builder,
                size, -size, -size,
                size, -size, size,
                size, size, size,
                size, size, -size
        );

        addQuad(
                builder,
                -size, size, -size,
                size, size, -size,
                size, size, size,
                -size, size, size
        );

        addQuad(
                builder,
                -size, -size, size,
                size, -size, size,
                size, -size, -size,
                -size, -size, -size
        );

        BufferUploader.drawWithShader(
                builder.end()
        );
    }

    private static void addQuad(
            BufferBuilder builder,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4
    ) {
        builder.vertex(
                x1, y1, z1
        ).uv(0.0F, 0.0F).endVertex();

        builder.vertex(
                x2, y2, z2
        ).uv(1.0F, 0.0F).endVertex();

        builder.vertex(
                x3, y3, z3
        ).uv(1.0F, 1.0F).endVertex();

        builder.vertex(
                x4, y4, z4
        ).uv(0.0F, 1.0F).endVertex();
    }

    private static ResourceLocation texture(
            String name
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                Oasiso.MODID,
                "textures/environment/"
                        + name
                        + ".png"
        );
    }
}