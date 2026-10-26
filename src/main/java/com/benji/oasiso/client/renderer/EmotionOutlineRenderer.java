package com.benji.oasiso.client.renderer;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import net.minecraft.world.entity.Entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;

public class EmotionOutlineRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    public static final float OUTLINE_OPACITY = .95F;
    public static final double OUTLINE_DISTANCE = 48;
    private static final int[] TRIANGLES = {0, 1, 2, 0, 2, 3};
    private final int color;
    private final MultiBufferSource.BufferSource ownBuffers = MultiBufferSource.immediate(new BufferBuilder(131072));
    private final ArrayList<float[]> mesh = new ArrayList<>();
    private Matrix4f clipMatrix;
    private boolean capture, crossesCamera, externalCapture;
    private float minX, minY, maxX, maxY;

    public EmotionOutlineRenderer(EntityRendererProvider.Context context, GeoModel<T> model, int color) {
        super(context, model);
        this.color = color;
    }

    @Override
    public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource source, int light) {
        if (!(source instanceof MultiBufferSource.BufferSource)) {
            boolean previous = capture;
            capture = false;
            externalCapture = true;
            try {
                super.render(entity, yaw, partial, pose, source, light);
            } finally {
                capture = previous;
                externalCapture = false;
            }
            return;
        }
        mesh.clear();
        minX = minY = Float.POSITIVE_INFINITY;
        maxX = maxY = Float.NEGATIVE_INFINITY;
        crossesCamera = false;
        clipMatrix = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        var previousShader = RenderSystem.getShader();
        try {
            super.render(entity, yaw, partial, pose, ownBuffers, light);
        } finally {
            capture = false;
            ownBuffers.endBatch();
            if (previousShader != null) RenderSystem.setShader(() -> previousShader);
        }
        if (mesh.isEmpty() || !EmotionOutlineShaders.ready()) return;
        int texture = Minecraft.getInstance().getTextureManager().getTexture(getTextureLocation(entity)).getId();
        float time = (entity.tickCount + partial) / 20F + (entity.getId() & 255) * 1.37F;
        EmotionOutlinePass.draw(mesh, texture, minX, minY, maxX, maxY, crossesCamera, time, OUTLINE_OPACITY, color);
        mesh.clear();
    }

    @Override
    public void actuallyRender(PoseStack pose, T entity, BakedGeoModel model, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean reRender, float partial, int light, int overlay, float red, float green, float blue, float alpha) {
        boolean previous = capture;
        capture = !externalCapture && !reRender && !entity.isInvisible() && EmotionOutlineShaders.ready() && entity.position().distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) <= OUTLINE_DISTANCE * OUTLINE_DISTANCE;
        try {
            super.actuallyRender(pose, entity, model, type, source, vertices, reRender, partial, light, overlay, red, green, blue, alpha);
        } finally {
            capture = previous;
        }
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        super.createVerticesOfQuad(quad, pose, normal, buffer, light, overlay, red, green, blue, alpha);
        if (!capture) return;
        float[][] corners = new float[4][];
        for (int i = 0; i < 4; i++) {
            var vertex = quad.vertices()[i];
            Vector3f p = vertex.position();
            Vector4f transformed = pose.transform(new Vector4f(p.x(), p.y(), p.z(), 1));
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
        for (int index : TRIANGLES) mesh.add(corners[index]);
    }

}
