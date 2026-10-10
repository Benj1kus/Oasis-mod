package com.benji.oasiso.client.geyser;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
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

import java.io.IOException;
import java.util.Comparator;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class BigGeyserJetRenderer {
    private static ShaderInstance shader;
    private static final BufferBuilder BUILDER = new BufferBuilder(262144);
    private static Matrix4f frameView, frameProjection;
    private static Vec3 frameCamera;
    private static ClientLevel frameLevel;

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
        if (shader == null || level == null || mc.level != level || view == null || projection == null || camera == null)
            return;
        var jets = BigGeyserClient.jets();
        jets.removeIf(j -> j.base().distanceToSqr(camera) >= BigGeyserClient.RANGE * BigGeyserClient.RANGE || !event.getFrustum().isVisible(new AABB(j.base().x - .7, j.base().y, j.base().z - .7, j.base().x + .7, j.base().y + j.height() + .1, j.base().z + .7)));
        if (jets.isEmpty()) return;
        jets.sort(Comparator.comparingDouble((BigGeyserClient.Jet j) -> j.base().distanceToSqr(camera)).reversed());
        shader.safeGetUniform("JetViewProjection").set(new Matrix4f(projection).mul(view));
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        ShaderInstance previous = RenderSystem.getShader();

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> shader);
            float partial = mc.isPaused() ? mc.getFrameTime() : event.getPartialTick();
            for (var jet : jets) {
                float age = (level.getGameTime() - jet.start() + partial) / 20F;
                shader.safeGetUniform("Age").set(age);
                shader.safeGetUniform("Seed").set((float) jet.seed());
                shader.safeGetUniform("DistanceFade").set((float) Math.max(0, Math.min(1, (32 - jet.base().distanceTo(camera)) / 4)));
                Vec3 origin = jet.base().subtract(camera);
                shader.safeGetUniform("ShellViewAngle").set((float) Math.atan2(-origin.z, -origin.x));
                var faces = BigGeyserGeometry.build(age, jet.seed(), jet.height());
                if (faces.isEmpty()) continue;
                faces.sort(Comparator.comparingDouble((BigGeyserGeometry.Face f) -> f.distanceSquared(origin.x, origin.y, origin.z)).reversed());
                BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                for (var face : faces)
                    for (var p : face.points())
                        BUILDER.vertex(origin.x + p.x(), origin.y + p.y(), origin.z + p.z()).color(face.layer(), 0F, 0F, 1F).uv(p.u(), p.v()).endVertex();
                BufferUploader.drawWithShader(BUILDER.end());
            }
        } finally {
            RenderSystem.depthMask(mask);
            RenderSystem.depthFunc(func);
            if (depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (previous != null) RenderSystem.setShader(() -> previous);
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            shader = null;
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "big_geyser_jet"), DefaultVertexFormat.POSITION_COLOR_TEX), loaded -> shader = loaded);
        }
    }

    private BigGeyserJetRenderer() {
    }
}
