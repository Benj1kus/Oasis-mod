package com.benji.oasiso.client.cutscene;

import com.benji.oasiso.Oasiso;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;


@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT)
public final class CutsceneForeground {
    private static RenderTarget target;
    private static Matrix4f view, projection, modelView;
    private static Matrix3f normal;
    private static boolean capturing, ready;
    private static CutsceneScreen owner;
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(65536));

    public static RenderTarget captureTarget() {
        return capturing ? target : null;
    }

    public static boolean isCapturing() {
        return capturing;
    }


    public static boolean owns(Entity entity) {
        CutsceneScreen scene = CutsceneScreen.current();
        return scene != null && scene.hasCamera() && scene.subject() == entity;
    }

    public static final class EffectsEvent extends net.minecraftforge.eventbus.api.Event {
        public final Entity entity;
        public final Matrix4f view, projection;
        public final net.minecraft.world.phys.Vec3 eye;
        public final float partial;

        private EffectsEvent(Entity entity, Matrix4f view, Matrix4f projection, net.minecraft.world.phys.Vec3 eye, float partial) {
            this.entity = entity;
            this.view = new Matrix4f(view);
            this.projection = new Matrix4f(projection);
            this.eye = eye;
            this.partial = partial;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            ready = false;
            view = null;
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            view = new Matrix4f(event.getPoseStack().last().pose());
            normal = new Matrix3f(event.getPoseStack().last().normal());
            projection = new Matrix4f(event.getProjectionMatrix());
            modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        CutsceneScreen scene = CutsceneScreen.current();
        if (scene == null || !scene.hasCamera() || view == null) return;
        Entity boss = scene.subject();
        Minecraft mc = Minecraft.getInstance();
        if (boss == null || boss.isRemoved() || boss.level() != mc.level) return;
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        PoseStack global = RenderSystem.getModelViewStack();
        global.pushPose();
        try (CutsceneRenderState saved = new CutsceneRenderState()) {
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            if (w < 1 || h < 1) return;
            if (target == null) {
                target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
                target.setClearColor(0, 0, 0, 0);
            } else if (target.width != w || target.height != h) target.resize(w, h, Minecraft.ON_OSX);
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(projection, sorting);
            global.setIdentity();
            global.mulPoseMatrix(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableDepthTest();
            mc.gameRenderer.lightTexture().turnOnLightLayer();
            mc.gameRenderer.overlayTexture().setupOverlayColor();
            PoseStack pose = new PoseStack();
            pose.mulPoseMatrix(view);
            pose.last().normal().set(normal);
            var camera = event.getCamera().getPosition();
            float partial = event.getPartialTick();
            double x = net.minecraft.util.Mth.lerp(partial, boss.xo, boss.getX());
            double y = net.minecraft.util.Mth.lerp(partial, boss.yo, boss.getY());
            double z = net.minecraft.util.Mth.lerp(partial, boss.zo, boss.getZ());
            pose.translate(x - camera.x, y - camera.y, z - camera.z);
            capturing = true;
            var renderer = mc.getEntityRenderDispatcher().getRenderer(boss);
            renderer.render(boss, boss.getYRot(), partial, pose, BUFFERS, mc.getEntityRenderDispatcher().getPackedLightCoords(boss, partial));
            BUFFERS.endBatch();
            target.bindWrite(true);
            try (CutsceneRenderState fxState = new CutsceneRenderState()) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EffectsEvent(boss, view, projection, camera, partial));
            }
            owner = scene;
            ready = true;
        } finally {
            capturing = false;
            mc.gameRenderer.overlayTexture().teardownOverlayColor();
            mc.gameRenderer.lightTexture().turnOffLightLayer();
            global.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, sorting);
            view = null;
        }
    }

    static boolean available(CutsceneScreen scene) {
        return ready && target != null && owner == scene;
    }

    static void composite(GuiGraphics g, CutsceneScreen scene) {
        if (!available(scene)) return;
        g.flush();
        try (CutsceneRenderState saved = new CutsceneRenderState()) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, target.getColorTextureId());
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            Matrix4f m = g.pose().last().pose();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(m, 0, 0, 0).uv(0, 1).endVertex();
            b.vertex(m, 0, scene.height, 0).uv(0, 0).endVertex();
            b.vertex(m, scene.width, scene.height, 0).uv(1, 0).endVertex();
            b.vertex(m, scene.width, 0, 0).uv(1, 1).endVertex();
            BufferUploader.drawWithShader(b.end());
        }
    }

    static void release() {
        ready = false;
        owner = null;
        view = null;
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }

    private CutsceneForeground() {
    }
}
