package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.common.entity.AzumaalEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumaalStageTwoOutlinePass {

    private static final int MASK_BUFFER_SIZE = 1024 * 1024;

    private static final MultiBufferSource.BufferSource MASK_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(MASK_BUFFER_SIZE));

    private static TextureTarget maskTarget;
    private static ShaderInstance shader;

    private static boolean renderingMaskPass;

    private AzumaalStageTwoOutlinePass() {
    }

    public static boolean isRenderingMaskPass() {
        return renderingMaskPass;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || shader == null) {

            return;
        }

        List<AzumaalEntity> targets = new ArrayList<>();

        for (Entity raw : level.entitiesForRendering()) {

            if (!(raw instanceof AzumaalEntity azumaal)) {
                continue;
            }

            if (!azumaal.isAlive() || azumaal.isRemoved() || azumaal.isClone() || !azumaal.isStageTwo()) {
                continue;
            }
            targets.add(azumaal);
        }
        if (targets.isEmpty()) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        ensureTarget(main.width, main.height);
        renderMask(minecraft, event, targets);
        main.bindWrite(false);

        float time = level.getGameTime() + event.getPartialTick();

        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.222F);

        composite(main, pulse, time);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (renderingMaskPass) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static void renderMask(Minecraft minecraft, RenderLevelStageEvent event, List<AzumaalEntity> targets) {
        RenderTarget main = minecraft.getMainRenderTarget();

        maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        maskTarget.clear(Minecraft.ON_OSX);
        maskTarget.copyDepthFrom(main);
        maskTarget.bindWrite(false);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        float partialTick = event.getPartialTick();

        renderingMaskPass = true;

        try {
            for (AzumaalEntity entity : targets) {

                renderEntityToMask(minecraft, entity, poseStack, camera, partialTick);
            }

            MASK_BUFFERS.endBatch();

        } finally {

            renderingMaskPass = false;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);

            main.bindWrite(false);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderEntityToMask(Minecraft minecraft, AzumaalEntity entity, PoseStack poseStack, Vec3 camera, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        EntityRenderer renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        poseStack.pushPose();
        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);

        try {
            renderer.render(entity, yaw, partialTick, poseStack, MASK_BUFFERS, LightTexture.FULL_BRIGHT);
        } finally {
            poseStack.popPose();
        }
    }

    private static void composite(RenderTarget main, float pulse, float time) {
        if (shader == null || maskTarget == null) {

            return;
        }

        main.bindWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);

        shader.setSampler("Mask", maskTarget.getColorTextureId());

        setVec2("TexelSize", 1.0F / main.width, 1.0F / main.height);

        setFloat("Pulse", pulse);
        setFloat("Time", time * 0.05F);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        shader.clear();

        RenderSystem.depthMask(true);

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void ensureTarget(int width, int height) {
        if (maskTarget == null) {
            maskTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            maskTarget.setFilterMode(GL11.GL_LINEAR);

            return;
        }

        if (maskTarget.width != width || maskTarget.height != height) {
            maskTarget.resize(width, height, Minecraft.ON_OSX);
            maskTarget.setFilterMode(GL11.GL_LINEAR);
        }
    }

    private static void setFloat(String name, float value) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void setVec2(String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);

        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    @Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ShaderRegistration {

        private ShaderRegistration() {
        }

        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) throws IOException {

            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "azumaal_stage2_outline"), DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
        }
    }
}