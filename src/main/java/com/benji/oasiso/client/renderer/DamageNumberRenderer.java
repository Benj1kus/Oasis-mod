package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.DamageNumberEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DamageNumberRenderer extends EntityRenderer<DamageNumberEntity> {

    /*
     * =========================================================
     * Digit textures
     * =========================================================
     */

    private static final ResourceLocation[] DIGIT_TEXTURES = new ResourceLocation[]{

            texture(0),
            texture(1),
            texture(2),
            texture(3),
            texture(4),
            texture(5),
            texture(6),
            texture(7),
            texture(8),
            texture(9)};

    private static final float DIGIT_SIZE_PIXELS = 8.0F;
    private static final float DIGIT_STEP_PIXELS = 6.0F;
    private static final float WORLD_SCALE = 0.045F;

    public DamageNumberRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(DamageNumberEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        int damage = entity.getDamageValue();
        String text = Integer.toString(damage);

        if (text.isEmpty()) {
            return;
        }

        float alpha = entity.getRenderAlpha(partialTick);

        if (alpha <= 0.001F) {
            return;
        }

        float popScale = entity.getRenderScale(partialTick);
        float totalWidth = DIGIT_SIZE_PIXELS;

        if (text.length() > 1) {
            totalWidth += (text.length() - 1) * DIGIT_STEP_PIXELS;
        }

        float startX = -totalWidth * 0.5F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float scale = WORLD_SCALE * popScale;

        poseStack.scale(-scale, -scale, scale);

        int alphaByte = Mth.clamp((int) (alpha * 255.0F), 0, 255);

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int digit = character - '0';

            if (digit < 0 || digit > 9) {
                continue;
            }

            float left = startX + i * DIGIT_STEP_PIXELS;
            float right = left + DIGIT_SIZE_PIXELS;
            float top = -DIGIT_SIZE_PIXELS * 0.5F;
            float bottom = DIGIT_SIZE_PIXELS * 0.5F;

            RenderType renderType = RenderType.entityTranslucent(DIGIT_TEXTURES[digit]);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            drawDigit(poseStack, consumer, left, right, top, bottom, alphaByte);
        }
        poseStack.popPose();
    }

    private void drawDigit(PoseStack poseStack, VertexConsumer consumer, float left, float right, float top, float bottom, int alpha) {
        PoseStack.Pose pose = poseStack.last();

        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();


        // Bottom-left
        consumer.vertex(matrix, left, bottom, 0.0F).color(255, 255, 255, alpha).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        //Bottom-right.
        consumer.vertex(matrix, right, bottom, 0.0F).color(255, 255, 255, alpha).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        //Top-right.
        consumer.vertex(matrix, right, top, 0.0F).color(255, 255, 255, alpha).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        // Top-left.
        consumer.vertex(matrix, left, top, 0.0F).color(255, 255, 255, alpha).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DamageNumberEntity entity) {
        return DIGIT_TEXTURES[0];
    }

    private static ResourceLocation texture(int digit) {
        return ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "textures/entity/" + "damage_numbers/" + digit + ".png");
    }
}