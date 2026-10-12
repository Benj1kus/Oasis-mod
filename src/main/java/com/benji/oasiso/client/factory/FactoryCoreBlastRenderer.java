package com.benji.oasiso.client.factory;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.util.*;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class FactoryCoreBlastRenderer {
    private static ShaderInstance shader;
    private static final BufferBuilder BUFFER = new BufferBuilder(2097152);
    private static Matrix4f frameView, frameProjection;
    private static Vec3 frameCamera;
    private static ClientLevel frameLevel;

    private record Effect(BlockPos pos, double age, boolean blast) {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            frameView = new Matrix4f(event.getPoseStack().last().pose());
            frameProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            frameCamera = event.getCamera().getPosition();
            frameLevel = mc.level;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f view = frameView, projection = frameProjection;
        Vec3 camera = frameCamera;
        ClientLevel level = frameLevel;
        frameView = frameProjection = null;
        frameCamera = null;
        frameLevel = null;
        if (shader == null || level == null || level != mc.level || camera == null || view == null) return;
        double time = level.getGameTime() + (mc.isPaused() ? mc.getFrameTime() : event.getPartialTick());
        List<Effect> effects = new ArrayList<>();
        for (var c : FactoryCoreBlastClient.charges()) {
            double age = (time - c.endTick() + 160) / 20.0;
            if (age >= 0 && age < 8 && event.getFrustum().isVisible(new AABB(c.pos()).inflate(5)))
                effects.add(new Effect(c.pos(), age, false));
        }
        for (var b : FactoryCoreBlastClient.blasts()) {
            double age = (time - b.tick) / 20.0;
            if (age >= 0 && age < 10 && event.getFrustum().isVisible(new AABB(b.pos.getX() - 30, b.pos.getY() - 4, b.pos.getZ() - 30, b.pos.getX() + 31, b.pos.getY() + 42, b.pos.getZ() + 31)))
                effects.add(new Effect(b.pos, age, true));
        }
        if (effects.isEmpty()) return;
        effects.sort(Comparator.comparingDouble(e -> Vec3.atCenterOf(e.pos).distanceToSqr(camera)));
        if (effects.size() > 8) effects.subList(8, effects.size()).clear();
        shader.safeGetUniform("BlastViewProjection").set(new Matrix4f(projection).mul(view));
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC), program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int eqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), eqAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        ShaderInstance previous = RenderSystem.getShader();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            for (Effect e : effects) {
                Vec3 origin = new Vec3(e.pos.getX() + .5, e.pos.getY(), e.pos.getZ() + .5), offset = origin.subtract(camera);
                boolean reduced = effects.size() > 2 || origin.distanceToSqr(camera) > 64 * 64;
                shader.safeGetUniform("Age").set((float) e.age);
                BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                FactoryCoreBlastGeometry.Sink sink = (x, y, z, p, k, h, l, u, v) -> BUFFER.vertex(x + offset.x, y + offset.y, z + offset.z).color(p, k, h, l).uv(u, v).endVertex();
                if (e.blast)
                    FactoryCoreBlastGeometry.blast(sink, e.age, e.pos.asLong(), -offset.x, -offset.y, -offset.z, reduced);
                else
                    FactoryCoreBlastGeometry.charge(sink, e.age, e.pos.asLong(), -offset.x, -offset.y, -offset.z, reduced);
                BufferUploader.drawWithShader(BUFFER.end());
            }
        } finally {
            RenderSystem.depthMask(mask);
            RenderSystem.depthFunc(func);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL20.glBlendEquationSeparate(eqRgb, eqAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
            GL20.glUseProgram(program);
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            FactoryCoreStreams.Shaders.register(event);

            shader = null;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "factory_core_blast"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> shader = loaded);
        }
    }

    private FactoryCoreBlastRenderer() {
    }
}
