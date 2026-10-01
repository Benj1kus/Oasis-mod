package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.ApolSpearModel;
import com.benji.oasiso.common.item.ApolSpearItem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class ApolSpearRenderer extends GeoItemRenderer<ApolSpearItem> {
    private final MultiBufferSource.BufferSource ownBuffers = MultiBufferSource.immediate(new BufferBuilder(32768));
    private Matrix4f tipPose;
    private boolean capture;

    public ApolSpearRenderer() {
        super(new ApolSpearModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource source, int light, int overlay) {
        boolean held = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (!held) {
            super.renderByItem(stack, context, pose, source, light, overlay);
            return;
        }
        capture = true;
        tipPose = null;
        try {
            super.renderByItem(stack, context, pose, ownBuffers, light, overlay);
            ownBuffers.endBatch();
            if (tipPose != null)
                ApollyonSpearTrail.renderItem(GeoItem.getId(stack), context.ordinal(), tipPose, Minecraft.getInstance().getFrameTime());
        } finally {
            capture = false;
            tipPose = null;
            ownBuffers.endBatch();
        }
    }

    @Override
    public void renderRecursively(PoseStack pose, ApolSpearItem item, GeoBone bone, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean reRender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        if (capture && !reRender && "aspear_top".equals(bone.getName())) {
            pose.pushPose();
            try {
                RenderUtils.translateMatrixToBone(pose, bone);
                RenderUtils.translateToPivotPoint(pose, bone);
                RenderUtils.rotateMatrixAroundBone(pose, bone);
                RenderUtils.scaleMatrixForBone(pose, bone);
                tipPose = new Matrix4f(pose.last().pose());
            } finally {
                pose.popPose();
            }
        }
        super.renderRecursively(pose, item, bone, type, source, vertices, reRender, partial, light, overlay, red, green, blue, alpha);
    }
}
