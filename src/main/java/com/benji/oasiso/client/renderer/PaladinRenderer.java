package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.layer.PaladinOutlineMaskLayer;
import com.benji.oasiso.client.layer.PaladinQteHeartAnchorLayer;
import com.benji.oasiso.client.layer.PaladinShockwaveLayer;
import com.benji.oasiso.client.layer.PaladinSwordSlashLayer;
import com.benji.oasiso.client.model.PaladinModel;
import com.benji.oasiso.common.entity.PaladinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Set;

public class PaladinRenderer extends GeoEntityRenderer<PaladinEntity> {

    public enum OutlineMaskMode {
        NONE, GOLD, SWORD
    }

    private OutlineMaskMode outlineMaskMode = OutlineMaskMode.NONE;

    private static final Set<String> GOLD_OUTLINE_BONES = Set.of("Body", "cape", "cp", "cpp", "psi",
            "Head", "nimb", "head_wing_left", "head_wing_right",

            "Left Arm", "Right_Arm",

            "Left Leg", "Right Leg");


    private static final Set<String> SWORD_OUTLINE_BONES = Set.of("paladin sword");

    public PaladinRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PaladinModel());
        this.shadowRadius = 0.4F;

        addRenderLayer(new GlowmaskLayer<>(this));
        addRenderLayer(new PaladinOutlineMaskLayer(this));
        addRenderLayer(new PaladinSwordSlashLayer(this));
        addRenderLayer(new PaladinQteHeartAnchorLayer(this));
        addRenderLayer(new PaladinShockwaveLayer(this));
    }

    public void setOutlineMaskMode(OutlineMaskMode mode) {
        this.outlineMaskMode = mode;
    }

    public void clearOutlineMaskMode() {
        this.outlineMaskMode = OutlineMaskMode.NONE;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, PaladinEntity entity, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        if (this.outlineMaskMode != OutlineMaskMode.NONE && isReRender) {

            boolean visible = switch (this.outlineMaskMode) {
                case GOLD -> GOLD_OUTLINE_BONES.contains(bone.getName());
                case SWORD -> SWORD_OUTLINE_BONES.contains(bone.getName());

                default -> true;
            };

            float maskAlpha = visible ? 1.0F : 0.0F;
            super.renderRecursively(poseStack, entity, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, maskAlpha);

            return;
        }

        super.renderRecursively(poseStack, entity, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }


    @Override
    public boolean shouldRender(PaladinEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}