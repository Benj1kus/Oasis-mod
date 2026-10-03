package com.benji.oasiso.client.renderer;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class ApolSpearImpactFx {
    private record Flash(Vec3 pos, Vec3 normal, int seed, long time) {
    }

    private static final ArrayList<Flash> FLASHES = new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(4096));
    private static ClientLevel world;
    private static Matrix4f view, projection;
    private static ShaderInstance shader;

    private static void checkWorld() {
        var current = Minecraft.getInstance().level;
        if (world != current) {
            world = current;
            FLASHES.clear();
            view = projection = null;
        }
    }

    public static void add(Vec3 pos, Vec3 normal, int seed) {
        checkWorld();
        if (world == null) return;
        if (FLASHES.size() >= 64) FLASHES.remove(0);
        FLASHES.add(new Flash(pos, normal, seed, world.getGameTime()));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        checkWorld();
        if (world != null) FLASHES.removeIf(f -> world.getGameTime() - f.time() >= 12);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            checkWorld();
            view = new Matrix4f(e.getPoseStack().last().pose());
            projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            return;
        }
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f v = view, p = projection;
        view = projection = null;
        if (world == null || shader == null || v == null || p == null || FLASHES.isEmpty()) return;
        Vec3 eye = e.getCamera().getPosition();
        float partial = Minecraft.getInstance().isPaused() ? Minecraft.getInstance().getFrameTime() : e.getPartialTick();
        ShaderInstance previous = RenderSystem.getShader();
        shader.safeGetUniform("FlashProjection").set(p);
        try {
            for (Flash f : FLASHES) {
                double age = world.getGameTime() - f.time() + partial;
                if (age < 0 || age >= 12 || f.pos().distanceToSqr(eye) > 80 * 80) continue;
                shader.safeGetUniform("Progress").set((float) (age / 12));
                shader.safeGetUniform("Seed").set((float) (f.seed() & 255));
                Vec3 n = f.normal(), axis = Math.abs(n.y) > .7 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 right = n.cross(axis).normalize(), up = right.cross(n).normalize();
                double radius = 1.8 + 2.2 * Math.min(1, age / 4);
                VertexConsumer out = BUFFER.getBuffer(FlashType.TYPE);
                quad(out, v, eye, f.pos().add(n.scale(.025)), right.scale(radius), up.scale(radius));
                if (age < 7) {
                    Vec3 toEye = eye.subtract(f.pos()).normalize(), side = toEye.cross(new Vec3(0, 1, 0));
                    if (side.lengthSqr() > 1e-6)
                        quad(out, v, eye, f.pos().add(0, .42, 0), side.normalize().scale(.6), new Vec3(0, .85, 0));
                }
                BUFFER.endBatch(FlashType.TYPE);
            }
        } finally {
            BUFFER.endBatch(FlashType.TYPE);
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    private static void quad(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 c, Vec3 right, Vec3 up) {
        vertex(out, view, eye, c.subtract(right).subtract(up), 0, 0);
        vertex(out, view, eye, c.add(right).subtract(up), 1, 0);
        vertex(out, view, eye, c.add(right).add(up), 1, 1);
        vertex(out, view, eye, c.subtract(right).add(up), 0, 1);
    }

    private static void vertex(VertexConsumer out, Matrix4f view, Vec3 eye, Vec3 p, float u, float v) {
        out.vertex(view, (float) (p.x - eye.x), (float) (p.y - eye.y), (float) (p.z - eye.z)).uv(u, v).endVertex();
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent e) throws IOException {
            shader = null;
            FLASHES.clear();
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "apol_spear_impact"), DefaultVertexFormat.POSITION_TEX), s -> shader = s);
        }
    }

    private static final class FlashType extends RenderType {
        static final RenderType TYPE = create("oasiso_spear_impact", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 4096, false, true, CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));

        private FlashType(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean c, boolean sort, Runnable a, Runnable b) {
            super(n, f, m, s, c, sort, a, b);
        }
    }

    private ApolSpearImpactFx() {
    }
}
