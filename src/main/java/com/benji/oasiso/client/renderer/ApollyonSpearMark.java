package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.ApollyonEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApollyonSpearMark {
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
        var shader = ApollyonSpearMarkShaders.shader;
        if (shader == null || world == null || mc.level != world || matrix == null || proj == null || camera == null)
            return;
        float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
        var previous = RenderSystem.getShader();
        int drawn = 0;
        try {
            for (var entity : world.entitiesForRendering()) {
                if (!(entity instanceof ApollyonEntity boss) || boss.isRemoved() || boss.isInvisible()) continue;
                if (!boss.isAlive() || boss.getSpearTargetId() < 0) continue;
                var target = world.getEntity(boss.getSpearTargetId());
                if (!(target instanceof Player player) || !player.isAlive()) continue;
                float age = (float) (world.getGameTime() - boss.getSpearMarkTime()) + partial;
                if (age < 0) continue;
                double x = Mth.lerp(partial, player.xo, player.getX());
                double y = Mth.lerp(partial, player.yo, player.getY());
                double z = Mth.lerp(partial, player.zo, player.getZ());
                var ground = world.clip(new ClipContext(new Vec3(x, y + .4, z), new Vec3(x, y - 8, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (ground.getType() != HitResult.Type.BLOCK) continue;
                Vec3 origin = new Vec3(x, ground.getLocation().y + .06, z);
                if (origin.distanceToSqr(camera) > 80 * 80) continue;
                if (drawn++ >= 24) break;
                shader.safeGetUniform("WaveProjection").set(proj);
                shader.safeGetUniform("Time").set(age / 20F);
                shader.safeGetUniform("Spin").set(0F);
                int[][] colors = {{70, 235, 255}, {255, 104, 212}, {153, 72, 255}};
                int alpha = Math.round(255 * Mth.clamp(age / 8F, 0, 1));
                for (int ring = 2; ring >= 0; ring--) {
                    // Разные фазы И скорости: кольца не расширяются одновременно.
                    double pulse = 1 + .12 * Math.sin(age * (.12 + ring * .027) + ring * 2.1);
                    double half = (1.05 + ring * .50) * pulse;
                    Vec3 center = origin.add(0, ring * .008, 0);
                    shader.safeGetUniform("Seed").set((float) ((boss.getId() & 255) + ring * 17));
                    VertexConsumer out = BUFFER.getBuffer(WaveType.TYPE);
                    int[] rgb = colors[ring];
                    vertex(out, matrix, center.add(-half, 0, -half), 0, 0, rgb, alpha);
                    vertex(out, matrix, center.add(half, 0, -half), 1, 0, rgb, alpha);
                    vertex(out, matrix, center.add(half, 0, half), 1, 1, rgb, alpha);
                    vertex(out, matrix, center.add(-half, 0, half), 0, 1, rgb, alpha);
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
        static final RenderType TYPE = create("oasiso_apollyon_spear_mark", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 4096, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(() -> ApollyonSpearMarkShaders.shader)).setCullState(NO_CULL).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private WaveType(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sorted, setup, clear);
        }
    }

    private ApollyonSpearMark() {
    }
}
