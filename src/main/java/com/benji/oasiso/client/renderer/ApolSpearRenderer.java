package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.ApolSpearModel;
import com.benji.oasiso.common.item.ApolSpearItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;

public class ApolSpearRenderer extends GeoItemRenderer<ApolSpearItem> {
    public static final float OUTLINE_WIDTH = 4.8F;
    public static final float FADE_START = 5F;
    public static final float FADE_END = 36F;
    public static final float OPACITY = 0.95F;

    private final MultiBufferSource.BufferSource ownBuffers = MultiBufferSource.immediate(new BufferBuilder(32768));
    private final ArrayList<float[]> triangles = new ArrayList<>();

    private Matrix4f tipPose;
    private boolean captureTip;

    private boolean captureOutline;
    private Matrix4f clipMatrix;
    private float minX, minY, maxX, maxY;
    private boolean crossesCamera;

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

        captureTip = true;
        tipPose = null;
        try {
            super.renderByItem(stack, context, pose, ownBuffers, light, overlay);
            ownBuffers.endBatch(); // Это вызовет actuallyRender, где сработает обводка
            if (tipPose != null)
                ApollyonSpearTrail.renderItem(GeoItem.getId(stack), context.ordinal(), tipPose, Minecraft.getInstance().getFrameTime());
        } finally {
            captureTip = false;
            tipPose = null;
        }
    }

    @Override
    public void actuallyRender(PoseStack pose, ApolSpearItem item, BakedGeoModel model, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean reRender, float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        super.actuallyRender(pose, item, model, type, source, vertices, reRender, partialTick, light, overlay, red, green, blue, alpha);

        if (reRender || source != ownBuffers || ApolSpearShaders.mask == null || ApolSpearShaders.outline == null)
            return;

        ownBuffers.endBatch(); // Сбрасываем основной рендер перед обводкой

        triangles.clear();
        clipMatrix = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        minX = minY = Float.POSITIVE_INFINITY;
        maxX = maxY = Float.NEGATIVE_INFINITY;
        crossesCamera = false;

        try {
            captureOutline = true;
            super.actuallyRender(pose, item, model, type, source, vertices, true, partialTick, light, overlay, 1, 1, 1, 1);
        } finally {
            captureOutline = false;
        }

        if (triangles.isEmpty()) return;
        int texture = Minecraft.getInstance().getTextureManager().getTexture(getTextureLocation(item)).getId();
        ApolSpearOutlinePass.draw(triangles, texture, new Matrix4f(pose.last().pose()).invert(), minX, minY, maxX, maxY, crossesCamera, alpha);
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (!captureOutline) {
            super.createVerticesOfQuad(quad, poseState, normal, buffer, light, overlay, red, green, blue, alpha);
            return;
        }

        float[][] corners = new float[4][];
        for (int i = 0; i < 4; i++) {
            var vertex = quad.vertices()[i];
            Vector3f p = vertex.position();
            Vector4f transformed = poseState.transform(new Vector4f(p.x(), p.y(), p.z(), 1));
            corners[i] = new float[]{transformed.x(), transformed.y(), transformed.z(), vertex.texU(), vertex.texV()};
            Vector4f clip = clipMatrix.transform(new Vector4f(transformed));
            if (clip.w() <= 0.0001F) {
                crossesCamera = true;
                continue;
            }
            float x = clip.x() / clip.w(), y = clip.y() / clip.w();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        for (int index : new int[]{0, 1, 2, 0, 2, 3}) triangles.add(corners[index]);
    }

    @Override
    public void renderRecursively(PoseStack pose, ApolSpearItem item, GeoBone bone, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean reRender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        if (captureTip && !reRender && "aspear_top".equals(bone.getName())) {
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