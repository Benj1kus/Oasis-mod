package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.CrusaderScienceEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

import java.io.IOException;

public final class ScienceShieldRenderer {
    private static ShaderInstance shader;
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(262144));
    private static final Vec3[][] MESH = makeMesh();
    private static boolean warnedMissingShader;

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        shader = null;
        warnedMissingShader = false;
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "science_jelly"), DefaultVertexFormat.POSITION_COLOR_TEX), value -> shader = value);
    }

    public static void render(CrusaderScienceEntity entity, float partial, PoseStack poses, MultiBufferSource entityBuffers) {
        if (!entity.isAlive() || entity.isInvisible() || visibility(entity, partial) <= .001F) return;
        if (entityBuffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
        var previous = RenderSystem.getShader();
        try {
            if (shader == null) {
                if (!warnedMissingShader) {
                    warnedMissingShader = true;
                    com.mojang.logging.LogUtils.getLogger().warn("meh");
                }
                fallback(entity, partial, poses.last().pose());
                return;
            }
            shader.safeGetUniform("ShieldView").set(poses.last().pose());
            shader.safeGetUniform("ShieldProjection").set(RenderSystem.getProjectionMatrix());
            draw(entity, partial);
        } finally {
            BUFFER.endBatch(JellyType.TYPE);
            BUFFER.endBatch(JellyType.FALLBACK);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void fallback(CrusaderScienceEntity entity, float partial, Matrix4f matrix) {
        Vec3 r = entity.shieldRadii();
        float time = (entity.tickCount + partial) * .05F;
        int alpha = (int) (visibility(entity, partial) * 85);
        VertexConsumer out = BUFFER.getBuffer(JellyType.FALLBACK);
        for (Vec3[] face : MESH)
            for (Vec3 p : face) {
                double wave = 1 + .02 * Math.sin(p.x * 5 + time) * Math.cos(p.z * 4 - time);
                out.vertex(matrix, (float) (p.x * r.x * wave), (float) (r.y - .04 + p.y * r.y * wave), (float) (p.z * r.z * wave)).color(108, 32, 173, alpha).endVertex();
            }
        BUFFER.endBatch(JellyType.FALLBACK);
    }

    private static float visibility(CrusaderScienceEntity entity, float partial) {
        if (!entity.isProvoked()) return 0;
        float age = Math.max(0, (float) (entity.level().getGameTime() - entity.shieldChangedAt()) + partial);
        float t = Mth.clamp(age / 12F, 0, 1);
        float smooth = t * t * (3 - 2 * t);
        float envelope = entity.isShieldActive() ? .2F + .8F * smooth : 1 - smooth;
        float flicker = t < 1 ? (.38F + .62F * (Math.sin(age * 2.7) > -.15 ? 1F : .1F)) : 1F;
        return envelope * flicker;
    }

    private static void draw(CrusaderScienceEntity entity, float partial) {
        Vec3 radii = entity.shieldRadii();
        shader.safeGetUniform("Center").set(0F, (float) (radii.y - .04), 0F);
        shader.safeGetUniform("Radii").set((float) radii.x, (float) radii.y, (float) radii.z);
        shader.safeGetUniform("Time").set(((entity.level().getGameTime() % 24000) + partial) / 20F);
        shader.safeGetUniform("Seed").set((float) (entity.getId() & 1023));
        shader.safeGetUniform("Amount").set(visibility(entity, partial));
        int i = 0;
        for (var hit : ScienceCombatEffects.hits(entity.getUUID())) {
            float age = (entity.level().getGameTime() - hit.start() + partial) / 20F;
            Vec3 n = hit.normal();
            shader.safeGetUniform("Hit" + i++).set((float) n.x, (float) n.y, (float) n.z, age);
            if (i == 4) break;
        }
        while (i < 4) shader.safeGetUniform("Hit" + i++).set(0F, 0F, 1F, -10F);
        for (int layer = 2; layer >= 0; layer--) {
            shader.safeGetUniform("Layer").set((float) layer);
            VertexConsumer out = BUFFER.getBuffer(JellyType.TYPE);
            for (Vec3[] face : MESH)
                for (Vec3 p : face)
                    out.vertex(p.x, p.y, p.z).color(255, 255, 255, 255).uv(0, 0).endVertex();
            BUFFER.endBatch(JellyType.TYPE);
        }
    }

    private static Vec3[][] makeMesh() {
        int sides = 16, rows = 10;
        Vec3[][] mesh = new Vec3[sides * rows][4];
        int i = 0;
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < sides; x++) {
                mesh[i++] = new Vec3[]{point(x, y, sides, rows), point(x + 1, y, sides, rows), point(x + 1, y + 1, sides, rows), point(x, y + 1, sides, rows)};
            }
        return mesh;
    }

    private static Vec3 point(int x, int y, int sides, int rows) {
        double phi = Math.PI * y / rows, theta = Math.PI * 2 * x / sides;
        return new Vec3(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta));
    }

    private static final class JellyType extends RenderType {
        private static final RenderType TYPE = create("oasiso_science_jelly", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 262144, false, false, CompositeState.builder().setShaderState(new ShaderStateShard(() -> ScienceShieldRenderer.shader)).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private static final RenderType FALLBACK = create("oasiso_science_jelly_fallback", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorShader)).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private JellyType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }

    private ScienceShieldRenderer() {
    }
}
