package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.model.ApolWingsModel; // Убедитесь, что модель называется так
import com.benji.oasiso.common.item.ApolWingsArmorItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.ArrayList;

public class ApolWingsRenderer extends GeoArmorRenderer<ApolWingsArmorItem> {

    public static final float OUTLINE_OPACITY = 0.95F;
    public static final double OUTLINE_DISTANCE = 64.0;
    private static final int[] TRIANGLES = {0, 1, 2, 0, 2, 3};

    private final MultiBufferSource.BufferSource ownBuffers = MultiBufferSource.immediate(new BufferBuilder(131072));
    private final ArrayList<float[]> mesh = new ArrayList<>();
    private Matrix4f clipMatrix;
    private boolean capture, crossesCamera;
    private float minX, minY, maxX, maxY;

    public ApolWingsRenderer() {
        super(new ApolWingsModel());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        mesh.clear();
        minX = minY = Float.POSITIVE_INFINITY;
        maxX = maxY = Float.NEGATIVE_INFINITY;
        crossesCamera = false;
        clipMatrix = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());

        try {
            VertexConsumer ownBuffer = ownBuffers.getBuffer(RenderType.armorCutoutNoCull(getTextureLocation(this.animatable)));
            super.renderToBuffer(poseStack, ownBuffer, packedLight, packedOverlay, red, green, blue, alpha);
        } finally {
            capture = false;
            ownBuffers.endBatch();
        }

        if (mesh.isEmpty() || !ApollyonShaders.ready() || this.getCurrentEntity() == null) return;

        int texture = Minecraft.getInstance().getTextureManager().getTexture(getTextureLocation(this.animatable)).getId();
        float partial = Minecraft.getInstance().getFrameTime();

        float time = (this.getCurrentEntity().tickCount + partial) / 20F;
        time += (this.getCurrentEntity().getId() & 255) * 1.37F;

        ApollyonOutlinePass.draw(mesh, texture, minX, minY, maxX, maxY, crossesCamera, time, OUTLINE_OPACITY);
        mesh.clear();
    }

    @Override
    public void actuallyRender(PoseStack pose, ApolWingsArmorItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource source, VertexConsumer vertices, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean previous = capture;

        capture = !isReRender && ApollyonShaders.ready() && this.getCurrentEntity() != null && this.getCurrentEntity().position().distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) <= OUTLINE_DISTANCE * OUTLINE_DISTANCE;

        try {
            super.actuallyRender(pose, animatable, model, type, source, vertices, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
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
            float x = clip.x() / clip.w();
            float y = clip.y() / clip.w();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        for (int index : TRIANGLES) mesh.add(corners[index]);
    }
}