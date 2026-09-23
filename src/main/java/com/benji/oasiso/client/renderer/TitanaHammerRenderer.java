package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.TitanaHammerModel;
import com.benji.oasiso.common.item.TitanaHammerItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.ArrayList;

public final class TitanaHammerRenderer extends GeoItemRenderer<TitanaHammerItem> {
    public static final float OUTLINE_WIDTH = 4.8F;
    public static final float FADE_START = 5F;
    public static final float FADE_END = 36F;
    public static final float OPACITY = 0.95F;

    private final MultiBufferSource.BufferSource itemBuffers = MultiBufferSource.immediate(new BufferBuilder(65536));
    private final ArrayList<float[]> triangles = new ArrayList<>();
    private boolean capture;
    private Matrix4f clipMatrix;
    private float minX, minY, maxX, maxY;
    private boolean crossesCamera;

    public TitanaHammerRenderer() {
        super(new TitanaHammerModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource source, int light, int overlay) {
        super.renderByItem(stack, context, pose, itemBuffers, light, overlay);
        itemBuffers.endBatch();
    }

    @Override
    public void actuallyRender(PoseStack pose, TitanaHammerItem item, BakedGeoModel model, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean reRender, float partialTick, int light, int overlay, float red, float green, float blue, float alpha) {
        super.actuallyRender(pose, item, model, type, source, vertices, reRender, partialTick, light, overlay, red, green, blue, alpha);
        if (reRender || source != itemBuffers || TitanaHammerShaders.mask == null || TitanaHammerShaders.outline == null)
            return;
        itemBuffers.endBatch();

        triangles.clear();
        clipMatrix = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        minX = minY = Float.POSITIVE_INFINITY;
        maxX = maxY = Float.NEGATIVE_INFINITY;
        crossesCamera = false;
        try {
            capture = true;
            super.actuallyRender(pose, item, model, type, source, vertices, true, partialTick, light, overlay, 1, 1, 1, 1);
        } finally {
            capture = false;
        }
        if (triangles.isEmpty()) return;
        int texture = net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(getTextureLocation(item)).getId();
        TitanaHammerOutlinePass.draw(triangles, texture, new Matrix4f(pose.last().pose()).invert(), minX, minY, maxX, maxY, crossesCamera, alpha);
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (!capture) {
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

    static void releaseMask() {
        TitanaHammerOutlinePass.release();
    }
}
