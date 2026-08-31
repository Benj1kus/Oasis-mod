package com.benji.oasiso.client.shader;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.AzumalitChainClient;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.List;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AzumalitChainTargetPass {

    private static final int MASK_BUFFER_SIZE = 1024 * 1024;

    private static final MultiBufferSource.BufferSource OUTLINE_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(MASK_BUFFER_SIZE));

    private static final MultiBufferSource.BufferSource PULSE_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(MASK_BUFFER_SIZE));

    private static TextureTarget outlineMask;
    private static TextureTarget pulseMask;
    private static ShaderInstance shader;
    private static boolean renderingMaskPass;

    private AzumalitChainTargetPass() {
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

        List<Integer> outlineTargets = AzumalitChainClient.getOutlineTargetIds();

        if (outlineTargets.isEmpty()) {
            return;
        }

        double gameTime = level.getGameTime() + event.getPartialTick();

        AzumalitChainClient.PulseSnapshot pulse = AzumalitChainClient.getPulseSnapshot(gameTime);
        RenderTarget main = minecraft.getMainRenderTarget();
        ensureTargets(main.width, main.height);
        renderMask(minecraft, event, outlineMask, OUTLINE_BUFFERS, outlineTargets);

        if (!pulse.targetEntityIds().isEmpty() && pulse.alpha() > 0.001F) {

            renderMask(minecraft, event, pulseMask, PULSE_BUFFERS, pulse.targetEntityIds());
        } else {
            clearMask(pulseMask, main);
        }

        main.bindWrite(false);

        composite(main, Mth.clamp(pulse.alpha(), 0.0F, 1.0F));
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (renderingMaskPass) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static void renderMask(Minecraft minecraft, RenderLevelStageEvent event, TextureTarget target, MultiBufferSource.BufferSource buffers, List<Integer> entityIds) {
        RenderTarget main = minecraft.getMainRenderTarget();

        clearMask(target, main);
        target.bindWrite(false);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();

        renderingMaskPass = true;

        try {
            for (int entityId : entityIds) {
                Entity raw = minecraft.level.getEntity(entityId);

                if (!(raw instanceof LivingEntity entity) || !entity.isAlive() || entity.isRemoved()) {
                    continue;
                }

                renderEntityToMask(minecraft, entity, poseStack, buffers, camera, partialTick);
            }

            buffers.endBatch();
        } finally {
            renderingMaskPass = false;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);

            main.bindWrite(false);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderEntityToMask(Minecraft minecraft, LivingEntity entity, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        float entityYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        EntityRenderer renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);

        poseStack.pushPose();

        poseStack.translate(x - camera.x, y - camera.y, z - camera.z);

        try {
            renderer.render(entity, entityYaw, partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT);
        } finally {
            poseStack.popPose();
        }
    }

    private static void clearMask(TextureTarget target, RenderTarget main) {
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        target.copyDepthFrom(main);
    }

    private static void composite(RenderTarget main, float pulseStrength) {
        if (outlineMask == null || pulseMask == null || shader == null) {
            return;
        }

        main.bindWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);

        shader.setSampler("OutlineMask", outlineMask.getColorTextureId());
        shader.setSampler("PulseMask", pulseMask.getColorTextureId());

        setVec2("TexelSize", 1.0F / main.width, 1.0F / main.height);
        setFloat("PulseStrength", pulseStrength);

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

    private static void ensureTargets(int width, int height) {
        if (outlineMask == null) {
            outlineMask = createTarget(width, height);
        }

        if (pulseMask == null) {
            pulseMask = createTarget(width, height);
        }

        if (outlineMask.width != width || outlineMask.height != height) {
            outlineMask.resize(width, height, Minecraft.ON_OSX);
            outlineMask.setFilterMode(GL11.GL_NEAREST);
        }

        if (pulseMask.width != width || pulseMask.height != height) {
            pulseMask.resize(width, height, Minecraft.ON_OSX);
            pulseMask.setFilterMode(GL11.GL_NEAREST);
        }
    }

    private static TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);

        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.setFilterMode(GL11.GL_NEAREST);

        return target;
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
            event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Oasiso.MODID, "azumalit_chain_target"), DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
        }
    }
}
