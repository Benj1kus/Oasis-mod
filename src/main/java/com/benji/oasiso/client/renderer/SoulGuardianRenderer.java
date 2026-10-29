package com.benji.oasiso.client.renderer;

import com.benji.oasiso.client.layer.GlowmaskLayer;
import com.benji.oasiso.client.model.SoulGuardianModel;
import com.benji.oasiso.common.entity.SoulGuardianEntity;
import com.benji.oasiso.common.gravesoul.GraveSoulParticles;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.*;
import software.bernie.geckolib.cache.object.*;

import java.lang.Math;
import java.util.WeakHashMap;

public class SoulGuardianRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<SoulGuardianEntity> {
    private record Emission(int tick, float progress) {
    }

    private final WeakHashMap<SoulGuardianEntity, Emission> emissions = new WeakHashMap<>();
    private SoulGuardianEntity drawing;
    private Matrix4f rootInverse;
    private float progress, previousProgress;
    private boolean emit;
    private int faceIndex, emitted;

    public SoulGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulGuardianModel());
        shadowRadius = .3F;
        addRenderLayer(new GlowmaskLayer<SoulGuardianEntity>(this) {
            @Override
            public void render(PoseStack pose, SoulGuardianEntity entity, BakedGeoModel model, RenderType type, MultiBufferSource source, VertexConsumer buffer, float partial, int light, int overlay) {
                if (!entity.isDissolving())
                    super.render(pose, entity, model, type, source, buffer, partial, light, overlay);
            }
        });
    }

    @Override
    public void render(SoulGuardianEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource source, int light) {
        drawing = entity;
        faceIndex = emitted = 0;
        progress = entity.isDissolving() ? Math.min(1, (entity.getDissolveAge() + partial) / SoulGuardianEntity.DISSOLVE_TICKS) : 0;
        rootInverse = new Matrix4f(pose.last().pose()).invert();
        Emission last = emissions.get(entity);
        previousProgress = last == null ? 0 : last.progress;
        emit = entity.isDissolving() && source instanceof MultiBufferSource.BufferSource && (last == null || last.tick != entity.tickCount) && entity.position().distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) < 48 * 48;
        if (emit) emissions.put(entity, new Emission(entity.tickCount, progress));
        shadowRadius = entity.isDissolving() ? 0 : .3F;
        try {
            super.render(entity, yaw, partial, pose, source, entity.isDissolving() ? 0xF000F0 : light);
        } finally {
            drawing = null;
            emit = false;
        }
    }

    @Override
    protected float getDeathMaxRotation(SoulGuardianEntity entity) {
        return 0;
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (drawing == null || !drawing.isDissolving()) {
            super.createVerticesOfQuad(quad, pose, normal, buffer, light, overlay, red, green, blue, alpha);
            return;
        }
        var v = quad.vertices();
        int face = faceIndex++;
        int nx = Math.max(1, Math.min(32, (int) Math.ceil(v[0].position().distance(v[1].position()) * 16)));
        int ny = Math.max(1, Math.min(32, (int) Math.ceil(v[0].position().distance(v[3].position()) * 16)));
        for (int iy = 0; iy < ny; iy++)
            for (int ix = 0; ix < nx; ix++) {
                float cut = threshold(face, ix, iy, drawing.getId());
                float a = ix / (float) nx, b = iy / (float) ny, aa = (ix + 1F) / nx, bb = (iy + 1F) / ny;
                if (cut <= progress) {
                    if (emit && cut > previousProgress && emitted < 12) {
                        Vector3f center = point(quad, (a + aa) * .5F, (b + bb) * .5F);
                        pose.transformPosition(center);
                        rootInverse.transformPosition(center);
                        var random = drawing.getRandom();
                        drawing.level().addParticle(GraveSoulParticles.PIXEL, drawing.getX() + center.x, drawing.getY() + center.y, drawing.getZ() + center.z, (random.nextDouble() - .5) * .025, .025 + random.nextDouble() * .035, (random.nextDouble() - .5) * .025);
                        emitted++;
                    }
                    continue;
                }
                vertex(quad, pose, normal, buffer, a, b, red, green, blue, alpha);
                vertex(quad, pose, normal, buffer, aa, b, red, green, blue, alpha);
                vertex(quad, pose, normal, buffer, aa, bb, red, green, blue, alpha);
                vertex(quad, pose, normal, buffer, a, bb, red, green, blue, alpha);
            }
    }

    static float threshold(int face, int x, int y, int seed) {
        int h = face * 73428767 ^ x * 912931 ^ y * 19349663 ^ seed * 83492791;
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        h ^= h >>> 16;
        return ((h & 0xFFFFFF) + .5F) / 16777216F;
    }

    private static Vector3f point(GeoQuad quad, float a, float b) {
        var v = quad.vertices();
        return new Vector3f(v[0].position()).lerp(v[1].position(), a).lerp(new Vector3f(v[3].position()).lerp(v[2].position(), a), b);
    }

    private static void vertex(GeoQuad quad, Matrix4f pose, Vector3f n, VertexConsumer out, float a, float b, float r, float g, float blue, float alpha) {
        var v = quad.vertices();
        Vector3f p = point(quad, a, b);
        pose.transformPosition(p);
        float u = (v[0].texU() + (v[1].texU() - v[0].texU()) * a) * (1 - b) + (v[3].texU() + (v[2].texU() - v[3].texU()) * a) * b;
        float w = (v[0].texV() + (v[1].texV() - v[0].texV()) * a) * (1 - b) + (v[3].texV() + (v[2].texV() - v[3].texV()) * a) * b;
        out.vertex(p.x, p.y, p.z, r, g, blue, alpha, u, w, OverlayTexture.NO_OVERLAY, 0xF000F0, n.x, n.y, n.z);
    }
}
