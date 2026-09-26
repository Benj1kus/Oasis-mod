package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import com.benji.oasiso.common.entity.ai.ApollyonShockwaveAttack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApollyonShockwave {
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
        var shader = ApollyonShockwaveShaders.shader;
        if (shader == null || world == null || mc.level != world || matrix == null || proj == null || camera == null)
            return;
        float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
        var previous = RenderSystem.getShader();
        int drawn = 0;
        try {
            for (var entity : world.entitiesForRendering()) {
                if (!(entity instanceof ApollyonEntity boss) || boss.isRemoved() || boss.isInvisible()) continue;
                long start = boss.getShockwaveStart();
                if (start < 0) continue;
                double age = world.getGameTime() - start + partial;
                if (age < 0 || age >= ApollyonShockwaveAttack.WAVE_TICKS) continue;
                Vec3 origin = boss.getShockwaveOrigin();
                if (origin.distanceToSqr(camera) > 80 * 80) continue;
                if (drawn++ >= 24) break;
                float progress = (float) (age / ApollyonShockwaveAttack.WAVE_TICKS);
                shader.safeGetUniform("WaveProjection").set(proj);
                shader.safeGetUniform("Progress").set(progress);
                shader.safeGetUniform("Seed").set((float) (boss.getId() & 1023));
                double half = ApollyonShockwaveAttack.WAVE_RADIUS + .25;

                Vec3 center = origin.add(0, .85 * progress * progress, 0);
                VertexConsumer out = BUFFER.getBuffer(WaveType.TYPE);
                vertex(out, matrix, center.add(-half, 0, -half), 0, 0);
                vertex(out, matrix, center.add(half, 0, -half), 1, 0);
                vertex(out, matrix, center.add(half, 0, half), 1, 1);
                vertex(out, matrix, center.add(-half, 0, half), 0, 1);
                BUFFER.endBatch(WaveType.TYPE);
            }
        } finally {
            BUFFER.endBatch(WaveType.TYPE);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, float u, float v) {
        out.vertex(matrix, (float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z)).color(255, 255, 255, 255).uv(u, v).endVertex();
    }

    private static final class WaveType extends RenderType {
        static final RenderType TYPE = create("oasiso_apollyon_shockwave", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 4096, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(() -> ApollyonShockwaveShaders.shader)).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private WaveType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }

    private ApollyonShockwave() {
    }
}
