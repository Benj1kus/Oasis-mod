package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.soul.SoulMesh;
import com.benji.oasiso.client.soul.SoulGraveEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import com.benji.oasiso.client.model.SoulGiverModel;
import com.benji.oasiso.common.entity.SoulGiverEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SoulGiverRenderer extends GeoEntityRenderer<SoulGiverEntity> {
    private SoulMesh capture;
    private boolean captureBone;
    private boolean sourceBone;

    public SoulGiverRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SoulGiverModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new GlowmaskLayer<>(this));
    }

    @Override
    public void render(SoulGiverEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        capture = !entity.isInvisible() && entity.isAlive() ? new SoulMesh() : null;
        try {
            super.render(entity, yaw, partial, poses, buffers, light);
            if (capture != null && !capture.vertices.isEmpty()) SoulGraveEffects.capture(entity, capture);
        } finally {
            capture = null;
            captureBone = false;
            sourceBone = false;
        }
    }

    @Override
    public void renderRecursively(PoseStack poses, SoulGiverEntity entity, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer out, boolean rerender, float partial, int light, int overlay, float r, float g, float b, float a) {
        boolean previous = captureBone, previousSource = sourceBone;
        captureBone = !rerender && (previous || bone.getName().equals("head"));
        sourceBone = bone.getName().equals("head");
        try {
            super.renderRecursively(poses, entity, bone, type, buffers, out, rerender, partial, light, overlay, r, g, b, a);
        } finally {
            captureBone = previous;
            sourceBone = previousSource;
        }
    }

    @Override
    public void renderCubesOfBone(PoseStack poses, GeoBone bone, VertexConsumer out, int light, int overlay, float r, float g, float b, float a) {
        super.renderCubesOfBone(poses, bone, capture != null && captureBone ? capture.capture(out, sourceBone) : out, light, overlay, r, g, b, a);
    }

    @Override
    public boolean shouldRender(SoulGiverEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}