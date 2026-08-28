package com.benji.oasiso.client.event;

import com.benji.oasiso.Oasiso;
import com.benji.oasiso.client.shader.EntropyPhysicsOutlineShaders;
import com.benji.oasiso.common.entity.EntropyPhysicsBlockEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = Oasiso.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntropyPhysicsSilhouettePass {

    private static final int MASK_BUFFER_SIZE = 1024 * 1024;

    private static TextureTarget maskTarget;

    private static final MultiBufferSource.BufferSource MASK_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(MASK_BUFFER_SIZE));

    private EntropyPhysicsSilhouettePass() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        ShaderInstance composite = EntropyPhysicsOutlineShaders.getCompositeShader();

        if (composite == null) {
            return;
        }
        boolean foundAny = false;

        for (Entity entity : minecraft.level.entitiesForRendering()) {

            if (entity instanceof EntropyPhysicsBlockEntity && !entity.isRemoved()) {

                foundAny = true;
                break;
            }
        }

        if (!foundAny) {
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();

        ensureMaskTarget(mainTarget.width, mainTarget.height);

        maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        maskTarget.clear(false);
        maskTarget.copyDepthFrom(mainTarget);
        maskTarget.bindWrite(true);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0F, -10.0F);

        RenderSystem.disableBlend();

        renderAllPhysicsMasks(minecraft, event);
        MASK_BUFFERS.endBatch();

        RenderSystem.polygonOffset(0.0F, 0.0F);

        RenderSystem.disablePolygonOffset();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        mainTarget.bindWrite(true);

        compositeMask(composite, event.getPartialTick());
    }

    private static void ensureMaskTarget(int width, int height) {
        if (maskTarget == null) {
            maskTarget = new TextureTarget(width, height, true, false);

            maskTarget.setFilterMode(GL11.GL_NEAREST);

            return;
        }

        if (maskTarget.width != width || maskTarget.height != height) {

            maskTarget.resize(width, height, false);

            maskTarget.setFilterMode(GL11.GL_NEAREST);
        }
    }

    private static void renderAllPhysicsMasks(Minecraft minecraft, RenderLevelStageEvent event) {
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();

        Camera camera = event.getCamera();

        Vec3 cameraPosition = camera.getPosition();

        float partialTick = event.getPartialTick();
        RenderType maskRenderType = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);

        VertexConsumer maskConsumer = MASK_BUFFERS.getBuffer(maskRenderType);

        PoseStack poseStack = event.getPoseStack();

        for (Entity raw : minecraft.level.entitiesForRendering()) {

            if (!(raw instanceof EntropyPhysicsBlockEntity entity) || entity.isRemoved()) {

                continue;
            }

            Vec3 interpolated = entity.getPosition(partialTick);

            if (interpolated.distanceToSqr(cameraPosition) > 320.0D * 320.0D) {

                continue;
            }

            poseStack.pushPose();

            poseStack.translate(interpolated.x - cameraPosition.x, interpolated.y - cameraPosition.y, interpolated.z - cameraPosition.z);

            double wiggle = entity.getPullWiggle(partialTick);

            poseStack.translate(wiggle, 0.5D + Math.abs(wiggle) * 0.25D, -wiggle * 0.65D);

            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getVisualYaw(partialTick)));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVisualPitch(partialTick)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getVisualRoll(partialTick)));
            renderModelPartToMask(blockRenderer, maskConsumer, entity.getCarriedBlockState(), 0, 0, 0, poseStack);

            for (EntropyPhysicsBlockEntity.StructurePart part : entity.getAttachedParts()) {

                renderModelPartToMask(blockRenderer, maskConsumer, part.state(), part.offset().getX(), part.offset().getY(), part.offset().getZ(), poseStack);
            }

            poseStack.popPose();
        }
    }

    private static void renderModelPartToMask(BlockRenderDispatcher blockRenderer, VertexConsumer maskConsumer, BlockState state, int offsetX, int offsetY, int offsetZ, PoseStack poseStack) {
        if (state.getRenderShape() != RenderShape.MODEL) {

            return;
        }

        poseStack.pushPose();

        poseStack.translate(offsetX - 0.5D, offsetY - 0.5D, offsetZ - 0.5D);

        BakedModel model = blockRenderer.getBlockModel(state);

        blockRenderer.getModelRenderer().renderModel(poseStack.last(), maskConsumer, state, model, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    private static void compositeMask(ShaderInstance shader, float partialTick) {
        if (maskTarget == null) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderTexture(0, maskTarget.getColorTextureId());

        RenderSystem.setShader(() -> shader);

        var timeUniform = shader.getUniform("Time");

        if (timeUniform != null) {
            float time = (System.currentTimeMillis() % 600_000L) / 1000.0F;

            timeUniform.set(time);
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
        builder.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
        builder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }
}
