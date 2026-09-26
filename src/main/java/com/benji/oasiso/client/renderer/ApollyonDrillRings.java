package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import com.benji.oasiso.common.entity.ai.ApollyonAttackTimeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApollyonDrillRings {
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(4096));
    private static Matrix4f view, projection;
    private static ClientLevel world;
    private static Vec3 camera;

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == world) {
            world = null;
            camera = null;
            view = projection = null;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            world = mc.level;
            camera = event.getCamera().getPosition();
            view = new Matrix4f(event.getPoseStack().last().pose());
            projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f matrix = view, proj = projection;
        view = projection = null;
        var shader = ApollyonDrillShaders.shader;
        if (shader == null || world == null || mc.level != world || matrix == null || proj == null || camera == null)
            return;
        float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
        var previous = RenderSystem.getShader();
        int drawn = 0;
        try {
            for (var entity : world.entitiesForRendering()) {
                if (!(entity instanceof ApollyonEntity boss) || boss.isRemoved() || boss.isInvisible()) continue;
                if (!boss.isDrilling()) continue;
                float age = boss.getDrillAge(partial);
                if (age < 0 || age >= ApollyonAttackTimeline.DRILL_END) continue;
                Vec3 origin = new Vec3(Mth.lerp(partial, boss.xo, boss.getX()), Mth.lerp(partial, boss.yo, boss.getY()) + .9, Mth.lerp(partial, boss.zo, boss.getZ()));
                if (origin.distanceToSqr(camera) > 80 * 80) continue;
                if (drawn++ >= 24) break;
                Vec3 axis = boss.getDrillAxis().normalize();
                if (axis.lengthSqr() < 1E-8) axis = new Vec3(0, 0, 1);
                Vec3 right = axis.cross(new Vec3(0, 1, 0));
                if (right.lengthSqr() < 1E-8) right = new Vec3(1, 0, 0);
                right = right.normalize();
                Vec3 up = right.cross(axis).normalize();
                shader.safeGetUniform("WaveProjection").set(proj);
                shader.safeGetUniform("Time").set(age / 20F);
                float fade = Mth.clamp(age / 6F, 0, 1) * Mth.clamp((ApollyonAttackTimeline.DRILL_END - age) / 4F, 0, 1);
                int[][] colors = {{70, 235, 255}, {255, 104, 212}, {153, 72, 255}};
                for (int ring = 2; ring >= 0; ring--) {

                    double size = .95 + ring * .35;
                    Vec3 center = origin.add(axis.scale(.8 - ring * .75));
                    Vec3 x = right.scale(size), y = up.scale(size);
                    shader.safeGetUniform("Seed").set((float) ((boss.getId() & 255) + ring * 17));
                    shader.safeGetUniform("Spin").set(ring == 1 ? -1F : 1F);
                    VertexConsumer out = BUFFER.getBuffer(WaveType.TYPE);
                    int[] rgb = colors[ring];
                    int alpha = Math.round(255 * fade);
                    vertex(out, matrix, center.subtract(x).subtract(y), 0, 0, rgb, alpha);
                    vertex(out, matrix, center.add(x).subtract(y), 1, 0, rgb, alpha);
                    vertex(out, matrix, center.add(x).add(y), 1, 1, rgb, alpha);
                    vertex(out, matrix, center.subtract(x).add(y), 0, 1, rgb, alpha);
                    BUFFER.endBatch(WaveType.TYPE);
                }
            }
        } finally {
            BUFFER.endBatch(WaveType.TYPE);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, float u, float v, int[] rgb, int alpha) {
        out.vertex(matrix, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)).color(rgb[0], rgb[1], rgb[2], alpha).uv(u, v).endVertex();
    }

    private static final class WaveType extends RenderType {
        static final RenderType TYPE = create("oasiso_apollyon_drill", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 4096, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(() -> ApollyonDrillShaders.shader)).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private WaveType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }

    private ApollyonDrillRings() {
    }
}
